package com.leekleak.trafficlight.widget

import android.content.Context
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.updateAll
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import kotlinx.coroutines.runBlocking
import timber.log.Timber
import java.util.concurrent.TimeUnit

class WidgetUpdateWorker(val context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        Widget().updateAll(context)
        enqueue(context, ExistingWorkPolicy.APPEND_OR_REPLACE)
        return Result.success()
    }

    companion object {
        fun enqueue(context: Context, policy: ExistingWorkPolicy = ExistingWorkPolicy.KEEP) {
            val manager = GlanceAppWidgetManager(context)
            val ids = runBlocking { manager.getGlanceIds(Widget::class.java) }
            if (ids.isEmpty()) return // Only start chain if there are widgets

            val nextRequest = OneTimeWorkRequestBuilder<WidgetUpdateWorker>()
                .setInitialDelay(1, TimeUnit.MINUTES)
                .build()

            Timber.i("Queueing widget update")

            WorkManager.getInstance(context).enqueueUniqueWork(
                "TrafficLightWidgets",
                policy,
                nextRequest
            )
        }
    }
}