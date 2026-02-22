package com.leekleak.trafficlight.services

import android.Manifest.permission.POST_NOTIFICATIONS
import android.annotation.SuppressLint
import android.app.Activity
import android.app.AppOpsManager
import android.content.Context
import android.content.Context.POWER_SERVICE
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.PowerManager
import android.os.Process.myUid
import android.provider.Settings
import androidx.core.net.toUri
import com.leekleak.trafficlight.model.ShizukuDataManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import rikka.shizuku.Shizuku

class PermissionManager(
    private val context: Context
) : KoinComponent {
    val shizukuManager: ShizukuDataManager by inject()

    private val _backgroundPermission = MutableStateFlow(false)
    val backgroundPermissionFlow = _backgroundPermission.asStateFlow()

    private val _usagePermission = MutableStateFlow(true)
    val usagePermissionFlow = _usagePermission.asStateFlow()

    private val _notificationPermission = MutableStateFlow(false)
    val notificationPermissionFlow = _notificationPermission.asStateFlow()

    private val _shizukuRunning = MutableStateFlow(false)
    val shizukuRunningFlow = _shizukuRunning.asStateFlow()

    private val _shizukuPermission = MutableStateFlow(false)
    val shizukuPermissionFlow = _shizukuPermission.asStateFlow()

    @SuppressLint("BatteryLife")
    fun askBackgroundPermission(activity: Activity?) {
        activity?.startActivity(
            Intent(
                Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                "package:${activity.packageName}".toUri()
            )
        )
    }

    fun askUsagePermission(activity: Activity?) {
        try {
            activity?.startActivity(
                Intent(
                    Settings.ACTION_USAGE_ACCESS_SETTINGS,
                    "package:${activity.packageName}".toUri()
                )
            )
        } catch (_: Exception){ // some device do not have separate usage access settings interface
            activity?.startActivity(
                Intent(
                    Settings.ACTION_USAGE_ACCESS_SETTINGS
                )
            )
        }
    }

    fun openUsagePermissionHelp(activity: Activity?) {
        activity?.startActivity(
            Intent(
                Intent.ACTION_VIEW,
                "https://github.com/leekleak/traffic-light/wiki/Troubleshooting#usage-data-access-denied".toUri()
            )
        )
    }

    fun update() {
        val packageName: String? = context.packageName
        val pm = context.getSystemService(POWER_SERVICE) as PowerManager
        _backgroundPermission.value = pm.isIgnoringBatteryOptimizations(packageName)

        val appOpsManager = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = appOpsManager.checkOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            myUid(),
            context.packageName
        )
        _usagePermission.value = mode == AppOpsManager.MODE_ALLOWED

        _notificationPermission.value = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.checkSelfPermission(POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }

        _shizukuRunning.value = Shizuku.pingBinder()
        if (_shizukuRunning.value) {
            _shizukuPermission.value = Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
            if (_shizukuPermission.value) {
                shizukuManager.updateSimData()
            }
        }
    }
}