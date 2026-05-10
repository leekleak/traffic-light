package com.leekleak.trafficlight.ui.plans

import com.leekleak.trafficlight.database.DataPlan
import com.leekleak.trafficlight.database.DataType
import com.leekleak.trafficlight.database.UsageQuery
import com.leekleak.trafficlight.model.NetworkUsageManager
import java.time.LocalDate
import kotlin.math.max

class DataPlanLogic(private val networkUsageManager: NetworkUsageManager) {
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