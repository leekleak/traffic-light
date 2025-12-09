package com.leekleak.trafficlight.services

import android.Manifest.permission.POST_NOTIFICATIONS
import android.app.AppOpsManager
import android.content.Context
import android.content.Context.POWER_SERVICE
import android.content.pm.PackageManager
import android.os.Build
import android.os.PowerManager
import android.os.Process.myUid
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine

class PermissionManager(
    private val context: Context
) {

    fun hasAllPermissions(): Boolean {
        return  backgroundPermission &&
                usagePermission &&
                notificationPermission
    }

    private val _backgroundPermission = MutableStateFlow(false)
    val backgroundPermission: Boolean get() = _backgroundPermission.value
    val backgroundPermissionFlow = _backgroundPermission.asStateFlow()

    private val _usagePermission = MutableStateFlow(false)
    val usagePermission: Boolean get() = _usagePermission.value
    val usagePermissionFlow = _usagePermission.asStateFlow()

    private val _notificationPermission = MutableStateFlow(false)
    val notificationPermission: Boolean get() = _notificationPermission.value
    val notificationPermissionFlow = _notificationPermission.asStateFlow()


    val hasAllPermissions: Flow<Boolean> = combine(
        backgroundPermissionFlow,
        usagePermissionFlow,
        notificationPermissionFlow
    ) { background, usage, notification ->
        background && usage && notification
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
    }
}