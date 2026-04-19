package com.leekleak.trafficlight.ui.settings

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.core.net.toUri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.leekleak.trafficlight.database.AppPreferenceRepo
import com.leekleak.trafficlight.services.UsageService
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

class SettingsVM(appPreferenceRepo: AppPreferenceRepo) : ViewModel() {
    val notification: StateFlow<Boolean> = appPreferenceRepo.notification
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)
    val liveNotification: StateFlow<Boolean> = appPreferenceRepo.liveNotification
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    suspend fun runUsageService(value: Boolean, context: Context) {
        delay(50) // Give time for database to update
        if (value) {
            UsageService.startService(context)
        } else {
            UsageService.stopService()
        }
    }

    fun openAppSettings(activity: Activity?) {
        activity?.startActivity(
            Intent(
                Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                "package:${activity.packageName}".toUri()
            )
        )
    }

    fun openNotificationChannelSettings(activity: Activity?, channel: String) {
        activity?.startActivity(
            Intent(Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS).apply {
                putExtra(Settings.EXTRA_APP_PACKAGE, activity.packageName)
                putExtra(Settings.EXTRA_CHANNEL_ID, channel)
            }
        )
    }
}