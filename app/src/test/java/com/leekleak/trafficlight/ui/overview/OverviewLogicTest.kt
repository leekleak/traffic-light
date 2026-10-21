package com.leekleak.trafficlight.ui.overview

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
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class OverviewLogicTest {

    @Test
    fun `prediction uses the selected data type and applies uid and direction filters`() = runTest {
        val networkUsageManager = mockk<NetworkUsageManager>(relaxed = true)
        val dataUID = mockk<DataUID>()
        every { dataUID.uid } returns 1000
        every { dataUID.uidQuery } returns 1000

        val query = UsageQuery(
            dataType = DataType.Wifi,
            dataDirection = DataDirection.Download,
            dataUID = dataUID,
        )

        coEvery {
            networkUsageManager.getNetworkDataForType(any(), any(), null, DataType.Wifi)
        } returns listOf(
            UsageData(upload = 1000L, download = 40L, uid = 1000),
            UsageData(upload = 1000L, download = 7L, uid = 2000),
        )

        val prediction = OverviewLogic(networkUsageManager).getPrediction(query)

        // 40 download bytes for uid 1000 are used for both the 24h and weekly windows,
        // so the ratio is 1 and the predicted total is 40. Mobile data must not leak in.
        assertEquals(40L, prediction)
        coVerify(exactly = 0) {
            networkUsageManager.getNetworkDataForType(any(), any(), null, DataType.Mobile)
        }
    }
}
