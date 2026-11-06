package com.leekleak.trafficlight.model

import android.app.usage.NetworkStats
import com.leekleak.core.util.toTimestamp
import com.leekleak.trafficlight.database.CryptoManager
import com.leekleak.trafficlight.database.DataPlan
import com.leekleak.trafficlight.database.DataPlanDao
import com.leekleak.trafficlight.database.DataPlanSnapshot
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.security.KeyStore
import java.time.LocalDate

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class DataPlanManagerTest {

    private lateinit var dao: FakeDataPlanDao
    private lateinit var networkUsageManager: NetworkUsageManager
    private lateinit var dataPlanManager: DataPlanManager

    @Before
    fun setUp() {
        networkUsageManager = mockk(relaxed = true)

        val networkStats = mockk<NetworkStats>(relaxed = true)
        every { networkStats.hasNextBucket() } returns false
        coEvery { networkUsageManager.queryDetails(any(), any(), any(), any()) } returns networkStats
        coEvery { networkUsageManager.getNetworkDataForType(any(), any(), any(), any()) } returns emptyList()

        mockkStatic(KeyStore::class)
        every { KeyStore.getInstance(any<String>()) } returns mockk(relaxed = true)
        mockkObject(CryptoManager)
        every { CryptoManager.decrypt(any()) } answers { firstArg() }
        every { CryptoManager.encrypt(any()) } answers { firstArg() }
        every { CryptoManager.hashIdentifier(any()) } answers { firstArg() }

        dao = FakeDataPlanDao()
        dataPlanManager = DataPlanManager(dao, networkUsageManager)
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `snapshot equality ignores computedAt`() {
        val snapshot = DataPlanSnapshot(mainDataUsed = 100L, computedAt = 1L)

        assertEquals(snapshot, snapshot.copy(computedAt = 2L))
        assertEquals(snapshot.hashCode(), snapshot.copy(computedAt = 2L).hashCode())
    }

    @Test
    fun `refresh persists updated plans and caches snapshots`() = runTest {
        val plan = testPlan(mainDataUsed = 123L)
        dao.add(plan)

        val results = dataPlanManager.refresh()

        assertEquals(1, results.size)
        assertEquals(123L, results.first().snapshot.mainDataUsed)
        assertEquals(results.first().plan, dao.plans.value.first())
        assertEquals(results.first().snapshot, dataPlanManager.snapshots.value[plan.hashedSubscriberID])
    }

    @Test
    fun `getSnapshot reuses cache within ttl and refreshes after invalidation`() = runTest {
        val plan = testPlan()
        dao.add(plan)

        dataPlanManager.getSnapshot(plan)
        dataPlanManager.getSnapshot(plan)

        coVerify(exactly = 1) {
            networkUsageManager.getNetworkDataForType(any(), any(), any(), any())
        }

        dataPlanManager.addPlan(plan.copy(mainDataUsed = 42L))
        dataPlanManager.getSnapshot(plan.copy(mainDataUsed = 42L))

        coVerify(exactly = 2) {
            networkUsageManager.getNetworkDataForType(any(), any(), any(), any())
        }
    }

    @Test
    fun `refreshPlan returns null for a missing plan`() = runTest {
        assertNull(dataPlanManager.refreshPlan("missing"))
    }

    private fun testPlan(mainDataUsed: Long = 0L): DataPlan = DataPlan(
        hashedSubscriberID = "hash",
        encryptedSubscriberID = "subscriber",
        simIndex = 0,
        startDate = LocalDate.of(2023, 10, 1).atStartOfDay().toTimestamp(),
        mainDataUsed = mainDataUsed,
        lastUpdateStamp = 0L
    )

    private class FakeDataPlanDao : DataPlanDao {
        val plans = MutableStateFlow<List<DataPlan>>(emptyList())

        override suspend fun getAll(): List<DataPlan> = plans.value

        override suspend fun getByHash(hashedID: String): DataPlan? =
            plans.value.find { it.hashedSubscriberID == hashedID }

        override val allPlansFlow: Flow<List<DataPlan>>
            get() = plans.map { it.sortedBy { plan -> plan.simIndex } }

        override val activePlansFlow: Flow<List<DataPlan>>
            get() = allPlansFlow.map { plans -> plans.filter { it.simIndex != -1 } }

        override fun getActivePlansWithNotificationsFlow(): Flow<List<DataPlan>> =
            activePlansFlow.map { plans -> plans.filter { it.notification } }

        override fun getActivePlansWithNotificationsCountFlow(): Flow<Int> =
            getActivePlansWithNotificationsFlow().map { it.size }

        override suspend fun getActivePlans(): List<DataPlan> =
            plans.value.filter { it.simIndex != -1 }.sortedBy { it.simIndex }

        override suspend fun add(dataPlan: DataPlan) {
            plans.update { current ->
                current.filterNot { it.hashedSubscriberID == dataPlan.hashedSubscriberID } + dataPlan
            }
        }

        override suspend fun addAll(plans: List<DataPlan>) {
            this.plans.update { current ->
                val updated = current.associateBy { it.hashedSubscriberID }.toMutableMap()
                plans.forEach { updated[it.hashedSubscriberID] = it }
                updated.values.toList()
            }
        }

        override suspend fun delete(hashedID: String) {
            plans.update { current -> current.filterNot { it.hashedSubscriberID == hashedID } }
        }
    }
}
