package com.leekleak.trafficlight.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.leekleak.trafficlight.database.AppUsage
import com.leekleak.trafficlight.database.DayUsage
import com.leekleak.trafficlight.database.HourlyUsageRepo
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.time.LocalDate

class HistoryVM: ViewModel(), KoinComponent {
    private val hourlyUsageRepo: HourlyUsageRepo by inject()
    private val dateParams = MutableStateFlow(DateParams(LocalDate.now(), false))

    @OptIn(ExperimentalCoroutinesApi::class)
    val appList: StateFlow<List<AppUsage>> = dateParams
        .flatMapLatest { (day, isMonth) ->
            if (!isMonth) {
                hourlyUsageRepo.getAllAppUsage(day, day)
            } else {
                val start = day.withDayOfMonth(1)
                val end = start.plusMonths(1).minusDays(1)
                hourlyUsageRepo.getAllAppUsage(start, end)
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    val totalUsage: Flow<DayUsage> = dateParams
        .flatMapLatest { (day, isMonth) ->
            flow {
                if (!isMonth) {
                    emit(hourlyUsageRepo.calculateDayUsageBasic(day, day))
                } else {
                    val start = day.withDayOfMonth(1)
                    val end = start.plusMonths(1).minusDays(1)
                    emit(hourlyUsageRepo.calculateDayUsageBasic(start, end))
                }
            }
        }

    fun appUsageSum(usage: List<AppUsage>): DayUsage {
        val dayUsage = DayUsage()
        for (i in usage) {
            dayUsage.totalWifi += i.usage.totalWifi
            dayUsage.totalCellular += i.usage.totalCellular
        }
        return dayUsage
    }

    fun updateQuery(day: LocalDate, showMonth: Boolean) {
        dateParams.value = DateParams(day, showMonth)
    }
}

data class DateParams(val day: LocalDate, val showMonth: Boolean)