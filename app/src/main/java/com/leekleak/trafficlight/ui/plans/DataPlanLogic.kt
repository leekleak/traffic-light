package com.leekleak.trafficlight.ui.plans

import com.leekleak.core.charts.model.BarData
import com.leekleak.trafficlight.database.AppUsage
import com.leekleak.trafficlight.database.DataPlan
import com.leekleak.trafficlight.database.DataPlanSnapshot
import com.leekleak.trafficlight.database.DataType
import com.leekleak.trafficlight.database.TimeInterval
import com.leekleak.trafficlight.database.UsageQuery
import com.leekleak.trafficlight.model.AppManager.Companion.allApp
import com.leekleak.trafficlight.model.NetworkUsageManager
import com.leekleak.core.util.MiniCardState
import com.leekleak.core.util.toTimestamp
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import kotlin.math.max
import kotlin.math.roundToInt

class DataPlanLogic(val networkUsageManager: NetworkUsageManager) {

    private suspend fun getFilteredUsage(dataPlan: DataPlan, startStamp: Long, endStamp: Long): Long =
        networkUsageManager
            .getNetworkDataForType(startStamp, endStamp, dataPlan.decryptedID, DataType.Mobile)
            .filter { usage -> usage.uid?.let { it in dataPlan.excludedApps } != true }
            .sumOf { it.total }

    fun getDataSafety(dataPlan: DataPlan, snapshot: DataPlanSnapshot): MiniCardState {
        val totalMax = dataPlan.mainDataSize.byteValue
        if (totalMax <= 0L) return MiniCardState.NEUTRAL

        val usageRatio = snapshot.mainDataUsed.toDouble() / totalMax.toDouble()
        val startDate = dataPlan.getStartDate()
        val endDate = dataPlan.getStartDate(next = true)
        val timeRatio = Duration.between(startDate, LocalDateTime.now()).seconds.toDouble() / Duration.between(startDate, endDate).seconds.toDouble()
        val difference = if (usageRatio.isNaN()) 0.0 else usageRatio - timeRatio

        return when {
            (usageRatio > 0.95) -> MiniCardState.NEGATIVE
            (difference <= 0.0 || usageRatio < 0.1) -> MiniCardState.POSITIVE
            (difference <= 0.1) -> MiniCardState.NEUTRAL
            else -> MiniCardState.NEGATIVE
        }
    }

    suspend fun getTrend(dataPlan: DataPlan): Int {
        val nowStamp = LocalDateTime.now().toTimestamp()
        val hourAverage24 = getFilteredUsage(dataPlan, nowStamp - 24 * 3_600_000, nowStamp) / 24.0
        val weekAverage = getFilteredUsage(
            dataPlan,
            nowStamp - 168 * 3_600_000,
            nowStamp - 24 * 3_600_000,
        ) / 144.0

        return ((hourAverage24 / max(weekAverage, 1.0) - 1) * 100.0).roundToInt()
    }

    fun getRemainingDailyBudget(dataPlan: DataPlan, snapshot: DataPlanSnapshot): Long {
        val remaining = max(dataPlan.mainDataSize.byteValue - snapshot.mainDataUsed, 0L)
        val dailyBudget = remaining / (dataPlan.getRemainingDuration().toDays() + 1)
        return dailyBudget
    }

    suspend fun getRemainingDailyBudgetToday(dataPlan: DataPlan, snapshot: DataPlanSnapshot): Long {
        val dailyBudget = getRemainingDailyBudget(dataPlan, snapshot)
        if (dataPlan.interval == TimeInterval.DAY && dataPlan.intervalMultiplier == 1) return dailyBudget
        val today = LocalDate.now()
        val todayUsage = networkUsageManager.totalDayUsage(
            query = UsageQuery(dataType = DataType.Mobile),
            startDate = today,
            subscriberId = dataPlan.decryptedID,
            endDate = today.plusDays(1),
            excludedApps = dataPlan.excludedApps.toSet(),
        )
        val remaining = max(dailyBudget - todayUsage, 0L)
        return remaining
    }

    suspend fun getWeekUsage(dataPlan: DataPlan): List<BarData> = networkUsageManager.getWeekUsage(
        subscriberId = dataPlan.decryptedID,
        dataType = DataType.Mobile,
        excludedApps = dataPlan.excludedApps.toSet(),
    )

    suspend fun getTopAppUsage(dataPlan: DataPlan): List<AppUsage> {
        val todayUsage = networkUsageManager.getAllAppUsage(
            startStamp = dataPlan.getStartDate().toTimestamp(),
            endStamp = LocalDate.now().plusDays(1).toTimestamp(),
            query1 = UsageQuery(DataType.Mobile),
            query2 = UsageQuery(DataType.None),
            subscriberId = dataPlan.decryptedID
        )
        return todayUsage
            .filter { it.app != allApp && it.app.uid !in dataPlan.excludedApps }
            .take(3)
    }
}