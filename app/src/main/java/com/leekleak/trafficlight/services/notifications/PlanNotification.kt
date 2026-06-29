package com.leekleak.trafficlight.services.notifications

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.leekleak.trafficlight.MainActivity
import com.leekleak.trafficlight.R
import com.leekleak.trafficlight.database.AppPreferenceRepo
import com.leekleak.trafficlight.database.DataPlan
import com.leekleak.trafficlight.model.NetworkUsageManager
import com.leekleak.trafficlight.util.DataSize
import com.leekleak.trafficlight.util.simIconRes
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.seconds

class PlanNotification(
    serviceScope: CoroutineScope,
    context: Context,
    notificationManager: NotificationManager,
    notificationId: Int,
    val dataPlan: DataPlan,
    private val networkUsageManager: NetworkUsageManager,
    private val appPreferenceRepo: AppPreferenceRepo,
) : PersistentNotification(serviceScope, context, notificationManager, notificationId) {

    private var sizeMetric = false

    init {
        scope.launch {
            appPreferenceRepo.sizeMetric.collect { sizeMetric = it; updateNotification() }
        }
        updateBaseNotification()
    }

    override fun start() {
        if (job?.isActive == true) return
        job = scope.launch {
            while (true) {
                updateNotification()
                delay(5.seconds)
            }
        }

    }

    override fun screenStateChange(on: Boolean) {
        if (on) start()
        else job?.cancel()
    }

    private suspend fun updateNotification() {
        val dataSize = DataSize(dataPlan.getTotalUsage(networkUsageManager))
        val dataSizeMax = DataSize(dataPlan.getTotalMax())
        val progress = dataSize.byteValue.toDouble() / dataSizeMax.byteValue.toDouble()

        val data = dataSize.toString(metric = sizeMetric)
        val speed = data.substringBefore(" ")
        val unit = data.substringAfter(" ")
        val maxString = if (dataPlan.mainDataSize.byteValue!=0L) "/${dataSizeMax.toString(metric = sizeMetric)}" else ""
        notification = notificationBuilder
            .apply {
                if (!dataPlan.liveNotification) {
                    setSmallIcon(notificationIconHelper.createIcon(speed, unit))
                    setWhen(Long.MAX_VALUE) // Keep above other notifications
                    setShowWhen(false) // Hide timestamp
                }
                else  {
                    setSmallIcon(simIconRes(dataPlan.simIndex))
                    setShortCriticalText(dataSize.toString(metric = sizeMetric))
                }
                if (dataPlan.mainDataSize.byteValue != 0L) {
                    setProgress(100, (progress*100).toInt(), false)
                }
            }
            .setContentTitle("${dataSize.toString(metric = sizeMetric)}$maxString")
            .setContentText(dataPlan.resetString(context))
            .build()
        notifySafely(notificationId, notification)
    }

    private fun updateBaseNotification() {
        notificationBuilder = NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(R.drawable.notification)
            .setContentTitle(context.getString(R.string.app_name_short))
            .setOngoing(true)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .setRequestPromotedOngoing(dataPlan.liveNotification)
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

    companion object {
        const val NOTIFICATION_CHANNEL_ID = "PlanNotification"
    }
}