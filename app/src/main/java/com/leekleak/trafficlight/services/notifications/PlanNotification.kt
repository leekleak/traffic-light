package com.leekleak.trafficlight.services.notifications

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.leekleak.trafficlight.MainActivity
import com.leekleak.trafficlight.R
import com.leekleak.trafficlight.database.DataPlan
import com.leekleak.trafficlight.model.NetworkUsageManager
import com.leekleak.trafficlight.util.DataSize
import com.leekleak.trafficlight.util.simIconRes
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class PlanNotification(
    serviceScope: CoroutineScope,
    context: Context,
    notificationManager: NotificationManager,
    notificationId: Int,
    val dataPlan: DataPlan,
    private val networkUsageManager: NetworkUsageManager,
) : PersistentNotification(serviceScope, context, notificationManager, notificationId) {

    init {
        updateBaseNotification()
        notification.flags = Notification.FLAG_ONGOING_EVENT or Notification.FLAG_NO_CLEAR
        notificationManager.notify(notificationId, notification)
    }

    override fun start() {
        if (job?.isActive == true) return
        job = scope.launch {
            while (true) {
                updateNotification()
                delay(5000)
            }
        }

    }

    override fun screenStateChange(on: Boolean) {
        if (on) start()
        else job?.cancel()
    }

    private suspend fun updateNotification() {
        val dataSize = DataSize(networkUsageManager.planUsage(dataPlan))
        val dataSizeMax = DataSize(dataPlan.dataMax)
        val progress = dataSize.byteValue.toDouble() / dataSizeMax.byteValue.toDouble()

        val data = dataSize.toString()
        val speed = data.substringBefore(" ")
        val unit = data.substringAfter(" ")
        notification = notificationBuilder
            .apply {
                if (!dataPlan.liveNotification) {
                    setSmallIcon(notificationIconHelper.createIcon(speed, unit))
                    setWhen(Long.MAX_VALUE) // Keep above other notifications
                    setShowWhen(false) // Hide timestamp
                }
                else  {
                    setSmallIcon(simIconRes(dataPlan.simIndex))
                    setShortCriticalText(dataSize.toString())
                }
            }
            .setContentTitle("$dataSize/$dataSizeMax")
            .setContentText(dataPlan.resetString(context))
            .setProgress(100, (progress*100).toInt(), false)
            .build()
        notification.flags = Notification.FLAG_ONGOING_EVENT or Notification.FLAG_NO_CLEAR
        notificationManager.notify(notificationId, notification)
    }

    private fun updateBaseNotification() {
        notificationBuilder = NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(context.getString(R.string.app_name_short))
            .setOngoing(true)
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