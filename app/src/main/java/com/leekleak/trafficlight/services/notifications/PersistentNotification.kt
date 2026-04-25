package com.leekleak.trafficlight.services.notifications

import android.app.Notification
import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.lifecycle.LifecycleService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel

abstract class PersistentNotification(
    serviceScope: CoroutineScope,
    internal val context: Context,
    internal val notificationManager: NotificationManager,
    internal val notificationId: Int,
) {
    internal val scope = CoroutineScope(serviceScope.coroutineContext + SupervisorJob(serviceScope.coroutineContext[Job]))
    internal var job: Job? = null
    internal lateinit var notificationBuilder: NotificationCompat.Builder
    internal lateinit var notification: Notification
    internal var notificationIconHelper = NotificationIconHelper(context)
    abstract fun start()
    fun cancel() {
        scope.cancel()
        notificationManager.cancel(notificationId)
    }
    fun startForeground(service: LifecycleService) {
        service.startForeground(
            notificationId,
            notification,
        )
    }
    abstract fun screenStateChange(on: Boolean)
}