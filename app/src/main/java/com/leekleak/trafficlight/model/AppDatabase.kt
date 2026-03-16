package com.leekleak.trafficlight.model

import android.annotation.SuppressLint
import android.app.usage.NetworkStats.Bucket.UID_REMOVED
import android.app.usage.NetworkStats.Bucket.UID_TETHERING
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import com.leekleak.trafficlight.R
import timber.log.Timber


@SuppressLint("QueryPermissionsNeeded")
class AppDatabase(
    private val context: Context
) {
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
        }.distinctBy { it.uid }.map {
            App(
                uid = it.uid,
                packageName = it.packageName,
                label = it.loadLabel(packageManager).toString()
            )
        }
    }

    fun getNameForUID(uid: Int): String? {
        when (uid) {
            UID_TETHERING -> return context.getString(R.string.tethering)
            UID_REMOVED -> return  context.getString(R.string.removed_apps)
            else -> {
                val packageName = getPackageNamesForUID(uid) ?: return null
                for (name in packageName) {
                    try {
                        val info = packageManager.getPackageInfo(name, 0).applicationInfo ?: return ""
                        return packageManager.getApplicationLabel(info).toString()
                    } catch (_: Exception) { }
                }
                Timber.e("Failed to get name for UID $uid, packageName ${packageName.firstOrNull()}")
                return packageName.firstOrNull()
            }
        }
    }

    fun getDrawableResourceForUID(uid: Int): Int? {
        return when (uid) {
            UID_TETHERING -> R.drawable.hotspot
            UID_REMOVED -> R.drawable.deleted
            else -> null
        }
    }

    fun getPackageNamesForUID(uid: Int): List<String>? {
        return when (uid) {
            UID_TETHERING -> listOf("")
            UID_REMOVED -> listOf("")
            else -> try {
                packageManager.getPackagesForUid(uid)?.toList()
            } catch (e: Exception) {
                Timber.w(e, "Failed to get package names for UID $uid")
                null
            }
        }
    }

    companion object {
        val specialUIDs = listOf(UID_REMOVED, UID_TETHERING)
    }
}

data class App(
    val uid: Int,
    val packageName: String,
    val label: String,
    val drawableResource: Int? = null
)