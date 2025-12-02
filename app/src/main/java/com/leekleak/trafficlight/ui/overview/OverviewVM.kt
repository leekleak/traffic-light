package com.leekleak.trafficlight.ui.overview

import androidx.lifecycle.ViewModel
import com.leekleak.trafficlight.charts.model.BarData
import com.leekleak.trafficlight.database.DayUsage
import com.leekleak.trafficlight.database.HourlyUsageRepo
import com.leekleak.trafficlight.services.UsageService
import com.leekleak.trafficlight.util.getName
import kotlinx.coroutines.flow.Flow
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.TextStyle

class OverviewVM : ViewModel(), KoinComponent {
    private val hourlyUsageRepo: HourlyUsageRepo by inject()
    val todayUsage: Flow<DayUsage> = UsageService.todayUsageFlow

    fun weekUsage(): List<BarData> {
        val data = MutableList(7) { i ->
            val x = DayOfWeek.entries[i].getName(TextStyle.SHORT_STANDALONE)
            BarData(x, 0.0, 0.0)
        }
        val now = LocalDate.now()

        for (i in 0..<now.dayOfWeek.value) {
            val usage = hourlyUsageRepo.calculateDayUsage(now.minusDays(i.toLong()))

            data[now.dayOfWeek.value - i - 1] += BarData(
                "",
                usage.totalCellular.toDouble(),
                usage.totalWifi.toDouble()
            )
        }
        return data
    }
}