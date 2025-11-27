package com.leekleak.trafficlight.ui.settings

import android.app.Activity
import android.content.Intent
import android.provider.Settings
import androidx.core.net.toUri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.leekleak.trafficlight.database.HourlyUsageRepo
import com.leekleak.trafficlight.model.PreferenceRepo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class SettingsVM : ViewModel(), KoinComponent {
    private val preferenceRepo: PreferenceRepo by inject()

    private val hourlyUsageRepo: HourlyUsageRepo by inject()

    val modeAOD = preferenceRepo.modeAOD.stateIn(viewModelScope, SharingStarted.WhileSubscribed(), false)
    fun setModeAOD(enabled: Boolean) = preferenceRepo.setModeAOD(enabled)
    val bigIcon = preferenceRepo.bigIcon.stateIn(viewModelScope, SharingStarted.WhileSubscribed(), false)
    fun setBigIcon(enabled: Boolean) = preferenceRepo.setBigIcon(enabled)
    val speedBits = preferenceRepo.speedBits.stateIn(viewModelScope, SharingStarted.WhileSubscribed(), false)
    fun setSpeedBits(enabled: Boolean) = preferenceRepo.setSpeedBits(enabled)

    val forceFallback = preferenceRepo.forceFallback.stateIn(viewModelScope, SharingStarted.WhileSubscribed(), false)
    fun setForceFallback(enabled: Boolean) = preferenceRepo.setForceFallback(enabled)
    val dbSize = hourlyUsageRepo.getDBSize().stateIn(viewModelScope, SharingStarted.WhileSubscribed(), 0)
    fun clearDB() {
        viewModelScope.launch(Dispatchers.IO) {
            hourlyUsageRepo.clearDB()
        }
    }

    fun openGithub(activity: Activity?) {
        activity?.startActivity(
            Intent(
                Intent.ACTION_VIEW,
                "https://github.com/leekleak/traffic-light".toUri()
            )
        )
    }

    fun openAppSettings(activity: Activity?) {
        activity?.startActivity(
            Intent(
                Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                "package:${activity.packageName}".toUri()
            )
        )
    }
}