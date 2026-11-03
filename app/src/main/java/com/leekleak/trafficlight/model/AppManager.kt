package com.leekleak.trafficlight.model

import android.app.usage.NetworkStats.Bucket.UID_REMOVED
import android.app.usage.NetworkStats.Bucket.UID_TETHERING
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
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
import androidx.core.content.ContextCompat
import coil3.ImageLoader
import coil3.asImage
import coil3.compose.rememberAsyncImagePainter
import coil3.decode.DataSource
import coil3.fetch.FetchResult
import coil3.fetch.Fetcher
import coil3.fetch.ImageFetchResult
import coil3.request.Options
import com.leekleak.core.R
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import timber.log.Timber

class AppManager(context: Context) {
    private val appContext = context.applicationContext
    private val packageManager: PackageManager = appContext.packageManager

    @Volatile
    private var cachedApps: List<DataUIDApp>? = null
    private val cacheMutex = Mutex()
    private val cacheLock = Any()
    private var cacheGeneration = 0
    private val refreshScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    @Volatile
    private var refreshJob: Job? = null

    private val packageReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                Intent.ACTION_PACKAGE_ADDED,
                Intent.ACTION_PACKAGE_REMOVED,
                Intent.ACTION_PACKAGE_CHANGED,
                Intent.ACTION_PACKAGE_REPLACED,
                Intent.ACTION_LOCALE_CHANGED -> invalidateCache()
            }
        }
    }

    init {
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_PACKAGE_ADDED)
            addAction(Intent.ACTION_PACKAGE_REMOVED)
            addAction(Intent.ACTION_PACKAGE_CHANGED)
            addAction(Intent.ACTION_PACKAGE_REPLACED)
            addAction(Intent.ACTION_LOCALE_CHANGED)
            addDataScheme("package")
        }
        runCatching {
            ContextCompat.registerReceiver(
                appContext,
                packageReceiver,
                filter,
                ContextCompat.RECEIVER_NOT_EXPORTED,
            )
        }.onFailure {
            Timber.e(it, "Failed to register package change receiver")
        }
    }

    suspend fun getAllApps(): List<DataUIDApp> {
        cachedApps?.let { return it }
        return cacheMutex.withLock {
            cachedApps?.let { return@withLock it }

            val generation = synchronized(cacheLock) { cacheGeneration }
            val apps = loadApps()
            synchronized(cacheLock) {
                // Apparently it's possible to race if the receiver fires during app loading.
                if (generation == cacheGeneration) {
                    cachedApps = apps
                }
            }
            apps
        }
    }

    private fun invalidateCache() {
        synchronized(cacheLock) {
            cacheGeneration++
            cachedApps = null
        }

        // Refill the cache immediately so the next lookup doesn't pay the load cost.
        refreshJob?.cancel()
        refreshJob = refreshScope.launch { refreshCache() }
    }

    private suspend fun refreshCache() {
        cacheMutex.withLock {
            if (cachedApps != null) return@withLock

            val generation = synchronized(cacheLock) { cacheGeneration }
            val apps = try {
                loadApps()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Timber.e(e, "Failed to refresh app list")
                return@withLock
            }

            synchronized(cacheLock) {
                if (generation == cacheGeneration) {
                    cachedApps = apps
                }
            }
        }
    }

    private suspend fun loadApps(): List<DataUIDApp> = withContext(Dispatchers.IO) {
        val installed = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            packageManager.getInstalledApplications(PackageManager.ApplicationInfoFlags.of(0L))
        } else {
            packageManager.getInstalledApplications(0)
        }

        installed.distinctBy { it.uid }.map { appInfo ->
            async {
                DataUIDApp(
                    uid = appInfo.uid,
                    packageName = appInfo.packageName,
                    label = appInfo.loadLabel(packageManager).toString()
                )
            }
        }.awaitAll()
    }

    suspend fun getAppForUID(uid: Int): DataUID {
        return getAllApps().plus(specialApps).find { it.uid == uid } ?: DataUIDService(uid = uid)
    }

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
        val otherUsersApp = DataUIDSpecial(
            uid = UID_OTHER_USERS,
            packageName = "",
            drawableResource = R.drawable.group,
            stringResource = R.string.other_users
        )
        val unknownApp = DataUIDSpecial(
            uid = UID_UNKNOWN,
            packageName = "",
            drawableResource = R.drawable.help,
            stringResource = R.string.unknown
        )
        val specialApps = listOf(allApp, tetheringApp, removedApp, otherUsersApp, unknownApp)
        val specialUIDs = listOf(UID_REMOVED, UID_TETHERING, UID_OTHER_USERS)
        const val UID_OTHER_USERS = -98
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
        Icon(
            modifier = modifier,
            painter = painterResource(drawableResource),
            contentDescription = getName(LocalContext.current),
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

class DataUIDService(uid: Int) : DataUID(uid, "") {
    override val uidQuery: Int? = if (uid == -100) null else uid
    override fun getName(context: Context): String = context.getString(R.string.system_service, uid)

    @Composable
    override fun GetIcon(
        modifier: Modifier,
        tint: Color
    ) {
        Icon(
            modifier = modifier,
            painter = painterResource(R.drawable.manufacturing),
            contentDescription = getName(LocalContext.current),
            tint = tint
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

fun List<DataUID>.search(query: String, context: Context): List<DataUID> {
    return this.sortedByDescending {
            if (it is DataUIDSpecial) 100
            else priorityApps.indexOf(it.packageName)
        }
        .filter {
            it.getName(context).lowercase().contains(query.lowercase()) ||
            it.packageName.lowercase().contains(query.lowercase())
        }
}

/**
 * Apps most often included as zero-rated.
 *
 * The further down the list the app, the higher it will be placed when sorted.
 */
private val priorityApps = listOf(
    "com.amazon.avod.thirdpartyclient", // Prime Video
    "org.telegram.messenger",
    "com.microsoft.teams",
    "us.zoom.videomeetings",
    "com.waze",
    "com.google.android.apps.maps",
    "com.apple.android.music",
    "com.netflix.mediaclient",
    "com.ss.android.ugc.trill", // TikTok
    "com.google.android.youtube",
    "com.spotify.music",
    "com.snapchat.android",
    "com.twitter.android",
    "com.instagram.android",
    "com.facebook.orca", // Facebook Messenger
    "com.facebook.katana", // Facebook
    "com.whatsapp",
)