package com.leekleak.trafficlight.ui.plans

import com.leekleak.trafficlight.database.DataPlan
import com.leekleak.trafficlight.database.DataType
import com.leekleak.trafficlight.database.UsageQuery
import com.leekleak.trafficlight.model.NetworkUsageManager
import com.leekleak.trafficlight.util.MiniCardState
import com.leekleak.trafficlight.util.toTimestamp
import java.time.LocalDate
import java.time.LocalDateTime
import kotlin.math.max

class DataPlanLogic(private val networkUsageManager: NetworkUsageManager) {
    suspend fun getDataSafety(dataPlan: DataPlan): MiniCardState {
        val dailyBudget = getRemainingDailyBudget(dataPlan)
        val todayUsage = networkUsageManager.totalDayUsage(
            query = UsageQuery(dataType = DataType.Mobile),
            startDate = LocalDate.now()
        )
        val remaining = max(dailyBudget - todayUsage, 0L)
        return MiniCardState.NEGATIVE
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
        val dailyBudget = remaining / dataPlan.getRemainingDays()
        return dailyBudget
    }

    suspend fun getRemainingDailyBudgetToday(dataPlan: DataPlan): Long {
        val dailyBudget = getRemainingDailyBudget(dataPlan)
        val todayUsage = networkUsageManager.totalDayUsage(
            query = UsageQuery(dataType = DataType.Mobile),
            startDate = LocalDate.now()
        )
        val remaining = max(dailyBudget - todayUsage, 0L)
        return remaining
    }
}