package com.leekleak.trafficlight

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.NotificationManager.IMPORTANCE_DEFAULT
import android.app.NotificationManager.IMPORTANCE_NONE
import com.leekleak.trafficlight.database.databaseModule
import com.leekleak.trafficlight.model.managerModule
import com.leekleak.trafficlight.services.SpeedNotification.Companion.NOTIFICATION_CHANNEL_ID
import com.leekleak.trafficlight.services.SpeedNotification.Companion.NOTIFICATION_CHANNEL_ID_SILENT
import com.leekleak.trafficlight.services.notificationModule
import com.leekleak.trafficlight.ui.navigation.navigationModule
import com.leekleak.trafficlight.ui.viewModelModule
import com.leekleak.trafficlight.widget.startAlarmManager
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.GlobalContext.startKoin
import timber.log.Timber

class TrafficLightApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        createNotificationChannel()
        startAlarmManager(this)
        if (BuildConfig.DEBUG && Timber.forest().isEmpty()) {
            Timber.plant(Timber.DebugTree())
        }

        startKoin {
            androidContext(this@TrafficLightApplication)
            modules(
                systemServiceModule,
                databaseModule,
                managerModule,
                viewModelModule,
                navigationModule,
                notificationModule
            )
        }
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(NOTIFICATION_CHANNEL_ID, "Persistent Notification", IMPORTANCE_DEFAULT).apply {
            setShowBadge(false)
        }
        val channelSilent = NotificationChannel(NOTIFICATION_CHANNEL_ID_SILENT, "Persistent Notification (Disconnected)", IMPORTANCE_NONE).apply {
            setShowBadge(false)
        }
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannels(listOf(channel, channelSilent))
    }
}