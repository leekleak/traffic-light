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
import android.graphics.Color
import android.graphics.Paint
import android.os.IBinder
import androidx.collection.LruCache
import androidx.compose.ui.graphics.NativeCanvas
import androidx.compose.ui.unit.Density
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import androidx.core.graphics.createBitmap
import androidx.core.graphics.drawable.IconCompat
import com.leekleak.trafficlight.MainActivity
import com.leekleak.trafficlight.R
import com.leekleak.trafficlight.database.DayUsage
import com.leekleak.trafficlight.database.HourlyUsageRepo
import com.leekleak.trafficlight.database.TrafficSnapshot
import com.leekleak.trafficlight.database.UsageMode
import com.leekleak.trafficlight.model.PreferenceRepo
import com.leekleak.trafficlight.util.SizeFormatter
import com.leekleak.trafficlight.util.clipAndPad
import com.leekleak.trafficlight.util.currentTimezone
import com.leekleak.trafficlight.util.toTimestamp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import timber.log.Timber
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import kotlin.apply

class UsageService : Service(), KoinComponent {
    private val serviceScope = CoroutineScope(Dispatchers.IO)
    private var job: Job? = null

    private val hourlyUsageRepo: HourlyUsageRepo by inject()
    private val preferenceRepo: PreferenceRepo by inject()
    private val notificationManager: NotificationManager by lazy { getSystemService(NOTIFICATION_SERVICE) as NotificationManager }
    private var notification: Notification? = null
    private val notificationBuilder by lazy {
        NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("Traffic Light")
            .setOngoing(true)
            .setSilent(true)
            .setLocalOnly(true)
            .setOnlyAlertOnce(true)
            .setWhen(Long.MAX_VALUE) // Keep above other notifications
            .setShowWhen(false) // Hide timestamp
    }

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
    private var limitedMode = true // For cases where the NetworkStatsManager is broken
    private val formatter by lazy { SizeFormatter() }

    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
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
        serviceScope.launch {
            hourlyUsageRepo.usageModeFlow().collect { limitedMode = it != UsageMode.Unlimited }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(screenStateReceiver)
        notificationManager.cancel(NOTIFICATION_ID)
        job?.cancel()
        serviceScope.cancel()
    }

    private fun onDismissedIntent(context: Context): PendingIntent? {
        val intent = Intent(context, DismissListener::class.java)
        intent.putExtra("com.leekleak.trafficlight.notificationId", NOTIFICATION_ID)

        val pendingIntent =
            PendingIntent.getBroadcast(
                context.applicationContext,
                NOTIFICATION_ID,
                intent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
        return pendingIntent
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (job == null) {
            startJob()

            if (!limitedMode) {
                todayUsage = hourlyUsageRepo.calculateDayUsage(LocalDate.now())
            }
            notificationBuilder
                .setContentIntent(
                    PendingIntent.getActivity(
                        this, 0, Intent(this, MainActivity::class.java).apply {
                            Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
                        }, PendingIntent.FLAG_IMMUTABLE
                    )
                )
                .setDeleteIntent(
                    onDismissedIntent(this)
                )

            try {
                notification?.let {
                    ServiceCompat.startForeground(
                        this,
                        NOTIFICATION_ID,
                        it,
                        ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
                    )
                }
            } catch (_: Exception) {
                Timber.e("Failed to start foreground service")
            }
        }
        return START_STICKY
    }

    private fun startJob() {
        job = serviceScope.launch {
            val trafficSnapshot = TrafficSnapshot()
            trafficSnapshot.updateSnapshot()
            trafficSnapshot.setCurrentAsLast()

            var updateCounter = 0
            while (true) {
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

                if (!limitedMode) {
                    if (updateCounter == DATA_UPDATE_FREQ) {
                        updateDatabase()
                        updateCounter = 0
                    } else {
                        //interpolateDatabase(trafficSnapshot)
                        updateCounter++
                    }
                }

                updateNotification(trafficSnapshot)
                trafficSnapshot.setCurrentAsLast()

                delay(900)
            }
        }
    }

    private fun updateDatabase() {
        if (todayUsage.date != LocalDate.now()) {
            todayUsage = hourlyUsageRepo.calculateDayUsage(LocalDate.now())
        } else {
            val time = LocalDateTime.now()

            val stampNow = time.toTimestamp()
            var hour = time.truncatedTo(ChronoUnit.HOURS)
            if (hour.hour % 2 == 1) hour = hour.minusHours(1)
            val stampHourStart = hour.toTimestamp()

            val newHour = (stampNow - stampHourStart) < (DATA_UPDATE_FREQ * 1000)

            if (newHour) updateTodayUsage(stampHourStart - 3_600_000, stampHourStart)
            updateTodayUsage(stampHourStart, stampNow)
        }
    }

    private fun updateTodayUsage(stamp: Long, stampNow: Long) {
        if (todayUsage.hours.containsKey(stamp)) {
            todayUsage = todayUsage.copy(
                hours = todayUsage.hours.apply {
                    this[stamp] = hourlyUsageRepo.calculateHourData(stamp, stampNow)
                }
            ).also { it.categorizeUsage() }
        } else {
            todayUsage = hourlyUsageRepo.calculateDayUsage(LocalDate.now())
        }
    }

    private fun interpolateDatabase(trafficSnapshot: TrafficSnapshot) {
        var hour = LocalDateTime.now().truncatedTo(ChronoUnit.HOURS).atOffset(currentTimezone())
        if (hour.hour % 2 == 1) hour = hour.minusHours(1L)
        val stamp = hour.nano / 1_000_000L

        todayUsage.hours[stamp]?.add(trafficSnapshot.speedToHourData()) ?: run {
            todayUsage.hours[stamp] = trafficSnapshot.speedToHourData()
        }
        todayUsage.categorizeUsage()
    }

    private var lastTitle: String = ""
    private suspend fun updateNotification(trafficSnapshot: TrafficSnapshot) {
        val title = getString(R.string.speed, formatter.format(trafficSnapshot.totalSpeed, 2, true))

        if (lastTitle == title) return // If the title is the same, so is the icon.
        else lastTitle = title

        val spacing = 18
        val messageShort =
            getString(R.string.wi_fi, formatter.format(todayUsage.totalWifi, 2)).clipAndPad(spacing) +
            getString(R.string.mobile, formatter.format(todayUsage.totalCellular, 2))

        notification = notificationBuilder
            .setSmallIcon(createIcon(trafficSnapshot))
            .setContentTitle(title)
            .run { if (!limitedMode) this.setContentText(messageShort) else this }
            .build()
        notification?.flags = Notification.FLAG_ONGOING_EVENT or Notification.FLAG_NO_CLEAR
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

        if (bitmap == null || bitmap!!.height != height) {
            bitmap = createBitmap(height, height, Bitmap.Config.ALPHA_8)
        } else {
            bitmap?.eraseColor(Color.TRANSPARENT)
        }

        val canvas = NativeCanvas(bitmap!!)

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

    companion object : KoinComponent {
        const val NOTIFICATION_ID = 228
        const val NOTIFICATION_CHANNEL_ID = "PersistentNotification"
        const val DATA_UPDATE_FREQ = 5

        private val _todayUsageFlow = MutableStateFlow(DayUsage())
        val todayUsageFlow = _todayUsageFlow.asStateFlow()
        var todayUsage: DayUsage
            get() = _todayUsageFlow.value
            set(value) {
                _todayUsageFlow.value = value
            }

        private var instance: UsageService? = null

        fun isInstanceCreated(): Boolean {
            return instance != null
        }

        fun startService(context: Context) {
            val preferenceRepo: PreferenceRepo by inject()
            CoroutineScope(Dispatchers.Default).launch {
                val enabled = preferenceRepo.notification.first()
                if (!isInstanceCreated() && enabled) {
                    val intent = Intent(context, UsageService::class.java)
                    context.startService(intent)
                    Timber.i("Started service")
                }
            }
        }

        fun stopService() {
            if (isInstanceCreated()) {
                instance?.stopSelf()
                instance = null
            }
        }
    }
}
