package com.leekleak.trafficlight.services.notifications

import android.app.Notification
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.core.app.NotificationCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import com.leekleak.trafficlight.R
import com.leekleak.trafficlight.database.AppPreferenceRepo
import com.leekleak.trafficlight.database.DataPlanDao
import com.leekleak.trafficlight.services.notifications.SpeedNotification.Companion.NOTIFICATION_CHANNEL_ID_SILENT
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.koin.android.ext.android.get
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.koin.core.parameter.parametersOf
import timber.log.Timber
import kotlin.concurrent.atomics.AtomicBoolean
import kotlin.concurrent.atomics.ExperimentalAtomicApi

class NotificationService : LifecycleService(), KoinComponent {
    private val appPreferenceRepo: AppPreferenceRepo by inject()
    private val dataPlanDao: DataPlanDao by inject()
    private var foregroundNotification: PersistentNotification? = null
    private var notificationIDCounter = 1
    private val activeNotifications: MutableList<PersistentNotification> = mutableListOf()
    private val screenStateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                Intent.ACTION_SCREEN_ON -> {
                    activeNotifications.forEach { it.screenStateChange(true) }
                }
                Intent.ACTION_SCREEN_OFF -> {
                    activeNotifications.forEach { it.screenStateChange(false) }
                }
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        Timber.i("Creating UsageService")

        registerReceiver(screenStateReceiver, IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_ON)
            addAction(Intent.ACTION_SCREEN_OFF)
        })

        lifecycleScope.launch {
            appPreferenceRepo.notification.collect { enabled ->
                if (enabled) {
                    val id = notificationIDCounter.also { notificationIDCounter++ }
                    val notif = get<SpeedNotification> { parametersOf(lifecycleScope, id) }
                    notif.start()
                    activeNotifications.add(notif)
                    updateForegroundNotification()
                } else {
                    val notif = activeNotifications.find { it is SpeedNotification }
                    notif?.let { notification ->
                        activeNotifications.remove(notification)
                        updateForegroundNotification()
                        notification.cancel()
                    }
                }
            }
        }
        lifecycleScope.launch {
            dataPlanDao.getActivePlansWithNotificationsFlow().collect { list ->
                val activePlanNotifications = activeNotifications.filterIsInstance<PlanNotification>()
                list.forEach { plan ->
                    val notification = activePlanNotifications.find { it.dataPlan == plan }
                    if (notification != null || !plan.notification) return@forEach

                    val id = notificationIDCounter.also { notificationIDCounter++ }
                    val notif = get<PlanNotification> { parametersOf(lifecycleScope, id, plan) }
                    notif.start()
                    activeNotifications.add(notif)
                    updateForegroundNotification()
                }
                activePlanNotifications.forEach { notification ->
                    if (!list.contains(notification.dataPlan)) {
                        activeNotifications.remove(notification)
                        updateForegroundNotification()
                        notification.cancel()
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(screenStateReceiver)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        Timber.i("Starting foreground service")
        val id = notificationIDCounter.also { notificationIDCounter++ }
        startForeground(id, placeholderNotification())
        activeNotifications.forEach { it.start() }
        updateForegroundNotification()
        return START_STICKY
    }

    fun updateForegroundNotification() {
        if (activeNotifications.contains(foregroundNotification)) return
        val firstNotification = activeNotifications.firstOrNull()
        firstNotification?.let {
            try {
                it.startForeground(this)
                foregroundNotification = it
            } catch (e: Exception) {
                Timber.e("Failed to start foreground service: $e")
            }
        } ?: stopForeground(STOP_FOREGROUND_REMOVE)
    }

    fun placeholderNotification(): Notification {
        return NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID_SILENT)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(getString(R.string.app_name_short))
            .setSilent(true)
            .setLocalOnly(true)
            .setOnlyAlertOnce(true)
            .build()
    }

    @OptIn(ExperimentalAtomicApi::class)
    companion object: KoinComponent {
        private var running = AtomicBoolean(false)
        fun startService(context: Context, scope: CoroutineScope) {
            scope.launch {
                val dataPlanDao: DataPlanDao by inject()
                val appPreferenceRepo: AppPreferenceRepo by inject()
                val notificationPlans = dataPlanDao.getActivePlansWithNotificationsFlow().first()
                val notificationSpeed = appPreferenceRepo.notification.first()
                if (notificationPlans.isNotEmpty() || notificationSpeed) {
                    if (!running.exchange(true)) {
                        context.startForegroundService(Intent(context, NotificationService::class.java))
                    }
                }
            }
        }
    }
}