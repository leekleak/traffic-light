package com.leekleak.trafficlight

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.NotificationManager.IMPORTANCE_DEFAULT
import android.app.NotificationManager.IMPORTANCE_NONE
import com.leekleak.trafficlight.database.databaseModule
import com.leekleak.trafficlight.model.managerModule
import com.leekleak.trafficlight.services.notifications.PlanNotification
import com.leekleak.trafficlight.services.notifications.SpeedNotification
import com.leekleak.trafficlight.services.notifications.notificationModule
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
        startAlarmManager(this)
    }

    private fun createNotificationChannel() {
        val speedChannel = NotificationChannel(SpeedNotification.NOTIFICATION_CHANNEL_ID, "Persistent Notification", IMPORTANCE_DEFAULT).apply {
            setShowBadge(false)
        }
        val speedChannelSilent = NotificationChannel(SpeedNotification.NOTIFICATION_CHANNEL_ID_SILENT, "Persistent Notification (Disconnected)", IMPORTANCE_NONE).apply {
            setShowBadge(false)
        }
        val planChannel = NotificationChannel(PlanNotification.NOTIFICATION_CHANNEL_ID, "Plan Notification", IMPORTANCE_DEFAULT).apply {
            setShowBadge(false)
        }
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannels(listOf(
            speedChannel,
            speedChannelSilent,
            planChannel
            ))
    }
}