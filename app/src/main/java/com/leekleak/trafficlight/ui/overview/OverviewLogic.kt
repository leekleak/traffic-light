package com.leekleak.trafficlight.ui.overview

import com.leekleak.trafficlight.charts.model.BarData
import com.leekleak.trafficlight.database.DataType
import com.leekleak.trafficlight.database.UsageQuery
import com.leekleak.trafficlight.model.NetworkUsageManager
import com.leekleak.trafficlight.util.getName
import com.leekleak.trafficlight.util.toTimestamp
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.TextStyle
import java.time.temporal.WeekFields
import java.util.Locale
import kotlin.math.max

class OverviewLogic(val networkUsageManager: NetworkUsageManager) {
    suspend fun getPrediction(): Long {
        val hour = LocalDateTime.now().hour
        val hoursLeft = 23 - hour
        val nowStamp = LocalDateTime.now().toTimestamp()
        val last24HourUsage = networkUsageManager
            .getNetworkDataForType(nowStamp - 24 * 3_600_000, nowStamp, null, DataType.Mobile)
            .sumOf { it.total }
        val todayUsage =
            networkUsageManager.totalDayUsage(UsageQuery(DataType.Mobile), LocalDate.now())

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

    suspend fun getTodayUsage(): Long {
        return networkUsageManager.totalDayUsage(UsageQuery(DataType.Mobile), LocalDate.now())
    }

    suspend fun getTrend(): Double {
        val nowStamp = LocalDateTime.now().toTimestamp()
        // Last 24 hours
        val hourAverage24 = networkUsageManager
            .getNetworkDataForType(nowStamp - 24 * 3_600_000, nowStamp, null, DataType.Mobile)
            .sumOf { it.total } / 24.0
        // Last week average excluding last 24 hours
        val weekAverage = networkUsageManager
            .getNetworkDataForType(
                nowStamp - 168 * 3_600_000,
                nowStamp - 24 * 3_600_000,
                null,
                DataType.Mobile
            )
            .sumOf { it.total } / 144.0

        return (hourAverage24 / max(weekAverage, 1.0) - 1) * 100.0
    }

    suspend fun getWeekUsage(): List<BarData> {
        val field = WeekFields.of(Locale.getDefault())
        val firstDay = field.firstDayOfWeek
        val data: MutableList<BarData> = MutableList(7) { i ->
            val x = firstDay.plus(i.toLong()).getName(TextStyle.SHORT_STANDALONE)
            BarData(x, 0, 0)
        }
        val now = LocalDate.now()
        val daysPassed = now.get(field.dayOfWeek()) - 1

        coroutineScope {
            (0..daysPassed).map { i ->
                async {
                    val now = now.minusDays(daysPassed.toLong() - i)
                    val usage1 = networkUsageManager.totalDayUsage(UsageQuery(DataType.Mobile), now)
                    val usage2 = networkUsageManager.totalDayUsage(UsageQuery(DataType.Wifi), now)
                    data[i] = data[i].copy(y1 = usage1, y2 = usage2)
                }
            }.awaitAll()
        }
        return data.toList()
    }
}