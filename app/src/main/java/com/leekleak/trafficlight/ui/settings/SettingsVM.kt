package com.leekleak.trafficlight.ui.settings

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.core.net.toUri
import androidx.lifecycle.ViewModel
import com.leekleak.trafficlight.services.UsageService
import kotlinx.coroutines.delay
import org.koin.core.component.KoinComponent

class SettingsVM : ViewModel(), KoinComponent {
    suspend fun runUsageService(value: Boolean, context: Context) {
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