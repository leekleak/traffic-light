package com.leekleak.trafficlight.widget

import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.content.Intent.ACTION_SCREEN_OFF
import android.content.Intent.ACTION_SCREEN_ON
import android.content.IntentFilter
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.state.getAppWidgetState
import androidx.glance.state.PreferencesGlanceStateDefinition
import com.leekleak.trafficlight.widget.Widget.Companion.SUBSCRIBER_ID
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class WidgetReceiver: GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = Widget()
    private var registered: Boolean = false

    @OptIn(DelicateCoroutinesApi::class)
    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        val pendingResult = goAsync()
        registerReceiver(context)

        /**
         * Unfortunately Glance widgets have a really stupid rate limit which stops the app from updating
         * the widget more than once every ~1min.
         *
         * The problem is that before widget creation the widget or the launcher or whatever asks the widget to update.
         * Since, of course, the widget is not yet configured, it returns early, leaving the widget empty.
         *
         * The early update also triggers the rate limit which means that after the configuration is
         * actually done the update fails!
         *
         * Very stupid, but if you just ignore and don't update widgets with no subscriberId, it works fine.
         */


        CoroutineScope(Dispatchers.IO).launch {
            for (appWidgetId in appWidgetIds) {
                try {
                    val glanceId = GlanceAppWidgetManager(context).getGlanceIdBy(appWidgetId)
                    val prefs = getAppWidgetState(context, PreferencesGlanceStateDefinition, glanceId)

                    if (prefs[SUBSCRIBER_ID] != null) {
                        glanceAppWidget.update(context, glanceId)
                    }
                } catch (_: Exception) { }
            }
            pendingResult.finish()
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            ACTION_SCREEN_ON -> {
                startAlarmManager(context)
            }
            ACTION_SCREEN_OFF -> {
                killAlarmManager(context)
            }
            else -> {
                super.onReceive(context, intent)
            }
        }
    }


    fun registerReceiver(context: Context) {
        if (registered) return
        context.applicationContext.registerReceiver(this, IntentFilter().apply {
            addAction(ACTION_SCREEN_ON)
            addAction(ACTION_SCREEN_OFF)
        })

        registered = true
    }
}