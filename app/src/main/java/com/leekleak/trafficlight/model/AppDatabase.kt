package com.leekleak.trafficlight.model

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.os.Build
import org.koin.core.component.KoinComponent
import kotlin.collections.contains


class AppDatabase(context: Context): KoinComponent {
    private var packageManager: PackageManager = context.packageManager
    val allApps by lazy {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            packageManager.getInstalledApplications(PackageManager.ApplicationInfoFlags.of(0L))
        } else {
            packageManager.getInstalledApplications(0)
        }
    }

    val suspiciousApps by lazy {
        allApps.filter { app ->
            val pi = packageManager.getPackageInfo(app.packageName, PackageManager.GET_PERMISSIONS)
            (pi.requestedPermissions?.contains("android.permission.INTERNET") ?: true)
        }
    }

    fun getLabel(app: ApplicationInfo): String = app.loadLabel(packageManager).toString()
}