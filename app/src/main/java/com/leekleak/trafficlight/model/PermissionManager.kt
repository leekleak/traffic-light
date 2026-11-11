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
import com.leekleak.core.R
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
    private val scope: CoroutineScope,
    private val appPreferenceRepo: AppPreferenceRepo,
    private val shizukuServicesProvider: ShizukuServicesProvider
) {
    val backgroundPermission: StateFlow<Boolean>
        field = MutableStateFlow(false)

    val usagePermission: StateFlow<Boolean>
        field = MutableStateFlow(false)

    val notificationPermission: StateFlow<Boolean>
        field = MutableStateFlow(false)

    val overlayPermission: StateFlow<Boolean>
        field = MutableStateFlow(false)

    val shizukuRunning: StateFlow<Boolean>
        field = MutableStateFlow(false)

    val shizukuPermission: StateFlow<Boolean>
        field = MutableStateFlow(false)

    private var pendingShizukuGrant: (() -> Unit)? = null

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
        scope.launch {
            shizukuPermission.collect { granted ->
                if (granted) {
                    pendingShizukuGrant?.invoke()
                    pendingShizukuGrant = null
                }
            }
        }
    }

    fun setShizukuTracking(enabled: Boolean) {
        if (!enabled) {
            pendingShizukuGrant = null
            scope.launch { appPreferenceRepo.setShizukuTracking(false) }
            return
        }
        if (shizukuPermission.value) {
            scope.launch { appPreferenceRepo.setShizukuTracking(true) }
        } else if (shizukuRunning.value) {
            pendingShizukuGrant = { scope.launch { appPreferenceRepo.setShizukuTracking(true) } }
            shizukuServicesProvider.shizukuRequestPermission()
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

    fun askOverlayPermission(activity: Activity?) {
        activity?.startActivity(
            Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
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

        overlayPermission.value = Settings.canDrawOverlays(context)

        shizukuRunning.value = shizukuServicesProvider.shizukuRunning()
        if (shizukuRunning.value) {
            shizukuPermission.value = shizukuServicesProvider.shizukuPermission() == PackageManager.PERMISSION_GRANTED
        } else {
            shizukuPermission.value = false
        }
    }
}