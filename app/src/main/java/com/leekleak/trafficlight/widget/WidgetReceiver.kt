package com.leekleak.trafficlight.widget

import android.annotation.SuppressLint
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
import com.leekleak.trafficlight.widget.Widget.Companion.SUBSCRIBER_ID_HASH
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import kotlin.concurrent.atomics.AtomicBoolean
import kotlin.concurrent.atomics.ExperimentalAtomicApi

class WidgetReceiver: GlanceAppWidgetReceiver(), KoinComponent {
    private val applicationScope: CoroutineScope by inject()
    override val glanceAppWidget: GlanceAppWidget = Widget()

    @SuppressLint("MissingSuperCall")
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

        applicationScope.launch {
            for (appWidgetId in appWidgetIds) {
                try {
                    val glanceId = GlanceAppWidgetManager(context).getGlanceIdBy(appWidgetId)
                    val prefs = getAppWidgetState(context, PreferencesGlanceStateDefinition, glanceId)

                    if (prefs[SUBSCRIBER_ID_HASH] != null) {
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

    @OptIn(ExperimentalAtomicApi::class)
    fun registerReceiver(context: Context) {
        if (registered.exchange(true)) return
        context.applicationContext.registerReceiver(this, IntentFilter().apply {
            addAction(ACTION_SCREEN_ON)
            addAction(ACTION_SCREEN_OFF)
        })
    }

    companion object {
        @OptIn(ExperimentalAtomicApi::class)
        private var registered = AtomicBoolean(false)
    }
}