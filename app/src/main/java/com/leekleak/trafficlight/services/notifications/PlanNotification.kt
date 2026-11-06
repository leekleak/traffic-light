package com.leekleak.trafficlight.services.notifications

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.leekleak.trafficlight.MainActivity
import com.leekleak.core.R
import com.leekleak.trafficlight.database.AppPreferenceRepo
import com.leekleak.trafficlight.model.DataPlanManager
import com.leekleak.core.util.DataSize
import com.leekleak.core.util.simIconRes
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.seconds

class PlanNotification(
    serviceScope: CoroutineScope,
    context: Context,
    notificationManager: NotificationManager,
    notificationId: Int,
    val hashedSubscriberID: String,
    private val dataPlanManager: DataPlanManager,
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
        val dataPlan = dataPlanManager.getPlan(hashedSubscriberID) ?: return
        val snapshot = dataPlanManager.getSnapshot(dataPlan)
        val dataSize = DataSize(snapshot.mainDataUsed)
        val dataSizeMax = dataPlan.mainDataSize
        val progress = if (dataSizeMax.byteValue != 0L) dataSize.byteValue.toDouble() / dataSizeMax.byteValue.toDouble() else 0.0

        val data = dataSize.toString(metric = sizeMetric)
        val speed = data.substringBefore(" ")
        val unit = data.substringAfter(" ")
        val maxString = if (dataPlan.mainDataSize.byteValue!=0L) "/${dataSizeMax.toString(metric = sizeMetric)}" else ""
        notification =
            (
                if (dataPlan.liveNotification) {
                    buildForChannel(NOTIFICATION_CHANNEL_ID, promoted = true)
                } else {
                    notificationBuilder
                }
            )
            .apply {
                if (!dataPlan.liveNotification) {
                    setSmallIcon(notificationIconHelper.createIcon(speed, unit))
                    setShowWhen(false)
                    setWhen(System.currentTimeMillis() + DECADE)
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

    private fun buildForChannel(
        channel: String,
        promoted: Boolean = false
    ): NotificationCompat.Builder = NotificationCompat.Builder(context, channel)
        .setSmallIcon(R.drawable.notification)
        .setContentTitle(context.getString(R.string.app_name_short))
        .setOngoing(true)
        .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
        .setRequestPromotedOngoing(promoted)
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

    private fun updateBaseNotification() {
        notificationBuilder = buildForChannel(NOTIFICATION_CHANNEL_ID)

        notification = notificationBuilder.build()
    }

    companion object {
        const val NOTIFICATION_CHANNEL_ID = "PlanNotification"
    }
}