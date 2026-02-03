package com.leekleak.trafficlight.widget

import android.appwidget.AppWidgetManager
import android.content.Context
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.state.getAppWidgetState
import androidx.glance.state.PreferencesGlanceStateDefinition
import com.leekleak.trafficlight.widget.Widget.Companion.SUBSCRIBER_ID
import kotlinx.coroutines.runBlocking

class WidgetReceiver: GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = Widget()

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {

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

        runBlocking {
            val newIds = appWidgetIds.filter {
                val glanceId = GlanceAppWidgetManager(context).getGlanceIdBy(it)
                val prefs = getAppWidgetState(context, PreferencesGlanceStateDefinition, glanceId)
                prefs[SUBSCRIBER_ID] != null
            }.toIntArray()

            super.onUpdate(context, appWidgetManager, newIds)
        }
    }
}