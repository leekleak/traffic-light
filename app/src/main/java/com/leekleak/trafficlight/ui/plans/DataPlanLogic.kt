package com.leekleak.trafficlight.ui.plans

import com.leekleak.trafficlight.charts.model.BarData
import com.leekleak.trafficlight.database.AppUsage
import com.leekleak.trafficlight.database.DataPlan
import com.leekleak.trafficlight.database.DataType
import com.leekleak.trafficlight.database.TimeInterval
import com.leekleak.trafficlight.database.UsageQuery
import com.leekleak.trafficlight.model.DataUIDApp
import com.leekleak.trafficlight.model.NetworkUsageManager
import com.leekleak.trafficlight.util.MiniCardState
import com.leekleak.trafficlight.util.toTimestamp
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import kotlin.math.max

class DataPlanLogic(private val networkUsageManager: NetworkUsageManager) {
    suspend fun getDataSafety(dataPlan: DataPlan): MiniCardState {
        val planUsage = networkUsageManager.planUsage(dataPlan)
        val usageRatio = planUsage.toDouble() / dataPlan.dataMax.toDouble()
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

    suspend fun getTrend(dataPlan: DataPlan): Double {
        val nowStamp = LocalDateTime.now().toTimestamp()
        val hourAverage24 = networkUsageManager
            .getNetworkDataForType(nowStamp - 24 * 3_600_000, nowStamp, dataPlan.decryptedID, DataType.Mobile)
            .sumOf { it.total } / 24.0
        val weekAverage = networkUsageManager
            .getNetworkDataForType(
                nowStamp - 168 * 3_600_000,
                nowStamp - 24 * 3_600_000,
                dataPlan.decryptedID,
                DataType.Mobile
            )
            .sumOf { it.total } / 144.0

        return (hourAverage24 / max(weekAverage, 1.0) - 1) * 100.0
    }

    suspend fun getRemainingDailyBudget(dataPlan: DataPlan): Long {
        val planUsage = networkUsageManager.planUsage(dataPlan)
        val remaining = max(dataPlan.dataMax - planUsage, 0L)
        val dailyBudget = remaining / (dataPlan.getRemainingDuration().toDays() + 1)
        return dailyBudget
    }

    suspend fun getRemainingDailyBudgetToday(dataPlan: DataPlan): Long {
        val dailyBudget = getRemainingDailyBudget(dataPlan)
        if (dataPlan.interval == TimeInterval.DAY && dataPlan.intervalMultiplier == 1) return dailyBudget
        val todayUsage = networkUsageManager.totalDayUsage(
            query = UsageQuery(dataType = DataType.Mobile),
            startDate = LocalDate.now()
        )
        val remaining = max(dailyBudget - todayUsage, 0L)
        return remaining
    }

    suspend fun getWeekUsage(dataPlan: DataPlan): List<BarData> = networkUsageManager.getWeekUsage(dataPlan.decryptedID, false)

    suspend fun getTopAppUsage(dataPlan: DataPlan): List<AppUsage> {
        val todayUsage = networkUsageManager.getAllAppUsage(
            startStamp = dataPlan.getStartDate().toTimestamp(),
            endStamp = LocalDate.now().toTimestamp(),
            query1 = UsageQuery(DataType.Mobile),
            query2 = UsageQuery(DataType.None),
            subscriberId = dataPlan.decryptedID
        )
        return todayUsage.filter { !dataPlan.excludedApps.contains(it.app.uid) && it.app is DataUIDApp }.take(3)
    }
}