package com.leekleak.trafficlight.database

import android.app.usage.NetworkStats
import android.content.Context
import androidx.room3.ColumnInfo
import androidx.room3.Entity
import androidx.room3.Ignore
import androidx.room3.PrimaryKey
import com.leekleak.core.R
import com.leekleak.core.util.DataSize
import com.leekleak.core.util.DataSizeUnit
import com.leekleak.core.util.fromTimestamp
import com.leekleak.core.util.toTimestamp
import com.leekleak.trafficlight.model.NetworkUsageManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import kotlin.math.min

@Serializable
data class DataPlanSnapshot(
    val mainDataUsed: Long = 0,
    val mainDataSizeUnit: DataSizeUnit = DataSizeUnit.GB,
    val extras: List<DataPlanExtra> = emptyList(),
    val computedAt: Long = 0
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is DataPlanSnapshot) return false

        return mainDataUsed == other.mainDataUsed &&
            mainDataSizeUnit == other.mainDataSizeUnit &&
            extras == other.extras
    }

    override fun hashCode(): Int {
        var result = mainDataUsed.hashCode()
        result = 31 * result + mainDataSizeUnit.hashCode()
        result = 31 * result + extras.hashCode()
        return result
    }
}

@Serializable
@Entity
data class DataPlan(
    @PrimaryKey
    val hashedSubscriberID: String,

    @ColumnInfo val encryptedSubscriberID: String,

    @ColumnInfo val simIndex: Int = -1,
    @ColumnInfo val carrierName: String = "",
    @ColumnInfo val configured: Boolean = false,

    @ColumnInfo val startDate: Long = LocalDate.now().withDayOfMonth(1).atStartOfDay().toTimestamp(),
    @ColumnInfo val interval: TimeInterval = TimeInterval.MONTH,
    @ColumnInfo val intervalMultiplier: Int = 1, // Intended only for use when TimeInterval.DAY. Otherwise should be ignored.
    @ColumnInfo val recurring: Boolean = false,

    @ColumnInfo val excludedApps: List<Int> = listOf(),

    @ColumnInfo val notification: Boolean = false,
    @ColumnInfo val liveNotification: Boolean = false,

    @ColumnInfo val budgetWarning: Boolean = false,
    @ColumnInfo val safetyWarning: Boolean = false,

    @ColumnInfo val lastSafetyState: Int = -1,
    @ColumnInfo val budgetOvershotNotified: Boolean = false,

    @ColumnInfo val mainDataSize: DataSize = DataSize(0),
    @ColumnInfo val mainDataSizeUnit: DataSizeUnit = DataSizeUnit.GB,
    @ColumnInfo val mainDataUsed: Long = 0,
    @ColumnInfo val mainStartStamp: Long = startDate,
    @ColumnInfo val mainExpiryStamp: Long = Long.MAX_VALUE,

    @ColumnInfo val extras: List<DataPlanExtra> = listOf(),
    @ColumnInfo val lastUpdateStamp: Long = 0,
    /**
     * Customization
     */
    @ColumnInfo val uiBackground: Int = if (simIndex != -1) simIndex + 1 else 0,
    @ColumnInfo val uiColor: Int = 0,
    @ColumnInfo val note: String = "",
) {
    init {
        require(intervalMultiplier > 0) {
            "intervalMultiplier must be positive, got $intervalMultiplier"
        }
    }

    @get:Ignore
    val decryptedID: String? by lazy {
        val decrypted = CryptoManager.decrypt(encryptedSubscriberID)
        if (decrypted == NULL_SUBSCRIBER) null else decrypted
    }

    fun getRemainingDuration(): Duration {
        val now = LocalDateTime.now()
        val startDate = getStartDate(true)
        return Duration.between(now, startDate)
    }

    fun resetString(context: Context): String {
        val remaining = getRemainingDuration()

        val days = remaining.toDays().toInt() + 1
        return if (days == 1) {
            val hours = remaining.toHours().toInt() + 1
            context.resources.getQuantityString(R.plurals.resets_in_hours, hours, hours)
        } else {
            context.resources.getQuantityString(R.plurals.resets_in_days, days,days)
        }
    }

    fun getStartDate(next: Boolean = false): LocalDateTime {
        val now = LocalDateTime.now()
        val startDateTime = fromTimestamp(startDate)
        return when (interval) {
            TimeInterval.MONTH -> {
                val monthsBetween = ChronoUnit.MONTHS.between(startDateTime, now)
                var currentPeriodStart = startDateTime.plusMonths(monthsBetween)
                if (currentPeriodStart > now) {
                    currentPeriodStart = currentPeriodStart.minusMonths(1)
                }
                if (next) currentPeriodStart.plusMonths(1) else currentPeriodStart
            }
            TimeInterval.DAY -> {
                val daysBetween = ChronoUnit.DAYS.between(startDateTime, now)
                val periodsBetween = daysBetween / intervalMultiplier
                var currentPeriodStart = startDateTime.plusDays(periodsBetween * intervalMultiplier)
                if (currentPeriodStart > now) {
                    currentPeriodStart = currentPeriodStart.minusDays(intervalMultiplier.toLong())
                }
                if (next) currentPeriodStart.plusDays(intervalMultiplier.toLong()) else currentPeriodStart
            }
        }
    }

    private fun calculateNextReset(from: Long): Long {
        val date = fromTimestamp(from)
        return when (interval) {
            TimeInterval.MONTH -> date.plusMonths(1).toTimestamp()
            TimeInterval.DAY -> date.plusDays(intervalMultiplier.toLong()).toTimestamp()
        }
    }

    suspend fun updateUsage(networkUsageManager: NetworkUsageManager): DataPlan = withContext(Dispatchers.Default) {
        computeUpdatedPlan(networkUsageManager)
    }

    suspend fun updateUsageAndSnapshot(networkUsageManager: NetworkUsageManager): Pair<DataPlan, DataPlanSnapshot> {
        val updatedPlan = updateUsage(networkUsageManager)
        return updatedPlan to updatedPlan.buildSnapshot(networkUsageManager)
    }

    suspend fun getUsageSnapshot(networkUsageManager: NetworkUsageManager): DataPlanSnapshot =
        updateUsageAndSnapshot(networkUsageManager).second

    private suspend fun computeUpdatedPlan(networkUsageManager: NetworkUsageManager): DataPlan {
        var plan = this
        val now = LocalDateTime.now().toTimestamp()

        val currentStart = plan.getStartDate(false).toTimestamp()
        val currentEnd = plan.getStartDate(true).toTimestamp()

        if (plan.lastUpdateStamp == 0L) {
            plan = plan.copy(lastUpdateStamp = currentStart)
        }

        if (plan.mainExpiryStamp == Long.MAX_VALUE || plan.mainExpiryStamp == 0L) {
            plan = plan.copy(mainStartStamp = currentStart, mainExpiryStamp = currentEnd)
        }

        val usageBuckets = networkUsageManager.queryDetails(DataType.Mobile.queryIndex!!, plan.decryptedID, plan.lastUpdateStamp, now)

        var bestEnd = plan.lastUpdateStamp
        while (usageBuckets.hasNextBucket()) {
            val bucket = NetworkStats.Bucket()
            usageBuckets.getNextBucket(bucket)
            if (bucket.endTimeStamp > now) {
                bestEnd = maxOf(bestEnd, bucket.startTimeStamp)
                break
            }
            bestEnd = maxOf(bestEnd, bucket.endTimeStamp)
        }
        usageBuckets.close()

        val timelineEnd = maxOf(bestEnd, currentStart)

        if (timelineEnd > plan.lastUpdateStamp) {
            val stamps = mutableSetOf(plan.lastUpdateStamp, timelineEnd)
            for (extra in plan.extras) {
                if (!extra.expired) {
                    if (extra.startStamp in (plan.lastUpdateStamp + 1)..<timelineEnd) {
                        stamps.add(extra.startStamp)
                    }
                    if (extra.expiryStamp in (plan.lastUpdateStamp + 1)..<timelineEnd) {
                        stamps.add(extra.expiryStamp)
                    }
                }
            }

            var nextReset = plan.mainExpiryStamp
            while (nextReset in plan.lastUpdateStamp..timelineEnd) {
                stamps.add(nextReset)
                nextReset = plan.calculateNextReset(nextReset)
            }

            val sortedStamps = stamps.sorted()

            for (i in 0 until sortedStamps.size - 1) {
                plan = plan.processInterval(networkUsageManager, sortedStamps[i], sortedStamps[i + 1])
            }

            plan = plan.copy(lastUpdateStamp = timelineEnd)
        }

        return plan.markExpiredExtras(now)
    }

    private fun markExpiredExtras(now: Long): DataPlan = copy(
        extras = extras.map { extra ->
            if (!extra.expired && extra.expiryStamp <= now) {
                extra.copy(expired = true)
            } else {
                extra
            }
        }
    )

    private suspend fun buildSnapshot(networkUsageManager: NetworkUsageManager): DataPlanSnapshot {
        val now = LocalDateTime.now().toTimestamp()
        val tomorrow = LocalDate.now().plusDays(1).atStartOfDay().toTimestamp()
        var mainUsed = mainDataUsed
        val snapshotExtras = extras.toMutableList()

        if (now > lastUpdateStamp) {
            val volatileUsage = getFilteredUsage(networkUsageManager, lastUpdateStamp, tomorrow, decryptedID)

            var usageToDistribute = volatileUsage

            val activeIndices = snapshotExtras.indices.filter { idx ->
                val extra = snapshotExtras[idx]
                extra.startStamp <= lastUpdateStamp && extra.expiryStamp >= now
            }.sortedBy { snapshotExtras[it].expiryStamp }

            for (idx in activeIndices) {
                val extra = snapshotExtras[idx]
                val used = min(extra.dataRemaining, usageToDistribute)
                usageToDistribute -= used
                snapshotExtras[idx] = extra.copy(dataUsed = extra.dataUsed + used)
            }
            mainUsed += usageToDistribute
        }

        return DataPlanSnapshot(
            mainDataUsed = mainUsed,
            mainDataSizeUnit = mainDataSizeUnit,
            extras = snapshotExtras,
            computedAt = now
        )
    }

    private suspend fun processInterval(networkUsageManager: NetworkUsageManager, start: Long, end: Long): DataPlan {
        var plan = this
        var currentStart = start
        while (end > plan.mainExpiryStamp || (currentStart < plan.mainExpiryStamp && end == plan.mainExpiryStamp)) {
            val usageBeforeReset = plan.getFilteredUsage(networkUsageManager, currentStart, plan.mainExpiryStamp, plan.decryptedID)
            plan = plan.distributeUsage(usageBeforeReset, currentStart, plan.mainExpiryStamp)

            val nextExpiry = plan.calculateNextReset(plan.mainExpiryStamp)
            if (plan.recurring) {
                val remaining = plan.mainDataSize.byteValue - plan.mainDataUsed
                if (remaining > 0) {
                    plan = plan.copy(
                        extras = plan.extras + DataPlanExtra(
                            dataAmount = DataSize(remaining),
                            unit = plan.mainDataSizeUnit,
                            startStamp = plan.mainExpiryStamp,
                            expiryStamp = nextExpiry
                        )
                    )
                }
            }
            plan = plan.copy(
                extras = plan.extras.filter { it.expiryStamp > plan.mainExpiryStamp },
                mainDataUsed = 0,
                mainStartStamp = plan.mainExpiryStamp,
                mainExpiryStamp = nextExpiry
            )
            currentStart = plan.mainStartStamp
        }

        if (end > currentStart) {
            val usage = plan.getFilteredUsage(networkUsageManager, currentStart, end, plan.decryptedID)
            plan = plan.distributeUsage(usage, currentStart, end)
        }
        return plan
    }

    private fun distributeUsage(usage: Long, start: Long, end: Long): DataPlan {
        var usageToDistribute = usage

        val updatedExtras = extras.toMutableList()
        val activeIndices = updatedExtras.indices.filter { idx ->
            val extra = updatedExtras[idx]
            extra.startStamp <= start && extra.expiryStamp >= end
        }.sortedBy { updatedExtras[it].expiryStamp }

        for (idx in activeIndices) {
            val extra = updatedExtras[idx]
            val used = min(extra.dataRemaining, usageToDistribute)
            usageToDistribute -= used
            updatedExtras[idx] = extra.copy(dataUsed = extra.dataUsed + used)
        }
        return copy(
            mainDataUsed = mainDataUsed + usageToDistribute,
            extras = updatedExtras
        )
    }

    private suspend fun getFilteredUsage(networkUsageManager: NetworkUsageManager, start: Long, end: Long, id: String?): Long {
        val usageData = networkUsageManager.getNetworkDataForType(start, end, id, DataType.Mobile)
        return usageData.filter { !excludedApps.contains(it.uid) }.sumOf { it.total }
    }

    companion object {
        const val NULL_SUBSCRIBER = "__shizuku_disabled_sim_fallback__"
    }
}
