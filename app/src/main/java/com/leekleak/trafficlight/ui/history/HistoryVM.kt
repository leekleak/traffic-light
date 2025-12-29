package com.leekleak.trafficlight.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.leekleak.trafficlight.database.AppUsage
import com.leekleak.trafficlight.database.HourlyUsageRepo
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.time.LocalDate

class HistoryVM: ViewModel(), KoinComponent {
    private val hourlyUsageRepo: HourlyUsageRepo by inject()
    private val _dateParams = MutableStateFlow(DateParams(LocalDate.now(), false))

    @OptIn(ExperimentalCoroutinesApi::class)
    val appList: StateFlow<List<AppUsage>> = _dateParams
        .flatMapLatest { params ->
            val (day, isMonth) = params
            if (!isMonth) {
                hourlyUsageRepo.getAllAppUsage(day, day)
            } else {
                val start = day.withDayOfMonth(1)
                val end = start.plusMonths(1)
                hourlyUsageRepo.getAllAppUsage(start, end)
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun updateQuery(day: LocalDate, showMonth: Boolean) {
        _dateParams.value = DateParams(day, showMonth)
    }
}

data class DateParams(val day: LocalDate, val showMonth: Boolean)