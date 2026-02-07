package com.leekleak.trafficlight.ui.overview

import android.telephony.SubscriptionInfo
import androidx.lifecycle.ViewModel
import com.leekleak.trafficlight.database.DataPlan
import com.leekleak.trafficlight.database.DataPlanDao
import com.leekleak.trafficlight.database.DayUsage
import com.leekleak.trafficlight.database.HourlyUsageRepo
import com.leekleak.trafficlight.model.PreferenceRepo
import com.leekleak.trafficlight.model.ShizukuDataManager
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
    val shizukuDataManager: ShizukuDataManager by inject()
    val dataPlanDao: DataPlanDao by inject()

    fun getSubscriptionInfos(): Flow<List<SubscriptionInfo>> = flow {
        emit(shizukuDataManager.getSubscriptionInfos())
    }.flowOn(Dispatchers.IO)
    fun getSubscriberID(subscriptionId: Int): Flow<String?> = flow {
        emit(shizukuDataManager.getSubscriberID(subscriptionId))
    }.flowOn(Dispatchers.IO)

    fun getSubscriberIDHasDataPlan(subscriberID: String): Flow<Boolean> = flow {
        emit(dataPlanDao.get(subscriberID) != null)
    }.flowOn(Dispatchers.IO)

    fun getDataPlan(subscriberID: String): Flow<DataPlan> = flow {
        emit(dataPlanDao.get(subscriberID) ?: DataPlan(subscriberID))
    }.flowOn(Dispatchers.IO)

    val todayUsage: Flow<DayUsage> = UsageService.todayUsageFlow
}