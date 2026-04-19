package com.leekleak.trafficlight.services

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import com.leekleak.trafficlight.database.AppPreferenceRepo
import com.leekleak.trafficlight.database.DayUsage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import org.koin.android.ext.android.get
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.koin.core.parameter.parametersOf
import timber.log.Timber
import java.lang.ref.WeakReference

class UsageService : LifecycleService(), KoinComponent {
    private val appPreferenceRepo: AppPreferenceRepo by inject()
    private var foregroundNotification: PersistentNotification? = null
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

    init {
        lifecycleScope.launch {
            appPreferenceRepo.notification.collect { enabled ->
                if (enabled) {
                    val notif = get<SpeedNotification> { parametersOf(lifecycleScope) }
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
    }

    private val activeNotifications: MutableList<PersistentNotification> = mutableListOf()

    override fun onCreate() {
        super.onCreate()
        instance = WeakReference(this)
        Timber.i("Creating UsageService")

        registerReceiver(screenStateReceiver, IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_ON)
            addAction(Intent.ACTION_SCREEN_OFF)
        })
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
        if (firstNotification == null) {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } else {
            try {
                firstNotification.startForeground(this)
            } catch (e: Exception) {
                Timber.e("Failed to start foreground service: $e")
            }
        }
    }

    companion object {
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
            if (!isInstanceCreated()) {
                val intent = Intent(context, UsageService::class.java)
                context.startService(intent)
                Timber.i("Started service")
            }
        }
    }
}
