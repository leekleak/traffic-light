package com.leekleak.trafficlight.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.leekleak.trafficlight.database.AppPreferenceRepo
import com.leekleak.trafficlight.model.DataPlanManager
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

class SettingsVM(appPreferenceRepo: AppPreferenceRepo, dataPlanManager: DataPlanManager) : ViewModel() {
    val notification: StateFlow<Boolean> = appPreferenceRepo.notification
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)
    val liveNotification: StateFlow<Boolean> = appPreferenceRepo.liveNotification
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val activePlanNotificationsCount: StateFlow<Int> = dataPlanManager.activePlanNotificationsCount
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)
}