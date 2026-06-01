package com.leekleak.trafficlight.model

import android.app.usage.NetworkStats
import android.app.usage.NetworkStatsManager
import app.cash.turbine.test
import com.leekleak.trafficlight.database.DataDirection
import com.leekleak.trafficlight.database.DataType
import com.leekleak.trafficlight.database.UsageQuery
import com.leekleak.trafficlight.ui.history.DateParams
import com.leekleak.trafficlight.util.toTimestamp
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate
import java.util.TimeZone

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class NetworkUsageManagerTest {

    private lateinit var networkStatsManager: NetworkStatsManager
    private lateinit var appManager: AppManager
    private lateinit var networkUsageManager: NetworkUsageManager

    @Before
    fun setUp() {
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"))
        networkStatsManager = mockk(relaxed = true)
        appManager = mockk(relaxed = true)
        networkUsageManager = NetworkUsageManager(networkStatsManager, appManager)
    }

    @Test
    fun `totalDayUsage returns correct sum for given query`() = runTest {
        val startDate = LocalDate.of(2023, 10, 1)
        val dataUID = mockk<DataUID>()
        every { dataUID.uid } returns 1001
        every { dataUID.uidQuery } returns null
        val query = UsageQuery(dataType = DataType.Mobile, dataDirection = DataDirection.Bidirectional, dataUID = dataUID)
        
        val mockSummary = mockk<NetworkStats>(relaxed = true)
        every { networkStatsManager.querySummary(any(), any(), any(), any()) } returns mockSummary
        
        var bucketCount = 0
        val bucketSlot = slot<NetworkStats.Bucket>()
        every { mockSummary.hasNextBucket() } answers { bucketCount < 2 }
        every { mockSummary.getNextBucket(capture(bucketSlot)) } answers {
            val bucket = bucketSlot.captured
            if (bucketCount == 0) {
                setBucketFields(bucket, uid = 1001, txBytes = 1000L, rxBytes = 2000L)
            } else {
                setBucketFields(bucket, uid = 1002, txBytes = 3000L, rxBytes = 4000L)
            }
            bucketCount++
            true
        }

        val result = networkUsageManager.totalDayUsage(query, startDate)
        
        // (1000 + 2000) + (3000 + 4000) = 10000
        assertEquals(10000L, result)
    }

    @Test
    fun `totalDayUsage filters by UID when uidQuery is not null`() = runTest {
        val startDate = LocalDate.of(2023, 10, 1)
        val dataUID = mockk<DataUID>()
        every { dataUID.uid } returns 1001
        every { dataUID.uidQuery } returns 1001 
        val query = UsageQuery(dataType = DataType.Mobile, dataDirection = DataDirection.Download, dataUID = dataUID)
        
        val mockSummary = mockk<NetworkStats>(relaxed = true)
        every { networkStatsManager.querySummary(any(), any(), any(), any()) } returns mockSummary
        
        var bucketCount = 0
        val bucketSlot = slot<NetworkStats.Bucket>()
        every { mockSummary.hasNextBucket() } answers { bucketCount < 2 }
        every { mockSummary.getNextBucket(capture(bucketSlot)) } answers {
            val bucket = bucketSlot.captured
            if (bucketCount == 0) {
                setBucketFields(bucket, uid = 1001, txBytes = 1000L, rxBytes = 2000L)
            } else {
                setBucketFields(bucket, uid = 1002, txBytes = 3000L, rxBytes = 4000L)
            }
            bucketCount++
            true
        }

        val result = networkUsageManager.totalDayUsage(query, startDate)
        
        // Only uid 1001 rxBytes (Download) = 2000
        assertEquals(2000L, result)
    }

    @Test
    fun `getNetworkDataForType aggregates usage by UID`() = runTest {
        val mockSummary = mockk<NetworkStats>(relaxed = true)
        every { networkStatsManager.querySummary(any(), any(), any(), any()) } returns mockSummary
        
        var bucketCount = 0
        val bucketSlot = slot<NetworkStats.Bucket>()
        every { mockSummary.hasNextBucket() } answers { bucketCount < 3 }
        every { mockSummary.getNextBucket(capture(bucketSlot)) } answers {
            val bucket = bucketSlot.captured
            when (bucketCount) {
                0 -> setBucketFields(bucket, uid = 1001, txBytes = 100L, rxBytes = 200L)
                1 -> setBucketFields(bucket, uid = 1001, txBytes = 50L, rxBytes = 50L)
                2 -> setBucketFields(bucket, uid = 1002, txBytes = 300L, rxBytes = 400L)
            }
            bucketCount++
            true
        }

        val result = networkUsageManager.getNetworkDataForType(0, 1000, null, DataType.Mobile)
        
        assertEquals(2, result.size)
        val usage1001 = result.find { it.uid == 1001 }
        assertEquals(150L, usage1001?.upload)
        assertEquals(250L, usage1001?.download)
    }

    @Test
    fun `daysUsage flow emits correct data points`() = runTest {
        val startDate = LocalDate.of(2023, 10, 1)
        val endDate = LocalDate.of(2023, 10, 2)
        val query1 = UsageQuery(DataType.Mobile)
        
        val mockSummary = mockk<NetworkStats>(relaxed = true)
        every { networkStatsManager.querySummary(any(), any(), any(), any()) } returns mockSummary
        
        var bucketCount = 0
        val bucketSlot = slot<NetworkStats.Bucket>()
        every { mockSummary.hasNextBucket() } answers { bucketCount < 1 }
        every { mockSummary.getNextBucket(capture(bucketSlot)) } answers {
            setBucketFields(bucketSlot.captured, uid = 1001, txBytes = 500L, rxBytes = 500L)
            bucketCount++
            true
        }

        networkUsageManager.daysUsage(startDate, endDate, query1).test {
            val initial = awaitItem()
            assertEquals(1, initial.size)

            val updated = awaitItem()
            assertEquals(1000L, updated[0].y1)
            
            awaitComplete()
        }
    }

    @Test
    fun `getAllHourUsage distributes usage into slots`() = runTest {
        val dateParams = mockk<DateParams>()
        val start = LocalDate.of(2023, 10, 1)
        val end = start.plusDays(1)
        every { dateParams.getStartEndDates() } returns Pair(start, end)
        every { dateParams.day } returns start

        val query1 = UsageQuery(DataType.Mobile)
        val query2 = UsageQuery(DataType.Wifi)

        val startTime = start.atStartOfDay().toTimestamp()

        // Mobile (queryIndex 0)
        every { networkStatsManager.querySummaryForDevice(0, any(), any(), any()) } answers {
            val s = it.invocation.args[2] as Long
            val e = it.invocation.args[3] as Long
            val bucket = NetworkStats.Bucket()
            if (s == startTime) {
                setBucketFields(bucket, uid = -1, txBytes = 1000L, rxBytes = 1000L, startTime = s, endTime = e)
            } else {
                setBucketFields(bucket, uid = -1, txBytes = 0, rxBytes = 0, startTime = s, endTime = e)
            }
            bucket
        }

        // Wifi (queryIndex 1)
        every { networkStatsManager.querySummaryForDevice(1, any(), any(), any()) } answers {
            val s = it.invocation.args[2] as Long
            val e = it.invocation.args[3] as Long
            val bucket = NetworkStats.Bucket()
            setBucketFields(bucket, uid = -1, txBytes = 0, rxBytes = 0, startTime = s, endTime = e)
            bucket
        }

        val result = networkUsageManager.getAllHourUsage(dateParams, query1, query2)

        assertEquals(12, result.size)
        // First 2-hour slot (00:00 - 02:00) should contain the 2000 bytes
        assertEquals(2000L, result[0].usage.usage1)
    }

    @Test
    fun `getAllHourUsage interpolates usage when bucket spans two slots with offset`() = runTest {
        val dateParams = mockk<DateParams>()
        val start = LocalDate.of(2023, 10, 1)
        val end = start.plusDays(1)
        every { dateParams.getStartEndDates() } returns Pair(start, end)
        every { dateParams.day } returns start
        
        val query1 = UsageQuery(DataType.Mobile)
        val query2 = UsageQuery(DataType.Wifi)
        
        val dayStartTime = start.toTimestamp()
        val hours2 = 1000 * 60 * 60 * 2L
        
        // Bucket: 01:15 to 02:15 (Duration: 1 hour)
        val bucketStart = dayStartTime + (1 * 3600 + 15 * 60) * 1000L
        val bucketEnd = dayStartTime + (2 * 3600 + 15 * 60) * 1000L
        
        every { networkStatsManager.querySummaryForDevice(0, any(), any(), any()) } answers {
            val s = it.invocation.args[2] as Long
            val bucket = NetworkStats.Bucket()
            if (s == dayStartTime) {
                setBucketFields(bucket, uid = -1, txBytes = 500L, rxBytes = 500L, 
                    startTime = bucketStart, endTime = bucketEnd)
            } else {
                setBucketFields(bucket, uid = -1, txBytes = 0, rxBytes = 0, startTime = s, endTime = s + hours2)
            }
            bucket
        }

        every { networkStatsManager.querySummaryForDevice(1, any(), any(), any()) } answers {
            val s = it.invocation.args[2] as Long
            val bucket = NetworkStats.Bucket()
            setBucketFields(bucket, uid = -1, txBytes = 0, rxBytes = 0, startTime = s, endTime = s + hours2)
            bucket
        }

        val result = networkUsageManager.getAllHourUsage(dateParams, query1, query2)
        
        /**
         * Correct Temporal Distribution:
         * Total Duration: 1h (60m).
         * Time in Slot 0 (01:15 to 02:00): 45 minutes (75% of 60m).
         * Time in Slot 1 (02:00 to 02:15): 15 minutes (25% of 60m).
         * 
         * For 1000 total bytes:
         * Expected Slot 0: 750 bytes
         * Expected Slot 1: 250 bytes
         */
        assertEquals(750L, result[0].usage.usage1)
        assertEquals(250L, result[1].usage.usage1)
    }

    /**
     * Helper to set private/final fields on NetworkStats.Bucket using reflection.
     * This is necessary because Robolectric's shadow for Bucket doesn't always expose 
     * setters for all fields across all SDK versions.
     */
    private fun setBucketFields(bucket: NetworkStats.Bucket, uid: Int, txBytes: Long, rxBytes: Long, startTime: Long = 0, endTime: Long = 0) {
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
            } catch (e: Exception) {
                // Ignore if field doesn't exist in this Android version
            }
        }
    }
}
