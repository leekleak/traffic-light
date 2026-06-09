package com.leekleak.trafficlight.ui.overview

import com.leekleak.trafficlight.charts.model.BarData
import com.leekleak.trafficlight.database.AppUsage
import com.leekleak.trafficlight.database.DataType
import com.leekleak.trafficlight.database.UsageQuery
import com.leekleak.trafficlight.model.DataUIDApp
import com.leekleak.trafficlight.model.NetworkUsageManager
import com.leekleak.trafficlight.util.toTimestamp
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import java.time.LocalDate
import java.time.LocalDateTime
import kotlin.math.max

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

    suspend fun getTrend(query: UsageQuery): Double {
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

        return (hourAverage24 / max(weekAverage, 1.0) - 1) * 100.0
    }

    suspend fun getWeekUsage(): List<BarData> = networkUsageManager.getWeekUsage(null, true)

    suspend fun getTopAppUsage(query: UsageQuery): List<AppUsage> {
        val todayUsage = networkUsageManager.getAllAppUsage(
            startStamp = LocalDate.now().toTimestamp(),
            endStamp = LocalDateTime.now().toTimestamp(),
            query1 = query,
            query2 = UsageQuery(DataType.None),
        )
        return todayUsage.filter { it.app is DataUIDApp }.take(3)
    }
}