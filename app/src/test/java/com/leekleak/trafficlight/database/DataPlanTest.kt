package com.leekleak.trafficlight.database

import android.app.usage.NetworkStats
import android.os.SystemClock
import com.leekleak.trafficlight.model.NetworkUsageManager
import com.leekleak.trafficlight.model.UsageData
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
        val extra1 = DataPlanExtra(dataAmount = 1000L, dataUsed = 0L, startStamp = startStamp, expiryStamp = now.plusDays(1).toTimestamp())
        val plan = DataPlan(
            hashedSubscriberID = "hash",
            encryptedSubscriberID = "enc",
            startDate = startStamp,
            mainUsage = DataPlanMain(dataAmount = 5000L, dataUsed = 0L, startStamp = startStamp, expiryStamp = LocalDate.of(2023, 11, 1).atStartOfDay().toTimestamp()),
            extras = listOf(extra1),
            lastUpdateStamp = startStamp
        )

        val mockStats = mockk<NetworkStats>(relaxed = true)
        every { networkUsageManager.queryDetails(any(), any(), any(), any()) } returns mockStats
        
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
        assertEquals("Main usage attribution failed", 500L, plan.mainUsage.dataUsed)
    }

    @Test
    fun `updateUsage marks extras as expired when time is up`() = runTest {
        val now = LocalDateTime.of(2023, 10, 15, 12, 0)
        setCurrentTime(now)

        val startStamp = LocalDate.of(2023, 10, 1).atStartOfDay().toTimestamp()
        val extra1 = DataPlanExtra(dataAmount = 1000L, dataUsed = 0L, startStamp = startStamp, expiryStamp = now.minusHours(1).toTimestamp())
        
        val plan = DataPlan(
            hashedSubscriberID = "hash",
            encryptedSubscriberID = "enc",
            startDate = startStamp,
            extras = listOf(extra1),
            lastUpdateStamp = startStamp
        )

        val mockStats = mockk<NetworkStats>(relaxed = true)
        every { networkUsageManager.queryDetails(any(), any(), any(), any()) } returns mockStats
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
            mainUsage = DataPlanMain(dataAmount = 5000L, dataUsed = 2000L, startStamp = octStart, expiryStamp = LocalDate.of(2023, 11, 1).atStartOfDay().toTimestamp()),
            lastUpdateStamp = octStart
        )

        val mockStats = mockk<NetworkStats>(relaxed = true)
        every { networkUsageManager.queryDetails(any(), any(), any(), any()) } returns mockStats
        every { mockStats.hasNextBucket() } returns false

        plan.updateUsage(networkUsageManager)

        val novStart = LocalDate.of(2023, 11, 1).atStartOfDay().toTimestamp()
        assertEquals("Cycle start mismatch", novStart, plan.mainUsage.startStamp)
        assertEquals("Cycle usage not cleared", 0L, plan.mainUsage.dataUsed)
        assertTrue("lastUpdateStamp not moved forward", plan.lastUpdateStamp >= novStart)
    }

    @Test
    fun `getUsage includes volatile usage since lastUpdateStamp`() = runTest {
        val now = LocalDateTime.of(2023, 10, 15, 12, 0)
        setCurrentTime(now)

        val startStamp = LocalDate.of(2023, 10, 1).atStartOfDay().toTimestamp()
        val lastUpdate = now.minusHours(1).toTimestamp()
        val plan = DataPlan(
            hashedSubscriberID = "hash",
            encryptedSubscriberID = "enc",
            startDate = startStamp,
            mainUsage = DataPlanMain(dataAmount = 5000L, dataUsed = 1000L, startStamp = startStamp, expiryStamp = LocalDate.of(2023, 11, 1).atStartOfDay().toTimestamp()),
            lastUpdateStamp = lastUpdate
        )

        val mockStats = mockk<NetworkStats>(relaxed = true)
        every { networkUsageManager.queryDetails(any(), any(), any(), any()) } returns mockStats
        every { mockStats.hasNextBucket() } returns false

        // Volatile usage in the last hour: 500 bytes
        coEvery { networkUsageManager.getNetworkDataForType(any(), any(), any(), any()) } returns listOf(
            UsageData(upload = 200L, download = 300L)
        )

        val totalUsage = plan.getUsage(networkUsageManager)

        assertEquals("Volatile usage not included", 1500L, totalUsage)
    }

    @Test
    fun `getUsage correctly handles expired extras by excluding them from both used and max`() = runTest {
        val now = LocalDateTime.of(2023, 10, 15, 12, 0)
        setCurrentTime(now)

        val startStamp = LocalDate.of(2023, 10, 1).atStartOfDay().toTimestamp()
        val extra1 = DataPlanExtra(dataAmount = 1000L, dataUsed = 1000L, startStamp = startStamp, expiryStamp = now.minusDays(1).toTimestamp(), expired = true)
        
        val plan = DataPlan(
            hashedSubscriberID = "hash",
            encryptedSubscriberID = "enc",
            startDate = startStamp,
            mainUsage = DataPlanMain(dataAmount = 5000L, dataUsed = 500L, startStamp = startStamp, expiryStamp = LocalDate.of(2023, 11, 1).atStartOfDay().toTimestamp()),
            extras = listOf(extra1),
            lastUpdateStamp = now.minusHours(1).toTimestamp()
        )

        val mockStats = mockk<NetworkStats>(relaxed = true)
        every { networkUsageManager.queryDetails(any(), any(), any(), any()) } returns mockStats
        every { mockStats.hasNextBucket() } returns false
        coEvery { networkUsageManager.getNetworkDataForType(any(), any(), any(), any()) } returns emptyList()

        val totalUsage = plan.getUsage(networkUsageManager)
        val totalMax = plan.getTotalMax()

        assertEquals("Usage should exclude expired extras", 500L, totalUsage)
        assertEquals("Max should exclude expired extras", 5000L, totalMax)
    }

    @Test
    fun `updateUsage handles multiple extras in correct order`() = runTest {
        val now = LocalDateTime.of(2023, 10, 15, 12, 0)
        setCurrentTime(now)

        val startStamp = LocalDate.of(2023, 10, 1).atStartOfDay().toTimestamp()
        val extra1 = DataPlanExtra(id = "later", dataAmount = 1000L, dataUsed = 0L, startStamp = startStamp, expiryStamp = now.plusDays(2).toTimestamp())
        val extra2 = DataPlanExtra(id = "sooner", dataAmount = 1000L, dataUsed = 0L, startStamp = startStamp, expiryStamp = now.plusDays(1).toTimestamp())
        
        val plan = DataPlan(
            hashedSubscriberID = "hash",
            encryptedSubscriberID = "enc",
            startDate = startStamp,
            extras = listOf(extra1, extra2),
            lastUpdateStamp = startStamp
        )

        val mockStats = mockk<NetworkStats>(relaxed = true)
        every { networkUsageManager.queryDetails(any(), any(), any(), any()) } returns mockStats
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
            mainUsage = DataPlanMain(dataAmount = 5000L, dataUsed = 0L, startStamp = 0L, expiryStamp = 0L),
            extras = listOf(
                DataPlanExtra(dataAmount = 1000L, dataUsed = 0L, startStamp = 0L, expiryStamp = 0L, expired = false),
                DataPlanExtra(dataAmount = 2000L, dataUsed = 0L, startStamp = 0L, expiryStamp = 0L, expired = true)
            )
        )

        assertEquals("Total max sum mismatch", 6000L, plan.getTotalMax())
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
