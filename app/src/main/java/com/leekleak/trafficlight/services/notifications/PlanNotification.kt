package com.leekleak.trafficlight.services.notifications

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import androidx.collection.LruCache
import androidx.compose.ui.unit.Density
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.graphics.createBitmap
import androidx.core.graphics.drawable.IconCompat
import androidx.lifecycle.LifecycleService
import com.leekleak.trafficlight.MainActivity
import com.leekleak.trafficlight.R
import com.leekleak.trafficlight.database.DataDirection
import com.leekleak.trafficlight.database.DataPlan
import com.leekleak.trafficlight.database.DataType
import com.leekleak.trafficlight.database.DayUsage
import com.leekleak.trafficlight.database.TrafficSnapshot
import com.leekleak.trafficlight.database.UsageQuery
import com.leekleak.trafficlight.model.NetworkUsageManager
import com.leekleak.trafficlight.util.DataSize
import com.leekleak.trafficlight.util.clipAndPad
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

class PlanNotification(
    serviceScope: CoroutineScope,
    private val context: Context,
    private val notificationId: Int,
    val dataPlan: DataPlan,
    private val networkUsageManager: NetworkUsageManager,
    private val notificationManager: NotificationManager,
) : PersistentNotification {
    private val scope = CoroutineScope(serviceScope.coroutineContext + SupervisorJob(serviceScope.coroutineContext[Job]))
    private var job: Job? = null
    private lateinit var notificationBuilder: NotificationCompat.Builder
    private lateinit var notification: Notification
    private val trafficSnapshot = TrafficSnapshot(scope)

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
    private var todayUsage = DayUsage()

    init {
        updateBaseNotification()
        notification.flags = Notification.FLAG_ONGOING_EVENT or Notification.FLAG_NO_CLEAR
        notificationManager.notify(notificationId, notification)
    }

    override fun start() {
        if (job?.isActive ?: false) return
        job = scope.launch {
            while (true) {
                updateNotification()
                delay(5000)
            }
        }

    }

    override fun cancel() {
        scope.cancel()
        notificationManager.cancel(notificationId)
    }

    override fun startForeground(service: LifecycleService) {
        ServiceCompat.startForeground(
            service,
            notificationId,
            notification,
            ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
        )
    }

    override fun screenStateChange(on: Boolean) {
        if (on) start()
        else job?.cancel()
    }

    override fun getId(): Int = notificationId

    private var lastTitle: String = ""
    private suspend fun updateNotification() {
        val speed = DataSize(trafficSnapshot.totalSpeed).asString()
        val title = context.getString(R.string.speed, speed)

        if (lastTitle == speed) return // If the title is the same, so is the icon.
        else lastTitle = speed

        val spacing = 18
        val messageShort =
            context.getString(R.string.wi_fi, DataSize(todayUsage.usage2).asString()).clipAndPad(spacing) +
            context.getString(R.string.mobile, DataSize(todayUsage.usage1).asString())

        updateBaseNotification()
        notification = notificationBuilder
            .apply {
                setSmallIcon(R.drawable.bigtop_updates)
                setShortCriticalText(speed)
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
            typeface = context.resources.getFont(R.font.roboto_condensed_semi_bold)
            textAlign = Paint.Align.CENTER
        }
    }

    private var cachedIcons = LruCache<String, IconCompat>(50)
    private var bitmap: Bitmap? = null
    private val bitmapMutex = Mutex()
    private suspend fun createIcon(snapshot: TrafficSnapshot): IconCompat = withContext(Dispatchers.Default) {
        val density = Density(context)
        val multiplier = 24 * density.density / 96f
        val height = (96 * multiplier).toInt()

        val data = DataSize(snapshot.totalSpeed).asString()
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

    private fun updateBaseNotification() {
        notificationBuilder = NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(context.getString(R.string.app_name_short))
            .setOngoing(true)
            .setRequestPromotedOngoing(true)
            .setSilent(true)
            .setLocalOnly(true)
            .setOnlyAlertOnce(true)
            .setWhen(Long.MAX_VALUE) // Keep above other notifications
            .setShowWhen(false) // Hide timestamp
            //.setGroup("id_$notificationId")
            .setContentIntent(
                PendingIntent.getActivity(
                    context, 0, Intent(context, MainActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
                    }, PendingIntent.FLAG_IMMUTABLE
                )
            )
        notification = notificationBuilder.build()
    }

    companion object {
        const val NOTIFICATION_CHANNEL_ID = "PlanNotification"
    }
}