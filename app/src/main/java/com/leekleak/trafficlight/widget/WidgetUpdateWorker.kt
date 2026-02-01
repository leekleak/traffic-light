package com.leekleak.trafficlight.widget

import android.content.Context
import androidx.glance.appwidget.updateAll
import androidx.work.Worker
import androidx.work.WorkerParameters
import kotlinx.coroutines.runBlocking

class WidgetUpdateWorker(val context: Context, params: WorkerParameters) : Worker(context, params) {
    override fun doWork(): Result {
        runBlocking {
            Widget().updateAll(context)
        }
        return Result.success()
    }
}