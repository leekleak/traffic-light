package com.leekleak.trafficlight.services.notifications

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.leekleak.trafficlight.MainActivity
import com.leekleak.trafficlight.R
import com.leekleak.trafficlight.database.DataPlan
import com.leekleak.trafficlight.util.MiniCardState

object WarningNotificationHelper {
    private const val BUDGET_NOTIFICATION_ID_OFFSET = 1000
    private const val SAFETY_NOTIFICATION_ID_OFFSET = 2000

    fun showBudgetWarning(context: Context, plan: DataPlan) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(context, plan.simIndex, intent, PendingIntent.FLAG_IMMUTABLE)

        val simName = context.getString(R.string.sim_card) + " " + (plan.simIndex + 1)

        val notification = NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(R.drawable.warning)
            .setContentTitle(context.getString(R.string.budget_overshoot_notif_title))
            .setContentText(context.getString(R.string.budget_overshoot_notif_text, simName))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(BUDGET_NOTIFICATION_ID_OFFSET + plan.simIndex, notification)
    }

    fun showSafetyWarning(context: Context, plan: DataPlan, state: MiniCardState) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(context, plan.simIndex, intent, PendingIntent.FLAG_IMMUTABLE)

        val stateString = when (state) {
            MiniCardState.POSITIVE -> context.getString(R.string.safe)
            MiniCardState.NEUTRAL -> context.getString(R.string.neutral)
            MiniCardState.NEGATIVE -> context.getString(R.string.unsafe)
        }

        val simName = context.getString(R.string.sim_card) + " " + (plan.simIndex + 1)

        val notification = NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(R.drawable.shield)
            .setContentTitle(context.getString(R.string.safety_status_notif_title))
            .setContentText(context.getString(R.string.safety_status_notif_text, simName, stateString))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(SAFETY_NOTIFICATION_ID_OFFSET + plan.simIndex, notification)
    }

    const val NOTIFICATION_CHANNEL_ID = "PlanWarningNotification"
}
