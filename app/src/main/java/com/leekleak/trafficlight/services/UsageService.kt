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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.koin.core.component.KoinComponent
import org.koin.core.component.get
import org.koin.core.component.inject
import org.koin.core.parameter.parametersOf
import timber.log.Timber
import java.lang.ref.WeakReference


class UsageService : LifecycleService() {
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

    private val activeNotifications: List<PersistentNotification> by lazy {
        listOf(
            get<SpeedNotification> { parametersOf(lifecycleScope) },
        )
    }

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
        val firstNotification = activeNotifications.firstOrNull() ?: return START_STICKY
        activeNotifications.forEach { it.start() }
        try {
            firstNotification.startForeground(this)
        } catch (e: Exception) {
            Timber.e("Failed to start foreground service: $e")
        }
        return START_STICKY
    }

    companion object : KoinComponent {

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
            val appPreferenceRepo: AppPreferenceRepo by inject()
            val enabled = runBlocking { appPreferenceRepo.notification.first() }
            if (!isInstanceCreated() && enabled) {
                val intent = Intent(context, UsageService::class.java)
                context.startService(intent)
                Timber.i("Started service")
            }
        }

        fun stopService() {
            instance.get()?.stopSelf()
            instance.clear()
        }
    }
}
