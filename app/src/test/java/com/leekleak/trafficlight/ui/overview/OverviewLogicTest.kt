package com.leekleak.trafficlight.ui.overview

import android.app.usage.NetworkStats
import android.app.usage.NetworkStatsManager
import com.leekleak.trafficlight.model.AppManager
import com.leekleak.trafficlight.model.NetworkUsageManager
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.TimeZone

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class OverviewLogicTest {

    private lateinit var networkStatsManager: NetworkStatsManager
    private lateinit var appManager: AppManager
    private lateinit var overviewLogic: OverviewLogic

    @Before
    fun setUp() {
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"))
        networkStatsManager = mockk(relaxed = true)
        appManager = mockk(relaxed = true)
        overviewLogic = OverviewLogic(NetworkUsageManager(networkStatsManager, appManager))
    }

    @Test
    fun `getTodayBreakdown returns download upload and total for each transport`() = runTest {
        stubUsage(
            mobileBuckets = listOf(UsageBucket(txBytes = 1_200L, rxBytes = 3_400L)),
            wifiBuckets = listOf(UsageBucket(txBytes = 5_600L, rxBytes = 7_800L)),
        )

        val result = overviewLogic.getTodayBreakdown()

        assertEquals(3_400L, result.cellular.download)
        assertEquals(1_200L, result.cellular.upload)
        assertEquals(4_600L, result.cellular.total)
        assertEquals(7_800L, result.wifi.download)
        assertEquals(5_600L, result.wifi.upload)
        assertEquals(13_400L, result.wifi.total)
    }

    @Test
    fun `getTodayBreakdown totals are derived without extra bidirectional queries`() = runTest {
        stubUsage(
            mobileBuckets = listOf(UsageBucket(txBytes = 10L, rxBytes = 20L)),
            wifiBuckets = listOf(UsageBucket(txBytes = 30L, rxBytes = 40L)),
        )

        val result = overviewLogic.getTodayBreakdown()

        assertEquals(result.cellular.download + result.cellular.upload, result.cellular.total)
        assertEquals(result.wifi.download + result.wifi.upload, result.wifi.total)
        verify(exactly = 2) { networkStatsManager.querySummary(MOBILE_QUERY_INDEX, null, any(), any()) }
        verify(exactly = 2) { networkStatsManager.querySummary(WIFI_QUERY_INDEX, null, any(), any()) }
    }

    @Test
    fun `getTodayBreakdown returns zero usage when no buckets exist`() = runTest {
        stubUsage()

        val result = overviewLogic.getTodayBreakdown()

        assertEquals(0L, result.cellular.download)
        assertEquals(0L, result.cellular.upload)
        assertEquals(0L, result.cellular.total)
        assertEquals(0L, result.wifi.download)
        assertEquals(0L, result.wifi.upload)
        assertEquals(0L, result.wifi.total)
    }

    private fun stubUsage(
        mobileBuckets: List<UsageBucket> = emptyList(),
        wifiBuckets: List<UsageBucket> = emptyList(),
    ) {
        every { networkStatsManager.querySummary(MOBILE_QUERY_INDEX, null, any(), any()) } answers {
            summaryWithBuckets(mobileBuckets)
        }
        every { networkStatsManager.querySummary(WIFI_QUERY_INDEX, null, any(), any()) } answers {
            summaryWithBuckets(wifiBuckets)
        }
        every { networkStatsManager.querySummaryForDevice(MOBILE_QUERY_INDEX, null, any(), any()) } answers {
            deviceBucketFor(mobileBuckets)
        }
        every { networkStatsManager.querySummaryForDevice(WIFI_QUERY_INDEX, null, any(), any()) } answers {
            deviceBucketFor(wifiBuckets)
        }
    }

    private fun summaryWithBuckets(buckets: List<UsageBucket>): NetworkStats {
        val summary = mockk<NetworkStats>(relaxed = true)
        var bucketCount = 0
        val bucketSlot = slot<NetworkStats.Bucket>()
        every { summary.hasNextBucket() } answers { bucketCount < buckets.size }
        every { summary.getNextBucket(capture(bucketSlot)) } answers {
            val usage = buckets[bucketCount]
            setBucketFields(
                bucketSlot.captured,
                uid = usage.uid,
                txBytes = usage.txBytes,
                rxBytes = usage.rxBytes,
            )
            bucketCount++
            true
        }
        return summary
    }

    private fun deviceBucketFor(buckets: List<UsageBucket>): NetworkStats.Bucket {
        val bucket = NetworkStats.Bucket()
        setBucketFields(
            bucket,
            uid = -1,
            txBytes = buckets.sumOf { it.txBytes },
            rxBytes = buckets.sumOf { it.rxBytes },
        )
        return bucket
    }

    /**
     * Helper to set private/final fields on NetworkStats.Bucket using reflection.
     * This is necessary because Robolectric's shadow for Bucket doesn't always expose
     * setters for all fields across all SDK versions.
     */
    private fun setBucketFields(bucket: NetworkStats.Bucket, uid: Int, txBytes: Long, rxBytes: Long) {
        val fields = NetworkStats.Bucket::class.java.declaredFields
        fields.forEach { field ->
            field.isAccessible = true
            try {
                when (field.name) {
                    "uid", "mUid" -> field.set(bucket, uid)
                    "txBytes", "mTxBytes" -> field.set(bucket, txBytes)
                    "rxBytes", "mRxBytes" -> field.set(bucket, rxBytes)
                }
            } catch (e: Exception) {
                // Ignore if field doesn't exist in this Android version
            }
        }
    }

    private data class UsageBucket(
        val uid: Int = 1001,
        val txBytes: Long,
        val rxBytes: Long,
    )

    private companion object {
        const val MOBILE_QUERY_INDEX = 0
        const val WIFI_QUERY_INDEX = 1
    }
}
