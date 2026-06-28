package com.leekleak.trafficlight.ui.overview

import com.leekleak.trafficlight.charts.model.BarData
import com.leekleak.trafficlight.database.AppUsage
import com.leekleak.trafficlight.database.DataDirection
import com.leekleak.trafficlight.database.DataType
import com.leekleak.trafficlight.database.UsageQuery
import com.leekleak.trafficlight.model.AppManager.Companion.allApp
import com.leekleak.trafficlight.model.NetworkUsageManager
import com.leekleak.trafficlight.util.toTimestamp
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import java.time.LocalDate
import java.time.LocalDateTime
import kotlin.math.max
import kotlin.math.roundToInt

data class TransportUsage(
    val download: Long = 0,
    val upload: Long = 0,
) {
    val total: Long
        get() = download + upload
}

data class TodayBreakdown(
    val cellular: TransportUsage = TransportUsage(),
    val wifi: TransportUsage = TransportUsage(),
)

class OverviewLogic(val networkUsageManager: NetworkUsageManager) {
    suspend fun getPrediction(query: UsageQuery): Long {
        val hour = LocalDateTime.now().hour
        val hoursLeft = 23 - hour
        val nowStamp = LocalDateTime.now().toTimestamp()
        val last24HourUsage = networkUsageManager
            .getNetworkDataForType(nowStamp - 24 * 3_600_000, nowStamp, null, DataType.Mobile)
            .sumOf { it.total }
        val todayUsage =
            networkUsageManager.totalDayUsage(query, LocalDate.now())

        val out = coroutineScope {
            (1..4).map { i ->
                async {
                    val pivotStamp = nowStamp - i * 24 * 7 * 3_600_000L
                    val futureHours = networkUsageManager
                        .getNetworkDataForType(
                            pivotStamp,
                            pivotStamp + hoursLeft * 3_600_000,
                            null,
                            DataType.Mobile
                        ).sumOf { it.total }
                    val pastHours = networkUsageManager
                        .getNetworkDataForType(
                            pivotStamp - 24 * 3_600_000,
                            pivotStamp,
                            null,
                            DataType.Mobile
                        ).sumOf { it.total }

                    (pastHours) to (futureHours + pastHours)
                }
            }.awaitAll()
        }

        val hourSum = out.sumOf { it.first }.toDouble()
        val daySum = out.sumOf { it.second }.toDouble()

        return if (hourSum == 0.0) {
            todayUsage
        } else {
            (last24HourUsage * (daySum / hourSum - 1)).toLong() + todayUsage
        }
    }

    suspend fun getTodayUsage(query: UsageQuery): Long {
        return networkUsageManager.totalDayUsage(query, LocalDate.now())
    }

    suspend fun getTodayBreakdown(): TodayBreakdown {
        val today = LocalDate.now()
        return TodayBreakdown(
            cellular = getTransportUsage(DataType.Mobile, today),
            wifi = getTransportUsage(DataType.Wifi, today),
        )
    }

    private suspend fun getTransportUsage(dataType: DataType, date: LocalDate): TransportUsage {
        return TransportUsage(
            download = networkUsageManager.totalDayUsage(
                UsageQuery(dataType, DataDirection.Download),
                date,
            ),
            upload = networkUsageManager.totalDayUsage(
                UsageQuery(dataType, DataDirection.Upload),
                date,
            ),
        )
    }

    suspend fun getTrend(query: UsageQuery): Int {
        val nowStamp = LocalDateTime.now().toTimestamp()
        // Last 24 hours
        val hourAverage24 = networkUsageManager
            .getNetworkDataForType(nowStamp - 24 * 3_600_000, nowStamp, null, query.dataType)
            .sumOf { it.total } / 24.0
        // Last week average excluding last 24 hours
        val weekAverage = networkUsageManager
            .getNetworkDataForType(
                nowStamp - 168 * 3_600_000,
                nowStamp - 24 * 3_600_000,
                null,
                query.dataType
            )
            .sumOf { it.total } / 144.0

        return ((hourAverage24 / max(weekAverage, 1.0) - 1) * 100.0).roundToInt()
    }

    suspend fun getWeekUsage(query: UsageQuery): List<BarData> = networkUsageManager.getWeekUsage(null, query.dataType)

    suspend fun getTopAppUsage(query: UsageQuery): List<AppUsage> {
        val todayUsage = networkUsageManager.getAllAppUsage(
            startStamp = LocalDate.now().toTimestamp(),
            endStamp = LocalDateTime.now().toTimestamp(),
            query1 = query,
            query2 = UsageQuery(DataType.None),
        )
        return todayUsage.filter { it.app != allApp }.take(3)
    }
}
