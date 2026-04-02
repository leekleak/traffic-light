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
import androidx.compose.ui.platform.LocalContext
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
class AppManager(context: Context) {
    private val packageManager: PackageManager = context.packageManager
    val allApps =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            packageManager.getInstalledApplications(PackageManager.ApplicationInfoFlags.of(0L))
        } else {
            packageManager.getInstalledApplications(0)
        }

    val suspiciousApps =
        allApps.filter { app ->
            try {
                val pi = packageManager.getPackageInfo(app.packageName, PackageManager.GET_PERMISSIONS)
                (pi.requestedPermissions?.contains("android.permission.INTERNET") ?: true)
            } catch (e: Exception) {
                Timber.e(e, "${app.packageName}")
                false
            }
        }.distinctBy { it.uid }.map {
            DataUIDApp(
                uid = it.uid,
                packageName = it.packageName,
                label = it.loadLabel(packageManager).toString()
            )
        }

    val suspiciousAppsFlow = flow {
        emit(suspiciousApps)
    }.flowOn(Dispatchers.IO)

    fun getNameForUID(uid: Int): String? {
        val packageName = getPackageNamesForUID(uid) ?: return null
        for (name in packageName) {
            try {
                val info = packageManager.getPackageInfo(name, 0).applicationInfo ?: return null
                return packageManager.getApplicationLabel(info).toString()
            } catch (_: Exception) { }
        }
        Timber.e("Failed to get name for UID $uid, packageName ${packageName.firstOrNull()}")
        return packageName.firstOrNull()
    }

    fun getPackageNamesForUID(uid: Int): List<String>? {
        return try {
            packageManager.getPackagesForUid(uid)?.toList()
        } catch (e: Exception) {
            Timber.w(e, "Failed to get package names for UID $uid")
            null
        }
    }

    fun getAppForUID(uid: Int): DataUID {
        return suspiciousApps.plus(specialApps).find { it.uid == uid } ?: allApp
    }

    companion object {
        val allApp = DataUIDSpecial(
            uid = -100,
            packageName = "",
            drawableResource = R.drawable.apps,
            stringResource = R.string.all_apps
        )
        val tetheringApp = DataUIDSpecial(
            uid = UID_TETHERING,
            packageName = "",
            drawableResource = R.drawable.hotspot,
            stringResource = R.string.tethering
        )
        val removedApp = DataUIDSpecial(
            uid = UID_REMOVED,
            packageName = "",
            drawableResource = R.drawable.deleted,
            stringResource = R.string.removed_apps
        )
        val unknownApp = DataUIDSpecial(
            uid = -99,
            packageName = "",
            drawableResource = R.drawable.help,
            stringResource = R.string.unknown
        )
        val specialApps = listOf(allApp, tetheringApp, removedApp, unknownApp)
        val specialUIDs = listOf(UID_REMOVED, UID_TETHERING)
    }
}

class DataUIDSpecial(
    uid: Int,
    packageName: String,
    private val drawableResource: Int,
    private val stringResource: Int
) : DataUID(uid, packageName) {
    override val uidQuery: Int? = if (uid == -100) null else uid
    override fun getName(context: Context): String = context.getString(stringResource)

    @Composable
    override fun GetIcon(
        modifier: Modifier,
        tint: Color
    ) {
        val context = LocalContext.current
        Icon(
            modifier = modifier,
            painter = painterResource(drawableResource),
            contentDescription = getName(context),
            tint = tint
        )
    }
}

class DataUIDApp(
    uid: Int,
    packageName: String,
    private val label: String,
) : DataUID(uid, packageName) {
    override val uidQuery: Int = uid
    override fun getName(context: Context): String = label
    @Composable
    override fun GetIcon(modifier: Modifier, tint: Color) {
        Image(
            modifier = modifier,
            painter = rememberAsyncImagePainter(AppIcon(packageName)),
            contentDescription = label
        )
    }
}

abstract class DataUID(
    val uid: Int,
    val packageName: String
) {
    abstract val uidQuery: Int?
    abstract fun getName(context: Context): String
    @Composable
    abstract fun GetIcon(modifier: Modifier = Modifier, tint: Color = LocalContentColor.current)
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