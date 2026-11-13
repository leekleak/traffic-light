package com.leekleak.trafficlight.model

import com.leekleak.core.util.toTimestamp
import com.leekleak.trafficlight.database.CryptoManager
import com.leekleak.trafficlight.database.DataPlan
import com.leekleak.trafficlight.database.DataPlanDao
import com.leekleak.trafficlight.database.DataPlanSnapshot
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

data class DataPlanRefresh(
    val plan: DataPlan,
    val snapshot: DataPlanSnapshot,
)

class DataPlanManager(
    private val dao: DataPlanDao,
    private val networkUsageManager: NetworkUsageManager,
) {
    val plans: Flow<List<DataPlan>> = dao.allPlansFlow
    val activePlans: Flow<List<DataPlan>> = dao.activePlansFlow
    val activePlansWithNotifications: Flow<List<DataPlan>> = dao.getActivePlansWithNotificationsFlow()
    val activePlanNotificationsCount: Flow<Int> = dao.getActivePlansWithNotificationsCountFlow()

    private val snapshotCache = mutableMapOf<String, DataPlanSnapshot>()
    private val snapshotCacheMutex = Mutex()
    private val _snapshots = MutableStateFlow<Map<String, DataPlanSnapshot>>(emptyMap())
    val snapshots: StateFlow<Map<String, DataPlanSnapshot>> = _snapshots.asStateFlow()

    private val dataMutex = Mutex()

    suspend fun getPlan(hashedSubscriberID: String): DataPlan? = dao.getByHash(hashedSubscriberID)

    suspend fun getPlans(): List<DataPlan> = dao.getAll()

    suspend fun addPlan(plan: DataPlan) = dataMutex.withLock {
        invalidateSnapshot(plan.hashedSubscriberID)
        dao.add(plan)
    }

    suspend fun addPlans(plans: List<DataPlan>) = dataMutex.withLock {
        if (plans.isEmpty()) return@withLock
        plans.forEach { invalidateSnapshot(it.hashedSubscriberID) }
        dao.addAll(plans)
    }

    suspend fun createPlan(plainSubscriberID: String?, simIndex: Int, carrierName: String) {
        val subscriberID = plainSubscriberID ?: DataPlan.NULL_SUBSCRIBER
        addPlan(
            DataPlan(
                hashedSubscriberID = CryptoManager.hashIdentifier(subscriberID),
                encryptedSubscriberID = CryptoManager.encrypt(subscriberID),
                simIndex = simIndex,
                carrierName = carrierName
            )
        )
    }

    suspend fun deletePlan(hashedSubscriberID: String) = dataMutex.withLock {
        invalidateSnapshot(hashedSubscriberID)
        dao.delete(hashedSubscriberID)
    }

    suspend fun saveConfiguredPlan(plan: DataPlan): DataPlan = dataMutex.withLock {
        val (updatedPlan, snapshot) = withContext(Dispatchers.Default) {
            plan.updateUsageAndSnapshot(networkUsageManager)
        }

        val volatileMain = snapshot.mainDataUsed - updatedPlan.mainDataUsed
        val volatileExtras = snapshot.extras.associate { extra ->
            extra.id to (extra.dataUsed - (updatedPlan.extras.find { it.id == extra.id }?.dataUsed ?: 0L))
        }
        val start = plan.getStartDate(false)
        val expiry = plan.getStartDate(true)

        val planToSave = plan.copy(
            mainDataUsed = plan.mainDataUsed - volatileMain,
            extras = plan.extras.map {
                it.copy(dataUsed = it.dataUsed - (volatileExtras[it.id] ?: 0L))
            },
            lastUpdateStamp = updatedPlan.lastUpdateStamp,
            lastSafetyState = -1,
            mainStartStamp = start.toTimestamp(),
            mainExpiryStamp = expiry.toTimestamp(),
            budgetOvershotNotified = false,
            configured = true
        )
        invalidateSnapshot(planToSave.hashedSubscriberID)
        dao.add(planToSave)
        planToSave
    }

    suspend fun computeUsage(plan: DataPlan, resetUsage: Boolean = false): DataPlanRefresh {
        val basePlan = if (resetUsage) {
            plan.copy(
                mainDataUsed = 0,
                lastUpdateStamp = 0,
                extras = plan.extras.map { it.copy(dataUsed = 0) },
                mainStartStamp = plan.getStartDate(false).toTimestamp(),
                mainExpiryStamp = plan.getStartDate(true).toTimestamp(),
            )
        } else {
            plan
        }
        val (updatedPlan, snapshot) = withContext(Dispatchers.Default) {
            basePlan.updateUsageAndSnapshot(networkUsageManager)
        }
        return DataPlanRefresh(updatedPlan, snapshot)
    }

    suspend fun getSnapshot(
        plan: DataPlan,
        maxAgeMs: Long = SNAPSHOT_UPDATE_MS
    ): DataPlanSnapshot {
        val now = System.currentTimeMillis()
        if (maxAgeMs > 0) {
            snapshotCacheMutex.withLock {
                snapshotCache[plan.hashedSubscriberID]
                    ?.takeIf { now - it.computedAt <= maxAgeMs }
                    ?.let { return it }
            }
        }

        val snapshot = withContext(Dispatchers.Default) {
            plan.getUsageSnapshot(networkUsageManager)
        }
        snapshotCacheMutex.withLock {
            snapshotCache[plan.hashedSubscriberID] = snapshot
            _snapshots.value = snapshotCache.toMap()
        }
        return snapshot
    }

    suspend fun getSnapshots(
        plans: List<DataPlan>,
        maxAgeMs: Long = SNAPSHOT_UPDATE_MS
    ): Map<String, DataPlanSnapshot> = coroutineScope {
        plans.map { plan ->
            async { plan.hashedSubscriberID to getSnapshot(plan, maxAgeMs) }
        }.awaitAll().toMap()
    }

    suspend fun ensureSnapshots(maxAgeMs: Long = SNAPSHOT_UPDATE_MS) {
        val activePlans = dao.getActivePlans()
        if (activePlans.isNotEmpty()) {
            getSnapshots(activePlans, maxAgeMs)
        }
    }

    suspend fun refresh(): List<DataPlanRefresh> = dataMutex.withLock {
        val currentPlans = dao.getActivePlans()
        if (currentPlans.isEmpty()) return@withLock emptyList()

        val results = coroutineScope {
            currentPlans.map { plan ->
                async(Dispatchers.Default) {
                    val (updatedPlan, snapshot) = plan.updateUsageAndSnapshot(networkUsageManager)
                    DataPlanRefresh(updatedPlan, snapshot)
                }
            }.awaitAll()
        }

        val changedPlans = results.mapIndexedNotNull { index, result ->
            result.plan.takeIf { it != currentPlans[index] }
        }
        if (changedPlans.isNotEmpty()) {
            dao.addAll(changedPlans)
        }

        snapshotCacheMutex.withLock {
            results.forEach { result ->
                snapshotCache[result.plan.hashedSubscriberID] = result.snapshot
            }
            _snapshots.value = snapshotCache.toMap()
        }

        results
    }

    suspend fun refreshPlan(hashedSubscriberID: String): DataPlanRefresh? = dataMutex.withLock {
        val plan = dao.getByHash(hashedSubscriberID) ?: return@withLock null
        val (updatedPlan, snapshot) = withContext(Dispatchers.Default) {
            plan.updateUsageAndSnapshot(networkUsageManager)
        }
        if (updatedPlan != plan) {
            dao.add(updatedPlan)
        }
        snapshotCacheMutex.withLock {
            snapshotCache[hashedSubscriberID] = snapshot
            _snapshots.value = snapshotCache.toMap()
        }
        DataPlanRefresh(updatedPlan, snapshot)
    }

    private suspend fun invalidateSnapshot(hashedSubscriberID: String) {
        snapshotCacheMutex.withLock {
            if (snapshotCache.remove(hashedSubscriberID) != null) {
                _snapshots.value = snapshotCache.toMap()
            }
        }
    }

    companion object {
        const val SNAPSHOT_UPDATE_MS = 5_000L
    }
}
