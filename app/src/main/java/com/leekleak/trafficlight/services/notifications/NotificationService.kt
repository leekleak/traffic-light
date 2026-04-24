package com.leekleak.trafficlight.services.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import com.leekleak.trafficlight.database.AppPreferenceRepo
import com.leekleak.trafficlight.database.DataPlanDao
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.koin.android.ext.android.get
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.koin.core.parameter.parametersOf
import timber.log.Timber

class NotificationService : LifecycleService(), KoinComponent {
    private val appPreferenceRepo: AppPreferenceRepo by inject()
    private val dataPlanDao: DataPlanDao by inject()
    private var foregroundNotification: PersistentNotification? = null
    private val mutableSet = buildSet { (228..250).forEach { add(it) } }.toMutableSet()
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
                    val id = mutableSet.firstOrNull() ?: return@collect
                    val notif = get<SpeedNotification> { parametersOf(lifecycleScope, id) }
                    mutableSet.remove(id)
                    notif.start()
                    activeNotifications.add(notif)
                    updateForegroundNotification()
                } else {
                    val notif = activeNotifications.find { it is SpeedNotification }
                    notif?.let { notification ->
                        activeNotifications.remove(notification)
                        updateForegroundNotification()
                        notification.cancel()
                        mutableSet.add(notification.getId())
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

                    val id = mutableSet.firstOrNull() ?: return@collect
                    val notif = get<PlanNotification> { parametersOf(lifecycleScope, id, plan) }
                    mutableSet.remove(id)
                    notif.start()
                    activeNotifications.add(notif)
                    updateForegroundNotification()
                }
                activePlanNotifications.forEach { notification ->
                    if (!list.contains(notification.dataPlan)) {
                        activeNotifications.remove(notification)
                        updateForegroundNotification()
                        notification.cancel()
                        mutableSet.add(notification.getId())
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

    companion object: KoinComponent {
        fun startService(context: Context, scope: CoroutineScope) {
            scope.launch {
                val dataPlanDao: DataPlanDao by inject()
                val appPreferenceRepo: AppPreferenceRepo by inject()
                val notificationPlans = dataPlanDao.getActivePlansWithNotificationsFlow().first()
                val notificationSpeed = appPreferenceRepo.notification.first()
                if (notificationPlans.isNotEmpty() || notificationSpeed) {
                    context.startForegroundService(Intent(context, NotificationService::class.java))
                }
            }
        }
    }
}