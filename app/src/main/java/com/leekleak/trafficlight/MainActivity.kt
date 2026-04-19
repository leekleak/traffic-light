package com.leekleak.trafficlight

import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.setWidgetPreviews
import androidx.lifecycle.lifecycleScope
import coil3.ImageLoader
import coil3.compose.setSingletonImageLoaderFactory
import com.leekleak.trafficlight.ui.app.App
import com.leekleak.trafficlight.ui.theme.Theme
import com.leekleak.trafficlight.widget.WidgetReceiver
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
            lifecycleScope.launch {
                delay(1000) // Apparently if you refresh previews too soon on app launch they'll be ignored
                GlanceAppWidgetManager(this@MainActivity).setWidgetPreviews<WidgetReceiver>()
            }
        }

        setContent {
            val imageLoader: ImageLoader = koinInject()
            setSingletonImageLoaderFactory { imageLoader }

            Theme {
                App()
            }
        }
    }
}