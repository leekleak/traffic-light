package com.leekleak.trafficlight.ui.permissions

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.provider.Settings
import androidx.core.net.toUri
import androidx.lifecycle.ViewModel
import org.koin.core.component.KoinComponent

class PermissionVM : ViewModel(), KoinComponent {
    @SuppressLint("BatteryLife")
    fun allowBackground(activity: Activity) {
        activity.startActivity(
            Intent(
                Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                "package:${activity.packageName}".toUri()
            )
        )
    }

    fun allowNotifications(activity: Activity) {
        activity.requestPermissions(
            arrayOf(Manifest.permission.POST_NOTIFICATIONS),
            1
        )
    }

    fun allowUsage(activity: Activity) {
        try {
            activity.startActivity(
                Intent(
                    Settings.ACTION_USAGE_ACCESS_SETTINGS,
                    "package:${activity.packageName}".toUri()
                )
            )
        }catch (e: Exception){// some device do not have separate usage access settings interface
            activity.startActivity(
                Intent(
                    Settings.ACTION_USAGE_ACCESS_SETTINGS
                )
            )
        }
    }
}