package com.leekleak.trafficlight.services.notifications

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import androidx.collection.LruCache
import androidx.compose.ui.unit.Density
import androidx.core.app.NotificationCompat
import androidx.core.graphics.createBitmap
import androidx.core.graphics.drawable.IconCompat
import androidx.lifecycle.LifecycleService
import com.leekleak.trafficlight.MainActivity
import com.leekleak.trafficlight.R
import com.leekleak.trafficlight.database.AppPreferenceRepo
import com.leekleak.trafficlight.database.DataDirection
import com.leekleak.trafficlight.database.DataType
import com.leekleak.trafficlight.database.DayUsage
import com.leekleak.trafficlight.database.TrafficSnapshot
import com.leekleak.trafficlight.database.UsageQuery
import com.leekleak.trafficlight.model.NetworkUsageManager
import com.leekleak.trafficlight.ui.theme.notificationIconFont
import com.leekleak.trafficlight.util.DataSize
import com.leekleak.trafficlight.util.clipAndPad
import com.leekleak.trafficlight.util.convertFontFamilyToTypeface
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.time.LocalDate

class SpeedNotification(
    serviceScope: CoroutineScope,
    private val context: Context,
    private val notificationId: Int,
    private val networkUsageManager: NetworkUsageManager,
    private val notificationManager: NotificationManager,
    private val connectivityManager: ConnectivityManager,
    private val appPreferenceRepo: AppPreferenceRepo,
    private val trafficSnapshot: TrafficSnapshot,
) : PersistentNotification {
    private val scope = CoroutineScope(serviceScope.coroutineContext + SupervisorJob(serviceScope.coroutineContext[Job]))
    private var job: Job? = null
    private lateinit var notificationBuilder: NotificationCompat.Builder
    private lateinit var notification: Notification
    private var updateCounter = DATA_UPDATE_FREQ

    private val queryMobile =
        UsageQuery(
            dataType = DataType.Mobile,
            dataDirection = DataDirection.Bidirectional,
        )
    private val queryWifi =
        UsageQuery(
            dataType = DataType.Wifi,
            dataDirection = DataDirection.Bidirectional,
        )

    private var bigIcon = false
    private var aodMode = false
    private var inBits = false
    private var liveNotification = false
    private var todayUsage = DayUsage()

    init {
        scope.launch {
            appPreferenceRepo.modeAOD.collect { aodMode = it }
        }
        scope.launch {
            appPreferenceRepo.bigIcon.collect { bigIcon = it }
        }
        scope.launch {
            appPreferenceRepo.speedBits.collect { inBits = it }
        }
        scope.launch {
            appPreferenceRepo.liveNotification.collect {
                liveNotification = it
            }
        }
        updateBaseNotification()
        notification.flags = Notification.FLAG_ONGOING_EVENT or Notification.FLAG_NO_CLEAR
        notificationManager.notify(notificationId, notification)
    }

    override fun start() {
        if (job?.isActive == true) return
        job = scope.launch {
            trafficSnapshot.updateSnapshot()
            trafficSnapshot.setCurrentAsLast()
            while (true) {
                Timber.i("Updating notification")
                trafficSnapshot.updateSnapshot()

                /**
                 * If network speed is changing rapidly, we use this while loop to self-calibrate
                 * the refresh timing to match the timing of the TrafficStats API updates.
                 *
                 * If network speed is not changing rapidly (i.e. it's zero)
                 * it's quite likely that the next tick will also be zero, so we ignore that and
                 * simply sleep for 1 second
                 */
                if (trafficSnapshot.isCurrentSameAsLast()) {
                    delay(100)
                    trafficSnapshot.updateSnapshot()
                }

                if (updateCounter == DATA_UPDATE_FREQ) {
                    updateTodayUsage()
                    updateCounter = 0
                } else {
                    updateCounter++
                }

                updateNotification(trafficSnapshot)
                trafficSnapshot.setCurrentAsLast()
                delay(900)
            }
        }

    }

    override fun cancel() {
        scope.cancel()
        notificationManager.cancel(notificationId)
    }

    override fun startForeground(service: LifecycleService) {
        service.startForeground(
            notificationId,
            notification,
        )
    }

    override fun screenStateChange(on: Boolean) {
        if (on) start()
        else if (!aodMode) job?.cancel()
    }

    override fun getId(): Int = notificationId

    private var lastTitle: String = ""
    private suspend fun updateNotification(trafficSnapshot: TrafficSnapshot) {
        val speed = DataSize(trafficSnapshot.totalSpeed).toString(speed = true, inBits = inBits)
        val title = context.getString(R.string.speed, speed)

        if (lastTitle == speed) return // If the title is the same, so is the icon.
        else lastTitle = speed

        val spacing = 18
        val messageShort =
            context.getString(R.string.wi_fi, DataSize(todayUsage.usage2).toString()).clipAndPad(spacing) +
            context.getString(R.string.mobile, DataSize(todayUsage.usage1).toString())

        updateBaseNotification()
        notification = notificationBuilder
            .apply {
                if (!liveNotification) {
                    setSmallIcon(createIcon(trafficSnapshot))
                    setWhen(Long.MAX_VALUE) // Keep above other notifications
                    setShowWhen(false) // Hide timestamp
                }
                else  {
                    setSmallIcon(R.drawable.mobiledata_arrows)
                    setShortCriticalText(speed)
                }
            }
            .setContentTitle(title)
            .setContentText(messageShort)
            .build()
        notification.flags = Notification.FLAG_ONGOING_EVENT or Notification.FLAG_NO_CLEAR
        notificationManager.notify(notificationId, notification)
    }

    private val paint by lazy {
        Paint().apply {
            color = context.getColor(R.color.white)
            typeface = convertFontFamilyToTypeface(context, notificationIconFont())
            textAlign = Paint.Align.CENTER
        }
    }

    private var cachedIcons = LruCache<String, IconCompat>(50)
    private var bitmap: Bitmap? = null
    private val bitmapMutex = Mutex()
    private suspend fun createIcon(snapshot: TrafficSnapshot): IconCompat = withContext(Dispatchers.Default) {
        val density = Density(context)
        val multiplier = 24 * density.density / 96f * if (bigIcon) 2f else 1f
        val height = (96 * multiplier).toInt()

        val data = DataSize(snapshot.totalSpeed).toString(speed = true, inBits = inBits)
        val speed = data.substringBefore(" ")
        val unit = data.substringAfter(" ")

        val iconTag = "$speed$unit$height"

        cachedIcons[iconTag]?.let { return@withContext it }

        bitmapMutex.withLock {
            if (bitmap == null || bitmap!!.height != height) {
                bitmap = createBitmap(height, height, Bitmap.Config.ALPHA_8)
            } else {
                bitmap?.eraseColor(Color.TRANSPARENT)
            }

            val canvas = Canvas(bitmap!!)

            paint.apply {
                textSize = 72f * multiplier
                letterSpacing = -0.05f * multiplier
            }
            canvas.drawText(speed, 48f * multiplier, 54f * multiplier, paint)

            paint.apply {
                textSize = 46f * multiplier
                letterSpacing = 0f * multiplier
                typeface = convertFontFamilyToTypeface(context, notificationIconFont())
            }
            canvas.drawText(unit, 48f * multiplier, 94f * multiplier, paint)

            /**
             * Don't cache numbers with many digits as they appear much more often and are unlikely
             * to be worth the cost of creating a new bitmap
             *
             * Mostly there to avoid re-rendering common values like 0KB/s, <1KB/s or other small values
             * caused by many background processes.
             *
             * Making caching more aggressive is probably a bad idea as duplicating bitmaps is quite
             * expensive and not worth it if the value appears once a day.
             */
            if (speed.count(Char::isDigit) == 1) {
                cachedIcons.put(
                    iconTag,
                    IconCompat.createWithBitmap(bitmap!!.copy(Bitmap.Config.ALPHA_8, false)),
                )
                return@withContext cachedIcons[iconTag]!!
            } else {
                return@withContext IconCompat.createWithBitmap(bitmap!!)
            }
        }
    }

    private suspend fun updateTodayUsage() {
        val date = LocalDate.now()
        val mobile = networkUsageManager.totalDayUsage(queryMobile, date)
        val wifi = networkUsageManager.totalDayUsage(queryWifi, date)
        todayUsage = DayUsage(date, mobile, wifi)
    }

    private fun updateBaseNotification() {
        val networkAvailable = isNetworkAvailable()
        val channel = if (networkAvailable) NOTIFICATION_CHANNEL_ID else NOTIFICATION_CHANNEL_ID_SILENT
        notificationBuilder = NotificationCompat.Builder(context, channel)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(context.getString(R.string.app_name_short))
            .setOngoing(true)
            .setRequestPromotedOngoing(liveNotification)
            .setSilent(true)
            .setLocalOnly(true)
            .setOnlyAlertOnce(true)
            .setContentIntent(
                PendingIntent.getActivity(
                    context, 0, Intent(context, MainActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
                    }, PendingIntent.FLAG_IMMUTABLE
                )
            )
        notification = notificationBuilder.build()
    }

    private fun isNetworkAvailable(): Boolean {
        val capabilities = connectivityManager.getNetworkCapabilities(connectivityManager.activeNetwork)
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            capabilities?.run {
                hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                        hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
                        hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)
            } ?: false
        } else {
            connectivityManager.activeNetworkInfo?.run {
                when (type) {
                    ConnectivityManager.TYPE_WIFI -> true
                    ConnectivityManager.TYPE_MOBILE -> true
                    ConnectivityManager.TYPE_ETHERNET -> true
                    else -> false
                }
            } ?: false
        }
    }

    companion object {
        private const val DATA_UPDATE_FREQ = 4
        const val NOTIFICATION_CHANNEL_ID = "PersistentNotification"
        const val NOTIFICATION_CHANNEL_ID_SILENT = "PersistentNotification (Silent)"
    }
}