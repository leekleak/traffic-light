package com.leekleak.trafficlight.widget

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Context.ALARM_SERVICE
import android.content.Intent
import androidx.glance.appwidget.updateAll
import com.leekleak.trafficlight.database.DataPlanDao
import com.leekleak.trafficlight.model.NetworkUsageManager
import com.leekleak.trafficlight.services.notifications.WarningNotificationHelper
import com.leekleak.trafficlight.ui.plans.DataPlanLogic
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import timber.log.Timber

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

class WidgetUpdateReceiver: BroadcastReceiver(), KoinComponent {
    private val applicationScope: CoroutineScope by inject()
    private val dataPlanDao: DataPlanDao by inject()
    private val dataPlanLogic: DataPlanLogic by inject()
    private val networkUsageManager: NetworkUsageManager by inject()

    override fun onReceive(context: Context, intent: Intent) {
        val pendingResult = goAsync()
        applicationScope.launch {
            try {
                Widget().updateAll(context)
                checkWarnings(context)
            } catch (e: Exception) {
                Timber.e(e)
            } finally {
                pendingResult.finish()
            }
        }
    }

    private suspend fun checkWarnings(context: Context) {
        val plans = dataPlanDao.getActivePlans()
        plans.forEach { plan ->
            plan.updateUsage(networkUsageManager)
            if (plan.budgetWarning) {
                val remainingBudget = dataPlanLogic.getRemainingDailyBudgetToday(plan)
                if (remainingBudget <= 0L && !plan.budgetOvershotNotified) {
                    WarningNotificationHelper.showBudgetWarning(context, plan)
                    dataPlanDao.add(plan.copy(budgetOvershotNotified = true))
                } else if (remainingBudget > 0L && plan.budgetOvershotNotified) {
                    dataPlanDao.add(plan.copy(budgetOvershotNotified = false))
                }
            }

            if (plan.safetyWarning) {
                val safetyState = dataPlanLogic.getDataSafety(plan)
                val stateInt = safetyState.ordinal
                if (plan.lastSafetyState != stateInt) {
                    WarningNotificationHelper.showSafetyWarning(context, plan, safetyState)
                    dataPlanDao.add(plan.copy(lastSafetyState = stateInt))
                }
            }
        }
    }
}
