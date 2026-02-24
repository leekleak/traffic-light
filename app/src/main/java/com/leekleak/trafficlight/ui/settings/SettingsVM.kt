package com.leekleak.trafficlight.ui.settings

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.core.net.toUri
import androidx.lifecycle.ViewModel
import com.leekleak.trafficlight.model.PreferenceRepo
import com.leekleak.trafficlight.services.UsageService
import kotlinx.coroutines.delay
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class SettingsVM : ViewModel(), KoinComponent {
    private val preferenceRepo: PreferenceRepo by inject()

    suspend fun setNotifications(value: Boolean, context: Context) {
        preferenceRepo.setNotification(value)
        delay(50) // Give time for database to update
        if (value) {
            UsageService.startService(context)
        } else {
            UsageService.stopService()
        }
    }

    fun openLink(activity: Activity?, link: String) {
        activity?.startActivity(
            Intent(
                Intent.ACTION_VIEW,
                link.toUri()
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