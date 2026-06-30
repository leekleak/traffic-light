package com.leekleak.trafficlight.services.notifications

import android.app.Notification
import android.app.NotificationManager
import android.content.Context
import android.content.pm.ServiceInfo
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.lifecycle.LifecycleService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import timber.log.Timber

abstract class PersistentNotification(
    serviceScope: CoroutineScope,
    internal val context: Context,
    internal val notificationManager: NotificationManager,
    internal val notificationId: Int,
) {
    internal val scope = CoroutineScope(serviceScope.coroutineContext + SupervisorJob(serviceScope.coroutineContext[Job]))
    internal var job: Job? = null
    internal var notificationBuilder: NotificationCompat.Builder = NotificationCompat.Builder(context, "temporary id")
    internal var notification: Notification = notificationBuilder.build()
    internal var notificationIconHelper = NotificationIconHelper(context)
    abstract fun start()
    fun cancel() {
        scope.cancel()
        try {
            notificationManager.cancel(notificationId)
        } catch (e: SecurityException) {
            Timber.e(e, "SecurityException while cancelling notification $notificationId")
        }
    }
    fun startForeground(service: LifecycleService) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            service.startForeground(
                notificationId,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            )
            Timber.e("STARTED FOREGROUND")
        } else {
            service.startForeground(
                notificationId,
                notification,
            )
        }
    }

    internal fun notifySafely(id: Int, notification: Notification) {
        try {
            notificationManager.notify(id, notification)
        } catch (e: SecurityException) {
            Timber.e(e, "SecurityException while notifying $id")
        }
    }
    abstract fun screenStateChange(on: Boolean)
}