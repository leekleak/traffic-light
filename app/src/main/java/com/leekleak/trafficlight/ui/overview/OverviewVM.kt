package com.leekleak.trafficlight.ui.overview

import androidx.lifecycle.ViewModel
import com.leekleak.trafficlight.database.DataPlan
import com.leekleak.trafficlight.database.DataPlanDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn

class OverviewVM(
    private val dataPlanDao: DataPlanDao,
) : ViewModel() {
    fun getDataPlan(subscriberID: String): Flow<DataPlan> = flow {
        emit(dataPlanDao.get(subscriberID) ?: DataPlan(subscriberID))
    }.flowOn(Dispatchers.IO)
}