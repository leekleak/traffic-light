package com.leekleak.trafficlight.services

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.os.IBinder
import androidx.collection.LruCache
import androidx.compose.ui.unit.Density
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import androidx.core.graphics.createBitmap
import androidx.core.graphics.drawable.IconCompat
import com.leekleak.trafficlight.MainActivity
import com.leekleak.trafficlight.R
import com.leekleak.trafficlight.database.DataDirection
import com.leekleak.trafficlight.database.DayUsage
import com.leekleak.trafficlight.database.Mobile
import com.leekleak.trafficlight.database.PreferenceRepo
import com.leekleak.trafficlight.database.TrafficSnapshot
import com.leekleak.trafficlight.database.UsageQuery
import com.leekleak.trafficlight.database.Wifi
import com.leekleak.trafficlight.model.NetworkUsageManager
import com.leekleak.trafficlight.util.SizeFormatter
import com.leekleak.trafficlight.util.clipAndPad
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.koin.core.component.KoinComponent
import org.koin.core.component.get
import org.koin.core.component.inject
import timber.log.Timber
import java.lang.ref.WeakReference
import java.time.LocalDate


class UsageService : Service() {
    private val serviceScope = CoroutineScope(Dispatchers.IO)
    private var job: Job? = null
    private val networkUsageManager: NetworkUsageManager by lazy { get() }
    private val preferenceRepo: PreferenceRepo by lazy { get() }
    private val notificationManager: NotificationManager by lazy { get() }
    private val connectivityManager: ConnectivityManager by lazy { get() }
    private lateinit var notificationBuilder: NotificationCompat.Builder
    private lateinit var notification: Notification

    private val queryMobile =
        UsageQuery(
            dataType = listOf(Mobile),
            dataDirection = DataDirection.Bidirectional,
        )
    private val queryWifi =
        UsageQuery(
            dataType = listOf(Wifi),
            dataDirection = DataDirection.Bidirectional,
        )

    private val screenStateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                Intent.ACTION_SCREEN_ON -> {
                    if (job == null) startJob()
                }
                Intent.ACTION_SCREEN_OFF -> {
                    if (!aodMode) {
                        job?.cancel()
                        job = null
                    }
                }
            }
        }
    }

    private var bigIcon = false
    private var aodMode = false
    private val formatter by lazy { SizeFormatter() }

    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    override fun onCreate() {
        super.onCreate()
        instance = WeakReference(this)
        Timber.i("Creating UsageService")

        updateBaseNotification()

        registerReceiver(screenStateReceiver, IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_ON)
            addAction(Intent.ACTION_SCREEN_OFF)
        })
        serviceScope.launch {
            preferenceRepo.modeAOD.collect { aodMode = it }
        }
        serviceScope.launch {
            preferenceRepo.bigIcon.collect { bigIcon = it }
        }
        serviceScope.launch {
            preferenceRepo.speedBits.collect { formatter.asBits = it }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(screenStateReceiver)
        notificationManager.cancel(NOTIFICATION_ID)
        job?.cancel()
        serviceScope.cancel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (job == null) {
            Timber.i("Starting foreground service")
            updateTodayUsage()
            try {
                notificationManager.notify(NOTIFICATION_ID, notification)
                notification.let {
                    ServiceCompat.startForeground(
                        this,
                        NOTIFICATION_ID,
                        it,
                        ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
                    )
                }
                startJob()
            } catch (e: Exception) {
                Timber.e("Failed to start foreground service: $e")
            }
        }
        return START_STICKY
    }

    private fun startJob() {
        job = serviceScope.launch {
            val trafficSnapshot = TrafficSnapshot(serviceScope)
            trafficSnapshot.updateSnapshot()
            trafficSnapshot.setCurrentAsLast()

            var updateCounter = 0
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

    private fun updateTodayUsage() {
        val date = LocalDate.now()
        val mobile = networkUsageManager.calculateDayUsageBasic(date, date, queryMobile)
        val wifi = networkUsageManager.calculateDayUsageBasic(date, date, queryWifi)
        todayUsage = DayUsage(date, mobile, wifi)
    }

    private var lastTitle: String = ""
    private suspend fun updateNotification(trafficSnapshot: TrafficSnapshot) {
        val title = getString(R.string.speed, formatter.format(trafficSnapshot.totalSpeed, 2, true))

        if (lastTitle == title) return // If the title is the same, so is the icon.
        else lastTitle = title

        val spacing = 18
        val messageShort =
            getString(R.string.wi_fi, formatter.format(todayUsage.usage1, 2)).clipAndPad(spacing) +
            getString(R.string.mobile, formatter.format(todayUsage.usage2, 2))

        updateBaseNotification()
        notification = notificationBuilder
            .setSmallIcon(createIcon(trafficSnapshot))
            .setContentTitle(title)
            .setContentText(messageShort)
            .build()
        notification.flags = Notification.FLAG_ONGOING_EVENT or Notification.FLAG_NO_CLEAR
        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    private val paint by lazy {
        Paint().apply {
            color = ContextCompat.getColor(this@UsageService, R.color.white)
            typeface = resources.getFont(R.font.roboto_condensed_semi_bold)
            textAlign = Paint.Align.CENTER
        }
    }

    private var cachedIcons = LruCache<String, IconCompat>(50)
    private var bitmap: Bitmap? = null
    private val bitmapMutex = Mutex()
    suspend fun createIcon(snapshot: TrafficSnapshot): IconCompat = withContext(Dispatchers.Default) {
        val density = Density(this@UsageService)
        val multiplier = 24 * density.density / 96f * if (bigIcon) 2f else 1f
        val height = (96 * multiplier).toInt()

        val data = formatter.partFormat(snapshot.totalSpeed, true)
        val bytesPerSecond: Boolean = data[2].lowercase() == "b/s"
        val speed = if (!bytesPerSecond || snapshot.totalSpeed == 0L) {
            data[0] + if (data[0].length == 1 && data[1].isNotEmpty()) "." + data[1] else ""
        } else "<1"
        val unit = if (!bytesPerSecond) data[2] else "K${data[2]}"

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
            canvas.drawText(speed, 48f * multiplier, 56f * multiplier, paint)

            paint.apply {
                textSize = 46f * multiplier
                letterSpacing = 0f * multiplier
            }
            canvas.drawText(unit, 48f * multiplier, 96f * multiplier, paint)

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

    private fun updateBaseNotification() {
        val networkAvailable = isNetworkAvailable()
        val channel = if (networkAvailable) NOTIFICATION_CHANNEL_ID else NOTIFICATION_CHANNEL_ID_SILENT
        notificationBuilder = NotificationCompat.Builder(this, channel)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("Traffic Light")
            .setOngoing(true)
            .setSilent(true)
            .setLocalOnly(true)
            .setOnlyAlertOnce(true)
            .setWhen(Long.MAX_VALUE) // Keep above other notifications
            .setShowWhen(false) // Hide timestamp
            .setContentIntent(
                PendingIntent.getActivity(
                    this, 0, Intent(this, MainActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
                    }, PendingIntent.FLAG_IMMUTABLE
                )
            )
        notification = notificationBuilder.build()
    }

    companion object : KoinComponent {
        const val NOTIFICATION_ID = 228
        const val NOTIFICATION_CHANNEL_ID = "PersistentNotification"
        const val NOTIFICATION_CHANNEL_ID_SILENT = "PersistentNotification (Silent)"
        const val DATA_UPDATE_FREQ = 4

        private val _todayUsageFlow = MutableStateFlow(DayUsage())
        var todayUsage: DayUsage
            get() = _todayUsageFlow.value
            set(value) {
                _todayUsageFlow.value = value
            }

        private var instance: WeakReference<UsageService?> = WeakReference(null)

        fun isInstanceCreated(): Boolean {
            return instance.get() != null
        }

        fun startService(context: Context) {
            val preferenceRepo: PreferenceRepo by inject()
            val enabled = runBlocking { preferenceRepo.notification.first() }
            if (!isInstanceCreated() && enabled) {
                val intent = Intent(context, UsageService::class.java)
                context.startService(intent)
                Timber.i("Started service")
            }
        }

        fun stopService() {
            instance.get()?.stopSelf()
            instance.clear()
        }
    }
}
