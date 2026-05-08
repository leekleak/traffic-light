package com.leekleak.trafficlight.model

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
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext

class AppManager(context: Context) {
    private val packageManager: PackageManager = context.packageManager
    private var allApps: List<DataUIDApp>? = null
    suspend fun getAllApps(): List<DataUIDApp> = withContext(Dispatchers.IO) {
        if (allApps != null) return@withContext allApps!!

        allApps = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            packageManager.getInstalledApplications(PackageManager.ApplicationInfoFlags.of(0L))
        } else {
            packageManager.getInstalledApplications(0)
        }.distinctBy { it.uid }.map {
            async {
                DataUIDApp(
                    uid = it.uid,
                    packageName = it.packageName,
                    label = it.loadLabel(packageManager).toString()
                )
            }
        }.awaitAll()
        return@withContext allApps!!
    }

    suspend fun getAppForUID(uid: Int): DataUID = getAllApps().plus(specialApps).find { it.uid == uid } ?: unknownApp

    companion object {
        val allApp = DataUIDSpecial(
            uid = UID_ALL,
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
            uid = UID_UNKNOWN,
            packageName = "",
            drawableResource = R.drawable.help,
            stringResource = R.string.unknown
        )
        val specialApps = listOf(allApp, tetheringApp, removedApp, unknownApp)
        val specialUIDs = listOf(UID_REMOVED, UID_TETHERING)
        const val UID_UNKNOWN = -99
        const val UID_ALL = -100
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
        val drawable = try {
            context.packageManager.getApplicationIcon(data.packageName)
        } catch (_: PackageManager.NameNotFoundException) {
            context.packageManager.defaultActivityIcon
        }

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