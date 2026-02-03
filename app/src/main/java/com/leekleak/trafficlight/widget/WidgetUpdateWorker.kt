package com.leekleak.trafficlight.widget

import android.content.Context
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.updateAll
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.Worker
import androidx.work.WorkerParameters
import kotlinx.coroutines.runBlocking
import java.util.concurrent.TimeUnit

class WidgetUpdateWorker(val context: Context, params: WorkerParameters) : Worker(context, params) {
    override fun doWork(): Result {
        runBlocking {
            Widget().updateAll(context)
        }

        enqueue(context)

        return Result.success()
    }

    companion object {
        fun enqueue(context: Context) {
            val manager = GlanceAppWidgetManager(context)
            val ids = runBlocking { manager.getGlanceIds(Widget::class.java) }
            if (ids.isEmpty()) return // Only start chain if there are widgets

            val nextRequest = OneTimeWorkRequestBuilder<WidgetUpdateWorker>()
                .setInitialDelay(1, TimeUnit.MINUTES)
                .build()

            WorkManager.getInstance(context).enqueueUniqueWork(
                "TrafficLightWidgets",
                ExistingWorkPolicy.REPLACE,
                nextRequest
            )
        }
    }
}