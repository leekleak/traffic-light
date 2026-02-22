package com.leekleak.trafficlight.ui.overview

import androidx.lifecycle.ViewModel
import com.leekleak.trafficlight.database.DataPlan
import com.leekleak.trafficlight.database.DataPlanDao
import com.leekleak.trafficlight.database.DayUsage
import com.leekleak.trafficlight.database.HourlyUsageRepo
import com.leekleak.trafficlight.model.PreferenceRepo
import com.leekleak.trafficlight.services.UsageService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class OverviewVM : ViewModel(), KoinComponent {
    val preferenceRepo: PreferenceRepo by inject()
    val hourlyUsageRepo: HourlyUsageRepo by inject()
    val dataPlanDao: DataPlanDao by inject()

    fun getDataPlan(subscriberID: String): Flow<DataPlan> = flow {
        emit(dataPlanDao.get(subscriberID) ?: DataPlan(subscriberID))
    }.flowOn(Dispatchers.IO)

    val todayUsage: Flow<DayUsage> = UsageService.todayUsageFlow
}