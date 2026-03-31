package com.leekleak.trafficlight.model

import android.annotation.SuppressLint
import android.app.usage.NetworkStats.Bucket.UID_REMOVED
import android.app.usage.NetworkStats.Bucket.UID_TETHERING
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.compose.foundation.Image
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import coil3.ImageLoader
import coil3.asImage
import coil3.compose.rememberAsyncImagePainter
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

    val suspiciousApps by lazy {
        allApps.filter { app ->
            try {
                val pi = packageManager.getPackageInfo(app.packageName, PackageManager.GET_PERMISSIONS)
                (pi.requestedPermissions?.contains("android.permission.INTERNET") ?: true)
            } catch (e: Exception) {
                Timber.e(e, "${app.packageName}")
                false
            }
        }.distinctBy { it.uid }.map {
            App(
                uid = it.uid,
                packageName = it.packageName,
                label = it.loadLabel(packageManager).toString()
            )
        }.plus(listOf(removedApp, tetheringApp, allApp, unknownApp))
    }

    val suspiciousAppsFlow = flow {
        emit(suspiciousApps)
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

    fun getAppForUID(uid: Int): App {
        return suspiciousApps.find { it.uid == uid } ?: allApp
    }

    val unknownApp = App(
        uid = -99,
        packageName = "",
        label = context.getString(R.string.unknown),
        drawableResource = R.drawable.help
    )

    val tetheringApp = App(
        uid = UID_TETHERING,
        packageName = "",
        label = context.getString(R.string.tethering),
        drawableResource = R.drawable.hotspot
    )

    val removedApp = App(
        uid = UID_REMOVED,
        packageName = "",
        label = context.getString(R.string.removed_apps),
        drawableResource = R.drawable.deleted
    )

    companion object {
        val allApp = App(
            uid = -100,
            packageName = "",
            label = "All apps", //TODO: Unhardcode this
            drawableResource = R.drawable.apps
        )
        val specialUIDs = listOf(UID_REMOVED, UID_TETHERING)
    }
}

data class App(
    val uid: Int,
    val packageName: String,
    val label: String,
    val drawableResource: Int? = null
) {
    val uidQuery: Int?
        get() = if (uid == -100) null else uid
    @Composable
    fun GetIcon(modifier: Modifier = Modifier, tint: Color = LocalContentColor.current) {
        var icon = false
        val painter = drawableResource?.let { icon = true; painterResource(it) }
            ?: rememberAsyncImagePainter(AppIcon(packageName))
        if (icon) {
            Icon(
                modifier = modifier,
                painter = painter,
                contentDescription = label,
                tint = tint
            )
        } else {
            Image(
                modifier = modifier,
                painter = painter,
                contentDescription = label
            )
        }
    }
}

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