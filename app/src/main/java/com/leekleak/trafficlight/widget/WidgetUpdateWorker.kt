package com.leekleak.trafficlight.widget

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Context.ALARM_SERVICE
import android.content.Intent
import androidx.glance.appwidget.updateAll
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

fun startAlarmManager(context: Context) {
    val alarmManager = context.getSystemService(ALARM_SERVICE) as AlarmManager
    val intent = Intent(context, WidgetUpdateReceiver::class.java)
    val pendingIntent = PendingIntent.getBroadcast(context, 0, intent,
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
    )

    alarmManager.setInexactRepeating(
        AlarmManager.RTC,
        System.currentTimeMillis(),
        1000 * 60 * 1,
        pendingIntent
    )
}

fun killAlarmManager(context: Context) {
    val alarmManager = context.getSystemService(ALARM_SERVICE) as AlarmManager
    val intent = Intent(context, WidgetUpdateReceiver::class.java)
    val pendingIntent = PendingIntent.getBroadcast(context, 0, intent,
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
    )
    alarmManager.cancel(pendingIntent)
}

class WidgetUpdateReceiver: BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        CoroutineScope(Dispatchers.IO).launch {
            Widget().updateAll(context)
        }
    }
}