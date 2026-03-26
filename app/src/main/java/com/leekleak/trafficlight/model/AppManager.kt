package com.leekleak.trafficlight.model

import android.annotation.SuppressLint
import android.app.usage.NetworkStats.Bucket.UID_REMOVED
import android.app.usage.NetworkStats.Bucket.UID_TETHERING
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import coil3.ImageLoader
import coil3.asImage
import coil3.decode.DataSource
import coil3.fetch.FetchResult
import coil3.fetch.Fetcher
import coil3.fetch.ImageFetchResult
import coil3.request.Options
import com.leekleak.trafficlight.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import timber.log.Timber

@SuppressLint("QueryPermissionsNeeded")
class AppManager(
    private val context: Context
) {
    private val packageManager: PackageManager = context.packageManager
    val allApps by lazy {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            packageManager.getInstalledApplications(PackageManager.ApplicationInfoFlags.of(0L))
        } else {
            packageManager.getInstalledApplications(0)
        }
    }

    val suspiciousApps = flow {
        emit(
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
        )
    }.flowOn(Dispatchers.IO)

    fun getNameForUID(uid: Int?): String? {
        when (uid) {
            UID_TETHERING -> return context.getString(R.string.tethering)
            UID_REMOVED -> return  context.getString(R.string.removed_apps)
            null -> return context.getString(R.string.all_apps)
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

data class AppIcon(val packageName: String)

class AppIconFetcher(
    private val data: AppIcon,
    private val context: Context
) : Fetcher {
    override suspend fun fetch(): FetchResult {
        val drawable = context.packageManager.getApplicationIcon(data.packageName)

        return ImageFetchResult(
            image = drawable.asImage(),
            isSampled = false,
            dataSource = DataSource.DISK
        )
    }

    class Factory(private val context: Context) : Fetcher.Factory<AppIcon> {
        override fun create(data: AppIcon, options: Options, imageLoader: ImageLoader): Fetcher {
            return AppIconFetcher(data, context)
        }
    }
}