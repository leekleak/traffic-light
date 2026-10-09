package com.leekleak.trafficlight.model

import android.Manifest.permission.POST_NOTIFICATIONS
import android.app.Activity
import android.app.AppOpsManager
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Context.POWER_SERVICE
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.PowerManager
import android.os.Process.myUid
import android.provider.Settings
import android.widget.Toast
import androidx.core.net.toUri
import com.leekleak.trafficlight.R
import com.leekleak.trafficlight.database.AppPreferenceRepo
import com.leekleak.trafficlight.integrations.ShizukuServicesProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

class PermissionManager(
    private val context: Context,
    scope: CoroutineScope,
    appPreferenceRepo: AppPreferenceRepo,
    private val shizukuServicesProvider: ShizukuServicesProvider
) {
    val backgroundPermission: StateFlow<Boolean>
        field = MutableStateFlow(false)

    val usagePermission: StateFlow<Boolean>
        field = MutableStateFlow(false)

    val notificationPermission: StateFlow<Boolean>
        field = MutableStateFlow(false)

    val shizukuRunning: StateFlow<Boolean>
        field = MutableStateFlow(false)

    val shizukuPermission: StateFlow<Boolean>
        field = MutableStateFlow(false)

    /**
     * Technically internet permission should always be granted. Unfortunately, that's not necessarily
     * the case in GrapheneOS. While the internet permission should not affect network counting at all,
     * whenever the permission is rejected, GrapheneOS also spoofs all system calls such as available
     * network interfaces and transport capabilities to indicate that no network is configured.
     *
     * In such cases we should just fall back and assume the device is connected.
     */
    val internetPermission: StateFlow<Boolean>
        field = MutableStateFlow(true)

    init {
        scope.launch {
            combine(
                appPreferenceRepo.shizukuTracking,
                shizukuPermission,
                shizukuRunning
            ) {
                setting, permission, running ->
                return@combine Triple(setting, permission, running)
            }.collectLatest { (setting, permission, running) ->
                if (setting && permission && running) {
                    shizukuServicesProvider.enable()
                } else if (!setting) {
                    shizukuServicesProvider.disable()
                    shizukuServicesProvider.updateSimData()
                }
            }
        }
    }

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
        activity ?: return
        val intent = Intent(
            Intent.ACTION_VIEW,
            "https://github.com/leekleak/traffic-light/wiki/Troubleshooting#usage-data-access-denied".toUri()
        )
        try {
            activity.startActivity(intent)
        } catch (_: ActivityNotFoundException) {
            Toast.makeText(activity, context.getString(R.string.no_browser_found), Toast.LENGTH_SHORT).show()
        }
    }

    fun update() {
        val packageName: String? = context.packageName
        val pm = context.getSystemService(POWER_SERVICE) as PowerManager
        backgroundPermission.value = pm.isIgnoringBatteryOptimizations(packageName)

        internetPermission.value = context.checkSelfPermission(android.Manifest.permission.INTERNET) == PackageManager.PERMISSION_GRANTED

        val appOpsManager = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = appOpsManager.checkOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            myUid(),
            context.packageName
        )
        usagePermission.value = mode == AppOpsManager.MODE_ALLOWED

        notificationPermission.value = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.checkSelfPermission(POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }

        shizukuRunning.value = shizukuServicesProvider.shizukuRunning()
        if (shizukuRunning.value) {
            shizukuPermission.value = shizukuServicesProvider.shizukuPermission() == PackageManager.PERMISSION_GRANTED
        } else {
            shizukuPermission.value = false
        }
    }
}