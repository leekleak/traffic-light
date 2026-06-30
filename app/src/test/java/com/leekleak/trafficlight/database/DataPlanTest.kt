package com.leekleak.trafficlight.database

import android.app.usage.NetworkStats
import android.os.SystemClock
import com.leekleak.trafficlight.model.NetworkUsageManager
import com.leekleak.trafficlight.model.UsageData
import com.leekleak.trafficlight.util.DataSize
import com.leekleak.trafficlight.util.toTimestamp
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.mockkStatic
import io.mockk.slot
import io.mockk.unmockkAll
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate
import java.time.LocalDateTime

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class DataPlanTest {

    private lateinit var networkUsageManager: NetworkUsageManager

    @Before
    fun setUp() {
        networkUsageManager = mockk(relaxed = true)
        mockkStatic(LocalDateTime::class)
        
        // Mock dependencies of CryptoManager initializer to prevent ExceptionInInitializerError
        mockkStatic(java.security.KeyStore::class)
        every { java.security.KeyStore.getInstance(any<String>()) } returns mockk(relaxed = true)
        
        mockkObject(CryptoManager)
        every { CryptoManager.decrypt(any()) } answers { firstArg() }
        every { CryptoManager.encrypt(any()) } answers { firstArg() }
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    private fun setCurrentTime(dateTime: LocalDateTime) {
        every { LocalDateTime.now() } returns dateTime
        SystemClock.setCurrentTimeMillis(dateTime.toTimestamp())
    }

    @Test
    fun `initial sync sets lastUpdateStamp to currentStart`() = runTest {
        println("STARTING TEST: initial sync")
        val now = LocalDateTime.of(2023, 10, 15, 12, 0)
        setCurrentTime(now)

        val plan = DataPlan(
            hashedSubscriberID = "hash",
            encryptedSubscriberID = "enc",
            startDate = LocalDate.of(2023, 10, 1).atStartOfDay().toTimestamp(),
            lastUpdateStamp = 0L
        )

        plan.updateUsage(networkUsageManager)

        val expectedStart = LocalDate.of(2023, 10, 1).atStartOfDay().toTimestamp()
        assertEquals("Initial sync failed", expectedStart, plan.lastUpdateStamp)
    }

    @Test
    fun `updateUsage distributes usage to extras first then main plan`() = runTest {
        val now = LocalDateTime.of(2023, 10, 15, 12, 0)
        setCurrentTime(now)

        val startStamp = LocalDate.of(2023, 10, 1).atStartOfDay().toTimestamp()
        val extra1 = DataPlanExtra(dataAmount = DataSize(1000L), dataUsed = 0L, startStamp = startStamp, expiryStamp = now.plusDays(1).toTimestamp())
        val plan = DataPlan(
            hashedSubscriberID = "hash",
            encryptedSubscriberID = "enc",
            startDate = startStamp,
            mainDataSize = DataSize(5000L),
            mainDataUsed = 0L,
            mainStartStamp = startStamp,
            mainExpiryStamp = LocalDate.of(2023, 11, 1).atStartOfDay().toTimestamp(),
            extras = listOf(extra1),
            lastUpdateStamp = startStamp
        )

        val mockStats = mockk<NetworkStats>(relaxed = true)
        coEvery { networkUsageManager.queryDetails(any(), any(), any(), any()) } returns mockStats
        
        var bucketCount = 0
        val bucketSlot = slot<NetworkStats.Bucket>()
        every { mockStats.hasNextBucket() } answers { bucketCount < 1 }
        every { mockStats.getNextBucket(capture(bucketSlot)) } answers {
            setBucketFields(bucketSlot.captured, endTime = now.minusHours(1).toTimestamp())
            bucketCount++
            true
        }

        coEvery { networkUsageManager.getNetworkDataForType(any(), any(), any(), any()) } returns listOf(
            UsageData(upload = 500L, download = 1000L)
        )

        plan.updateUsage(networkUsageManager)

        assertEquals("Extra usage attribution failed", 1000L, plan.extras[0].dataUsed)
        assertEquals("Main usage attribution failed", 500L, plan.mainDataUsed)
    }

    @Test
    fun `updateUsage marks extras as expired when time is up`() = runTest {
        val now = LocalDateTime.of(2023, 10, 15, 12, 0)
        setCurrentTime(now)

        val startStamp = LocalDate.of(2023, 10, 1).atStartOfDay().toTimestamp()
        val extra1 = DataPlanExtra(dataAmount = DataSize(1000L), dataUsed = 0L, startStamp = startStamp, expiryStamp = now.minusHours(1).toTimestamp())
        
        val plan = DataPlan(
            hashedSubscriberID = "hash",
            encryptedSubscriberID = "enc",
            startDate = startStamp,
            extras = listOf(extra1),
            lastUpdateStamp = startStamp
        )

        val mockStats = mockk<NetworkStats>(relaxed = true)
        coEvery { networkUsageManager.queryDetails(any(), any(), any(), any()) } returns mockStats
        every { mockStats.hasNextBucket() } returns false

        plan.updateUsage(networkUsageManager)

        assertTrue("Extra should be expired", plan.extras[0].expired)
    }

    @Test
    fun `cycle reset clears mainUsage and updates lastUpdateStamp`() = runTest {
        val now = LocalDateTime.of(2023, 11, 15, 12, 0)
        setCurrentTime(now)

        val octStart = LocalDate.of(2023, 10, 1).atStartOfDay().toTimestamp()
        val plan = DataPlan(
            hashedSubscriberID = "hash",
            encryptedSubscriberID = "enc",
            startDate = octStart,
            mainDataSize = DataSize(5000L),
            mainDataUsed = 2000L,
            mainStartStamp = octStart,
            mainExpiryStamp = LocalDate.of(2023, 11, 1).atStartOfDay().toTimestamp(),
            lastUpdateStamp = octStart
        )

        val mockStats = mockk<NetworkStats>(relaxed = true)
        coEvery { networkUsageManager.queryDetails(any(), any(), any(), any()) } returns mockStats
        every { mockStats.hasNextBucket() } returns false

        plan.updateUsage(networkUsageManager)

        val novStart = LocalDate.of(2023, 11, 1).atStartOfDay().toTimestamp()
        assertEquals("Cycle start mismatch", novStart, plan.mainStartStamp)
        assertEquals("Cycle usage not cleared", 0L, plan.mainDataUsed)
        assertEquals("Should not have added extras when recurring is false", 0, plan.extras.size)
        assertTrue("lastUpdateStamp not moved forward", plan.lastUpdateStamp >= novStart)
    }

    @Test
    fun `recursion logic adds unused data to an extra on reset`() = runTest {
        val now = LocalDateTime.of(2023, 11, 1, 12, 0)
        setCurrentTime(now)

        val octStart = LocalDate.of(2023, 10, 1).atStartOfDay().toTimestamp()
        val novStart = LocalDate.of(2023, 11, 1).atStartOfDay().toTimestamp()
        val decStart = LocalDate.of(2023, 12, 1).atStartOfDay().toTimestamp()

        val plan = DataPlan(
            hashedSubscriberID = "hash",
            encryptedSubscriberID = "enc",
            startDate = octStart,
            mainDataSize = DataSize(5000L),
            mainDataUsed = 2000L, // 3000L remaining
            mainStartStamp = octStart,
            mainExpiryStamp = novStart,
            lastUpdateStamp = octStart,
            recurring = true
        )

        val mockStats = mockk<NetworkStats>(relaxed = true)
        coEvery { networkUsageManager.queryDetails(any(), any(), any(), any()) } returns mockStats
        
        // Single bucket to move lastUpdateStamp to now
        var bucketCount = 0
        val bucketSlot = slot<NetworkStats.Bucket>()
        every { mockStats.hasNextBucket() } answers { bucketCount < 1 }
        every { mockStats.getNextBucket(capture(bucketSlot)) } answers {
            setBucketFields(bucketSlot.captured, startTime = octStart, endTime = now.toTimestamp())
            bucketCount++
            true
        }
        
        coEvery { networkUsageManager.getNetworkDataForType(any(), any(), any(), any()) } returns emptyList()

        plan.updateUsage(networkUsageManager)

        assertEquals("Usage should be reset", 0L, plan.mainDataUsed)
        assertEquals("Start stamp should be updated", novStart, plan.mainStartStamp)
        assertEquals("Expiry stamp should be updated", decStart, plan.mainExpiryStamp)
        
        assertEquals("One extra should be added", 1, plan.extras.size)
        val extra = plan.extras[0]
        assertEquals("Rollover amount mismatch", 3000L, extra.dataAmount.byteValue)
        assertEquals("Rollover start stamp mismatch", novStart, extra.startStamp)
        assertEquals("Rollover expiry stamp mismatch", decStart, extra.expiryStamp)
    }

    @Test
    fun `getTotalUsage includes volatile usage since lastUpdateStamp`() = runTest {
        val now = LocalDateTime.of(2023, 10, 15, 12, 0)
        setCurrentTime(now)

        val startStamp = LocalDate.of(2023, 10, 1).atStartOfDay().toTimestamp()
        val lastUpdate = now.minusHours(1).toTimestamp()
        val plan = DataPlan(
            hashedSubscriberID = "hash",
            encryptedSubscriberID = "enc",
            startDate = startStamp,
            mainDataSize = DataSize(5000L),
            mainDataUsed = 1000L,
            mainStartStamp = startStamp,
            mainExpiryStamp = LocalDate.of(2023, 11, 1).atStartOfDay().toTimestamp(),
            lastUpdateStamp = lastUpdate
        )

        val mockStats = mockk<NetworkStats>(relaxed = true)
        coEvery { networkUsageManager.queryDetails(any(), any(), any(), any()) } returns mockStats
        every { mockStats.hasNextBucket() } returns false

        // Volatile usage in the last hour: 500 bytes
        coEvery { networkUsageManager.getNetworkDataForType(any(), any(), any(), any()) } returns listOf(
            UsageData(upload = 200L, download = 300L)
        )

        val totalUsage = plan.getUsageSnapshot(networkUsageManager).totalUsage

        assertEquals("Volatile usage not included", 1500L, totalUsage)
    }

    @Test
    fun `getTotalUsage correctly handles expired extras by excluding them from both used and max`() = runTest {
        val now = LocalDateTime.of(2023, 10, 15, 12, 0)
        setCurrentTime(now)

        val startStamp = LocalDate.of(2023, 10, 1).atStartOfDay().toTimestamp()
        val extra1 = DataPlanExtra(dataAmount = DataSize(1000L), dataUsed = 1000L, startStamp = startStamp, expiryStamp = now.minusDays(1).toTimestamp(), expired = true)
        
        val plan = DataPlan(
            hashedSubscriberID = "hash",
            encryptedSubscriberID = "enc",
            startDate = startStamp,
            mainDataSize = DataSize(5000L),
            mainDataUsed = 500L,
            mainStartStamp = startStamp,
            mainExpiryStamp = LocalDate.of(2023, 11, 1).atStartOfDay().toTimestamp(),
            extras = listOf(extra1),
            lastUpdateStamp = now.minusHours(1).toTimestamp()
        )

        val mockStats = mockk<NetworkStats>(relaxed = true)
        coEvery { networkUsageManager.queryDetails(any(), any(), any(), any()) } returns mockStats
        every { mockStats.hasNextBucket() } returns false
        coEvery { networkUsageManager.getNetworkDataForType(any(), any(), any(), any()) } returns emptyList()

        val totalUsage = plan.getUsageSnapshot(networkUsageManager).totalUsage
        val totalMax = plan.getTotalMax()

        assertEquals("Usage should exclude expired extras", 500L, totalUsage)
        assertEquals("Max should exclude expired extras", 5000L, totalMax)
    }

    @Test
    fun `updateUsage handles multiple extras in correct order`() = runTest {
        val now = LocalDateTime.of(2023, 10, 15, 12, 0)
        setCurrentTime(now)

        val startStamp = LocalDate.of(2023, 10, 1).atStartOfDay().toTimestamp()
        val extra1 = DataPlanExtra(id = "later", dataAmount = DataSize(1000L), dataUsed = 0L, startStamp = startStamp, expiryStamp = now.plusDays(2).toTimestamp())
        val extra2 = DataPlanExtra(id = "sooner", dataAmount = DataSize(1000L), dataUsed = 0L, startStamp = startStamp, expiryStamp = now.plusDays(1).toTimestamp())
        
        val plan = DataPlan(
            hashedSubscriberID = "hash",
            encryptedSubscriberID = "enc",
            startDate = startStamp,
            extras = listOf(extra1, extra2),
            lastUpdateStamp = startStamp
        )

        val mockStats = mockk<NetworkStats>(relaxed = true)
        coEvery { networkUsageManager.queryDetails(any(), any(), any(), any()) } returns mockStats
        var bucketCount = 0
        val bucketSlot = slot<NetworkStats.Bucket>()
        every { mockStats.hasNextBucket() } answers { bucketCount < 1 }
        every { mockStats.getNextBucket(capture(bucketSlot)) } answers {
            setBucketFields(bucketSlot.captured, endTime = now.minusHours(1).toTimestamp())
            bucketCount++
            true
        }

        coEvery { networkUsageManager.getNetworkDataForType(any(), any(), any(), any()) } returns listOf(
            UsageData(upload = 750L, download = 750L)
        )

        plan.updateUsage(networkUsageManager)

        val sooner = plan.extras.find { it.id == "sooner" }!!
        val later = plan.extras.find { it.id == "later" }!!
        
        assertEquals("Sooner extra mismatch", 1000L, sooner.dataUsed)
        assertEquals("Later extra mismatch", 500L, later.dataUsed)
    }

    @Test
    fun `getTotalMax sums main plan and active extras`() {
        val plan = DataPlan(
            hashedSubscriberID = "hash",
            encryptedSubscriberID = "enc",
            mainDataSize = DataSize(5000L),
            mainDataUsed = 0L,
            mainStartStamp = 0L,
            mainExpiryStamp = 0L,
            extras = listOf(
                DataPlanExtra(dataAmount = DataSize(1000L), dataUsed = 0L, startStamp = 0L, expiryStamp = 0L, expired = false),
                DataPlanExtra(dataAmount = DataSize(2000L), dataUsed = 0L, startStamp = 0L, expiryStamp = 0L, expired = true)
            )
        )

        assertEquals("Total max sum mismatch", 6000L, plan.getTotalMax())
    }

    @Test
    fun `complex scenario with multiple extras and cycle reset`() = runTest {
        val startOct = LocalDateTime.of(2023, 10, 1, 0, 0)
        val startNov = LocalDateTime.of(2023, 11, 1, 0, 0)

        // Initial state at Oct 15
        val tOct15 = LocalDateTime.of(2023, 10, 15, 12, 0)
        setCurrentTime(tOct15)

        val extraA = DataPlanExtra(id = "extraA", dataAmount = DataSize(1000L), dataUsed = 0L, startStamp = startOct.toTimestamp(), expiryStamp = LocalDateTime.of(2023, 10, 20, 12, 0).toTimestamp())
        val extraB = DataPlanExtra(id = "extraB", dataAmount = DataSize(2000L), dataUsed = 0L, startStamp = startOct.toTimestamp(), expiryStamp = LocalDateTime.of(2023, 11, 5, 12, 0).toTimestamp())

        val plan = DataPlan(
            hashedSubscriberID = "hash",
            encryptedSubscriberID = "enc",
            startDate = startOct.toTimestamp(),
            mainDataSize = DataSize(5000L),
            mainDataUsed = 500L,
            mainStartStamp = startOct.toTimestamp(),
            mainExpiryStamp = startNov.toTimestamp(),
            extras = listOf(extraA, extraB),
            lastUpdateStamp = tOct15.toTimestamp()
        )

        val mockStats = mockk<NetworkStats>(relaxed = true)
        coEvery { networkUsageManager.queryDetails(any(), any(), any(), any()) } returns mockStats

        // --- Update 1: Oct 18. Usage 600MB ---
        val tOct18 = LocalDateTime.of(2023, 10, 18, 12, 0)
        setCurrentTime(tOct18)

        var bucketCount = 0
        val bucketSlot = slot<NetworkStats.Bucket>()
        every { mockStats.hasNextBucket() } answers { bucketCount < 1 }
        every { mockStats.getNextBucket(capture(bucketSlot)) } answers {
            setBucketFields(bucketSlot.captured, startTime = tOct15.toTimestamp(), endTime = tOct18.toTimestamp())
            bucketCount++
            true
        }

        coEvery { networkUsageManager.getNetworkDataForType(tOct15.toTimestamp(), tOct18.toTimestamp(), any(), any()) } returns listOf(
            UsageData(upload = 200L, download = 400L)
        )

        plan.updateUsage(networkUsageManager)

        assertEquals("Update 1: Extra A should have taken usage", 600L, plan.extras.find { it.id == "extraA" }?.dataUsed)
        assertEquals("Update 1: mainDataUsed should be unchanged", 500L, plan.mainDataUsed)

        // --- Update 2: Oct 22. Usage 1000MB total from Oct 18 to Oct 22 ---
        // Split by Extra A expiry at Oct 20.
        // Oct 18 - Oct 20: 400MB (fills Extra A)
        // Oct 20 - Oct 22: 600MB (goes to Extra B)
        val tOct20 = LocalDateTime.of(2023, 10, 20, 12, 0)
        val tOct22 = LocalDateTime.of(2023, 10, 22, 12, 0)
        setCurrentTime(tOct22)

        bucketCount = 0
        every { mockStats.hasNextBucket() } answers { bucketCount < 1 }
        every { mockStats.getNextBucket(capture(bucketSlot)) } answers {
            setBucketFields(bucketSlot.captured, startTime = tOct18.toTimestamp(), endTime = tOct22.toTimestamp())
            bucketCount++
            true
        }

        coEvery { networkUsageManager.getNetworkDataForType(tOct18.toTimestamp(), tOct20.toTimestamp(), any(), any()) } returns listOf(
            UsageData(upload = 100L, download = 350L)
        )
        coEvery { networkUsageManager.getNetworkDataForType(tOct20.toTimestamp(), tOct22.toTimestamp(), any(), any()) } returns listOf(
            UsageData(upload = 200L, download = 400L)
        )

        plan.updateUsage(networkUsageManager)

        val updatedExtraA = plan.extras.find { it.id == "extraA" }!!
        val updatedExtraB = plan.extras.find { it.id == "extraB" }!!

        assertEquals("Update 2: Extra A should be full", 1000L, updatedExtraA.dataUsed)
        assertTrue("Update 2: Extra A should be marked expired", updatedExtraA.expired)
        assertEquals("Update 2: Extra B should have taken usage", 650L, updatedExtraB.dataUsed)
        assertEquals("Update 2: mainDataUsed should be unchanged", 500L, plan.mainDataUsed)

        // --- Update 3: Nov 2 ---
        // Plan reset at Nov 1.
        // Usage from Nov 1 to Nov 2: 300MB.
        val tNov2 = LocalDateTime.of(2023, 11, 2, 12, 0)
        setCurrentTime(tNov2)

        bucketCount = 0
        every { mockStats.hasNextBucket() } answers { bucketCount < 1 }
        every { mockStats.getNextBucket(capture(bucketSlot)) } answers {
            setBucketFields(bucketSlot.captured, startTime = tOct22.toTimestamp(), endTime = tNov2.toTimestamp())
            bucketCount++
            true
        }

        coEvery { networkUsageManager.getNetworkDataForType(tOct22.toTimestamp(), startNov.toTimestamp(), any(), any()) } returns emptyList()
        coEvery { networkUsageManager.getNetworkDataForType(startNov.toTimestamp(), tNov2.toTimestamp(), any(), any()) } returns listOf(
            UsageData(upload = 100L, download = 200L)
        )

        plan.updateUsage(networkUsageManager)

        assertEquals("Update 3: mainDataUsed should be reset", 0L, plan.mainDataUsed)
        assertEquals("Update 3: mainStartStamp should be Nov 1", startNov.toTimestamp(), plan.mainStartStamp)
        assertEquals("Update 3: Extra B should have taken more usage", 950L, plan.extras.find { it.id == "extraB" }?.dataUsed)
        assertEquals("Update 3: lastUpdateStamp should be Nov 2", tNov2.toTimestamp(), plan.lastUpdateStamp)
    }

    @Test
    fun `getUsageSnapshot correctly distributes volatile usage to extras`() = runTest {
        val now = LocalDateTime.of(2023, 10, 15, 12, 0)
        setCurrentTime(now)

        val startStamp = LocalDate.of(2023, 10, 1).atStartOfDay().toTimestamp()
        val lastUpdate = now.minusHours(1).toTimestamp()
        
        // Extra has 1000L capacity, 0 used.
        // Use a very distant expiry to avoid issues with potential clock drift or unmocked time
        val extra1 = DataPlanExtra(id = "extra1", dataAmount = DataSize(1000L), dataUsed = 0L, startStamp = startStamp, expiryStamp = Long.MAX_VALUE)
        
        val plan = DataPlan(
            hashedSubscriberID = "hash",
            encryptedSubscriberID = "enc",
            startDate = startStamp,
            mainDataSize = DataSize(5000L),
            mainDataUsed = 1000L,
            mainStartStamp = startStamp,
            mainExpiryStamp = LocalDate.of(2023, 11, 1).atStartOfDay().toTimestamp(),
            extras = listOf(extra1),
            lastUpdateStamp = lastUpdate
        )

        val mockStats = mockk<NetworkStats>(relaxed = true)
        coEvery { networkUsageManager.queryDetails(any(), any(), any(), any()) } returns mockStats
        every { mockStats.hasNextBucket() } returns false

        // Volatile usage: 1500L
        // Match with any end time to be resilient to System.currentTimeMillis() vs LocalDateTime.now()
        coEvery { networkUsageManager.getNetworkDataForType(eq(lastUpdate), any(), any(), any()) } returns listOf(
            UsageData(upload = 500L, download = 1000L)
        )

        val snapshot = plan.getUsageSnapshot(networkUsageManager)

        assertEquals("Volatile usage should fill extra first", 1000L, snapshot.extras[0].dataUsed)
        assertEquals("Remaining volatile usage should go to main plan", 1500L, snapshot.mainDataUsed) // 1000L initial + 500L remaining volatile
        
        // Ensure original plan is NOT modified beyond the updateUsage call (which doesn't move lastUpdateStamp here)
        assertEquals("Original plan extra usage should be unchanged", 0L, plan.extras[0].dataUsed)
        assertEquals("Original plan main usage should be unchanged", 1000L, plan.mainDataUsed)
    }

    private fun setBucketFields(bucket: NetworkStats.Bucket, uid: Int = 0, txBytes: Long = 0, rxBytes: Long = 0, startTime: Long = 0, endTime: Long = 0) {
        val fields = NetworkStats.Bucket::class.java.declaredFields
        fields.forEach { field ->
            field.isAccessible = true
            try {
                when (field.name) {
                    "uid", "mUid" -> field.set(bucket, uid)
                    "txBytes", "mTxBytes" -> field.set(bucket, txBytes)
                    "rxBytes", "mRxBytes" -> field.set(bucket, rxBytes)
                    "startTimeStamp", "mStartTimeStamp", "mBeginTimeStamp" -> field.set(bucket, startTime)
                    "endTimeStamp", "mEndTimeStamp" -> field.set(bucket, endTime)
                }
            } catch (_: Exception) {
            }
        }
    }
}
