package com.leekleak.trafficlight.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.leekleak.trafficlight.database.AppPreferenceRepo
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

class SettingsVM(appPreferenceRepo: AppPreferenceRepo) : ViewModel() {
    val notification: StateFlow<Boolean> = appPreferenceRepo.notification
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)
    val liveNotification: StateFlow<Boolean> = appPreferenceRepo.liveNotification
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)
}