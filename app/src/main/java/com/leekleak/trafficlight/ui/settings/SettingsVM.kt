package com.leekleak.trafficlight.ui.settings

import android.app.Activity
import android.content.Intent
import android.provider.Settings
import androidx.core.net.toUri
import androidx.lifecycle.ViewModel
import com.leekleak.trafficlight.database.HourlyUsageRepo
import com.leekleak.trafficlight.model.PreferenceRepo
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class SettingsVM : ViewModel(), KoinComponent {
    val preferenceRepo: PreferenceRepo by inject()
    val hourlyUsageRepo: HourlyUsageRepo by inject()

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