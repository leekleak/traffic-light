package com.leekleak.trafficlight.ui.history

import androidx.lifecycle.ViewModel
import com.leekleak.trafficlight.database.AppUsage
import com.leekleak.trafficlight.database.DayUsage
import com.leekleak.trafficlight.database.HourlyUsageRepo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.time.LocalDate

class HistoryVM: ViewModel(), KoinComponent {
    val hourlyUsageRepo: HourlyUsageRepo by inject()

    fun dayUsage(date: LocalDate): Flow<DayUsage> =
        hourlyUsageRepo.singleDayUsageFlow(date)

    fun dayUsageBasic(date: LocalDate): Flow<DayUsage> = flow {
        emit(hourlyUsageRepo.calculateDayUsageBasic(date))
    }.flowOn(Dispatchers.IO)

    fun getAllAppUsage(date: LocalDate): Flow<List<AppUsage>> = flow {
        emit(hourlyUsageRepo.getAllAppUsage(date))
    }.flowOn(Dispatchers.IO)
}