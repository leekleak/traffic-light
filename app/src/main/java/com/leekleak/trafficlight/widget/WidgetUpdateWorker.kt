package com.leekleak.trafficlight.widget

import android.content.Context
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.updateAll
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import timber.log.Timber
import java.util.concurrent.TimeUnit

class WidgetUpdateWorker(val context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        Widget().updateAll(context)
        enqueue(context) // Technically a bit of a race condition, but one that would result in a an info level warning in the logcat
        return Result.success()
    }

    companion object {
        fun enqueue(context: Context) = CoroutineScope(Dispatchers.IO).launch {
            val manager = GlanceAppWidgetManager(context)
            val ids = runBlocking { manager.getGlanceIds(Widget::class.java) }
            if (ids.isEmpty()) return@launch // Only start chain if there are widgets

            val nextRequest = OneTimeWorkRequestBuilder<WidgetUpdateWorker>()
                .setInitialDelay(1, TimeUnit.MINUTES)
                .build()

            Timber.i("Queueing widget update")

            WorkManager.getInstance(context).enqueueUniqueWork(
                "TrafficLightWidgets",
                ExistingWorkPolicy.REPLACE,
                nextRequest
            )
        }
    }
}