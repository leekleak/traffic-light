package com.leekleak.trafficlight.widget

import android.content.Context
import android.os.Build
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.setWidgetPreviews
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.time.Duration.Companion.seconds

fun installWidgetPreviews(context: Context, scope: CoroutineScope) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.VANILLA_ICE_CREAM) return

    scope.launch {
        withContext(Dispatchers.IO) {
            // Apparently if you refresh previews too soon on app launch they'll be ignored
            delay(1.seconds)
            GlanceAppWidgetManager(context).setWidgetPreviews<WidgetReceiver>()
        }
    }
}
