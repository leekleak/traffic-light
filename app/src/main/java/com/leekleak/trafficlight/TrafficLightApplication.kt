package com.leekleak.trafficlight

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.NotificationManager.IMPORTANCE_DEFAULT
import android.app.NotificationManager.IMPORTANCE_HIGH
import android.app.NotificationManager.IMPORTANCE_LOW
import android.os.StrictMode
import android.os.StrictMode.ThreadPolicy
import android.os.StrictMode.VmPolicy
import com.leekleak.trafficlight.database.databaseModule
import com.leekleak.trafficlight.integrations.integrationsModule
import com.leekleak.trafficlight.model.managerModule
import com.leekleak.trafficlight.services.notifications.PlanNotification
import com.leekleak.trafficlight.services.notifications.SpeedNotification
import com.leekleak.trafficlight.services.notifications.WarningNotificationHelper
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

        if (BuildConfig.DEBUG) {
            StrictMode.setVmPolicy(
                VmPolicy.Builder(StrictMode.getVmPolicy()).apply {
                    detectAll()
                    penaltyLog()
                }.build()
            )
            StrictMode.setThreadPolicy(
                ThreadPolicy.Builder(StrictMode.getThreadPolicy())
                    .detectAll()
                    .penaltyLog()
                    .build()
            )
        }

        startKoin {
            androidContext(this@TrafficLightApplication)
            modules(
                systemServiceModule,
                databaseModule,
                managerModule,
                viewModelModule,
                navigationModule,
                notificationModule,
                integrationsModule
            )
        }
        startAlarmManager(this)
    }

    private fun createNotificationChannel() {
        val speedChannel = NotificationChannel(
            SpeedNotification.NOTIFICATION_CHANNEL_ID,
            getString(R.string.persistent_notification),
            IMPORTANCE_DEFAULT
        ).apply {
            setShowBadge(false)
        }

        val speedChannelSilent = NotificationChannel(
            SpeedNotification.NOTIFICATION_CHANNEL_ID_SILENT,
            getString(R.string.persistent_notification_silent),
            IMPORTANCE_LOW
        ).apply {
            setShowBadge(false)
        }

        val planChannel = NotificationChannel(
            PlanNotification.NOTIFICATION_CHANNEL_ID,
            getString(R.string.plan_notification),
            IMPORTANCE_DEFAULT
        ).apply {
            setShowBadge(false)
        }

        val planWarningChannel = NotificationChannel(
            WarningNotificationHelper.NOTIFICATION_CHANNEL_ID,
            getString(R.string.plan_warning_notification),
            IMPORTANCE_HIGH
        ).apply {
            enableLights(true)
            enableVibration(true)
            setShowBadge(true)
        }

        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager

        val currentChannels = listOf(
            SpeedNotification.NOTIFICATION_CHANNEL_ID,
            SpeedNotification.NOTIFICATION_CHANNEL_ID_SILENT,
            PlanNotification.NOTIFICATION_CHANNEL_ID,
            WarningNotificationHelper.NOTIFICATION_CHANNEL_ID
        )
        
        notificationManager.notificationChannels.forEach {
            if (!currentChannels.contains(it.id)) {
                notificationManager.deleteNotificationChannel(it.id)
            }
        }

        notificationManager.createNotificationChannels(
            listOf(
                speedChannel,
                speedChannelSilent,
                planChannel,
                planWarningChannel
            )
        )
    }
}