package com.leekleak.trafficlight

import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import coil3.ImageLoader
import coil3.compose.setSingletonImageLoaderFactory
import coil3.request.crossfade
import com.leekleak.trafficlight.model.AppIconFetcher
import com.leekleak.trafficlight.services.UsageService.Companion.NOTIFICATION_CHANNEL_ID
import com.leekleak.trafficlight.ui.app.App
import com.leekleak.trafficlight.ui.theme.Theme
import com.leekleak.trafficlight.widget.WidgetUpdateWorker
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import timber.log.Timber

class MainActivity : ComponentActivity(), KoinComponent {
    private val appIconFactory: AppIconFetcher.Factory by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }
        enableEdgeToEdge()
        createNotificationChannel()

        WidgetUpdateWorker.enqueue(this)

        setContent {
            setSingletonImageLoaderFactory {
                ImageLoader.Builder(this)
                    .components {
                        add(appIconFactory)
                    }
                    .crossfade(true)
                    .build()
            }

            Theme {
                App()
            }
        }
    }

    private fun createNotificationChannel() {
        val name = "Traffic Light Service"
        val importance = NotificationManager.IMPORTANCE_DEFAULT
        val channel = NotificationChannel(NOTIFICATION_CHANNEL_ID, name, importance).apply {
            setShowBadge(false)
        }
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
    }
}