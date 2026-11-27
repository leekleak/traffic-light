package com.leekleak.trafficlight.ui.overview

import com.leekleak.core.util.toTimestamp
import com.leekleak.trafficlight.database.DataDirection
import com.leekleak.trafficlight.database.DataType
import com.leekleak.trafficlight.database.UsageQuery
import com.leekleak.trafficlight.model.DataUID
import com.leekleak.trafficlight.model.NetworkUsageManager
import com.leekleak.trafficlight.model.UsageData
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDateTime

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class OverviewLogicTest {

    private fun usageQuery(): UsageQuery {
        val dataUID = mockk<DataUID>()
        every { dataUID.uid } returns 1000
        every { dataUID.uidQuery } returns 1000
        return UsageQuery(
            dataType = DataType.Wifi,
            dataDirection = DataDirection.Download,
            dataUID = dataUID,
        )
    }

    @Test
    fun `range usage starts at the range and applies uid and direction filters`() = runTest {
        val networkUsageManager = mockk<NetworkUsageManager>(relaxed = true)
        val startStamp = slot<Long>()

        coEvery {
            networkUsageManager.getNetworkDataForType(capture(startStamp), any(), null, DataType.Wifi)
        } returns listOf(
            UsageData(upload = 1000L, download = 40L, uid = 1000),
            UsageData(upload = 1000L, download = 7L, uid = 2000),
        )

        val usage = OverviewLogic(networkUsageManager).getUsage(usageQuery(), OverviewRange.Week)

        // 40 download bytes for uid 1000 are used, mobile data must not leak in.
        assertEquals(40L, usage)
        assertEquals(OverviewRange.Week.startDate().toTimestamp(), startStamp.captured)
        coVerify(exactly = 0) {
            networkUsageManager.getNetworkDataForType(any(), any(), null, DataType.Mobile)
        }
    }

    @Test
    fun `today graph sorts hourly buckets and sums both directions`() = runTest {
        val networkUsageManager = mockk<NetworkUsageManager>(relaxed = true)

        coEvery {
            networkUsageManager.getNetworkDataForTypeHourly(any(), any(), null, DataType.Wifi, null)
        } returns listOf(
            UsageData(upload = 10L, download = 20L, start = LocalDateTime.now().withHour(4)),
            UsageData(upload = 1L, download = 2L, start = LocalDateTime.now().withHour(2)),
        )

        val graph = OverviewLogic(networkUsageManager).getGraphUsage(usageQuery(), OverviewRange.Today)

        assertEquals(listOf(3f, 30f), graph.map { it.value })
        assertEquals(listOf(2, 4), graph.map { it.start.hour })
    }

    @Test
    fun `highlight indices pick the highest non zero values respecting the gap`() {
        val values = listOf(0f, 50f, 10f, 90f, 80f, 0f)

        assertEquals(listOf(3, 1), highlightIndices(values, count = 2, minGap = 2))
    }

    @Test
    fun `highlight indices ignore zeros and cap the number of markers`() {
        val values = listOf(1f, 0f, 5f, 4f, 3f, 2f)

        assertEquals(listOf(2, 3, 4), highlightIndices(values, count = 3, minGap = 1))
    }

    @Test
    fun `top apps query starts at the selected range`() = runTest {
        val networkUsageManager = mockk<NetworkUsageManager>(relaxed = true)
        val startStamp = slot<Long>()

        coEvery {
            networkUsageManager.getAllAppUsage(capture(startStamp), any(), any(), any())
        } returns emptyList()

        OverviewLogic(networkUsageManager).getTopAppUsage(usageQuery(), OverviewRange.Month)

        assertEquals(OverviewRange.Month.startDate().toTimestamp(), startStamp.captured)
    }
}
