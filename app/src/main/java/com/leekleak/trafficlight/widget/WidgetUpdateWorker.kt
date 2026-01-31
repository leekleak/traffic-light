package com.leekleak.trafficlight.widget

import android.content.Context
import androidx.glance.appwidget.updateAll
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.Worker
import androidx.work.WorkerParameters
import kotlinx.coroutines.runBlocking
import timber.log.Timber
import java.util.concurrent.TimeUnit

class WidgetUpdateWorker(val context: Context, params: WorkerParameters) : Worker(context, params) {
    override fun doWork(): Result {
        runBlocking {
            Widget().updateAll(context)
        }

        Timber.e("Queueing next refresh")
        val nextRequest = OneTimeWorkRequestBuilder<WidgetUpdateWorker>()
            .setInitialDelay(1, TimeUnit.MINUTES)
            .build()

        WorkManager.getInstance(context).enqueue(nextRequest)

        return Result.success()
    }
}