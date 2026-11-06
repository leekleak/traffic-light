package com.leekleak.trafficlight.widget

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Context.ALARM_SERVICE
import android.content.Intent
import androidx.glance.appwidget.updateAll
import com.leekleak.trafficlight.model.DataPlanManager
import com.leekleak.trafficlight.model.DataPlanRefresh
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
    private val dataPlanManager: DataPlanManager by inject()
    private val dataPlanLogic: DataPlanLogic by inject()

    override fun onReceive(context: Context, intent: Intent) {
        val pendingResult = goAsync()
        applicationScope.launch {
            try {
                val refreshedPlans = dataPlanManager.refresh()
                Widget().updateAll(context)
                checkWarnings(context, refreshedPlans)
            } catch (e: IllegalArgumentException) {
                Timber.e(e, "Failed to update all widgets: invalid ID or mismatch")
            } catch (e: IllegalStateException) {
                Timber.e(e, "Failed to update all widgets: invalid state")
            } catch (e: java.io.IOException) {
                Timber.e(e, "Failed to update all widgets: IO error")
            } finally {
                pendingResult.finish()
            }
        }
    }

    private suspend fun checkWarnings(context: Context, refreshedPlans: List<DataPlanRefresh>) {
        val updatedPlans = refreshedPlans.mapNotNull { refreshedPlan ->
            var currentPlan = refreshedPlan.plan

            if (currentPlan.budgetWarning) {
                val remainingBudget = dataPlanLogic.getRemainingDailyBudgetToday(currentPlan, refreshedPlan.snapshot)
                if (remainingBudget <= 0L && !currentPlan.budgetOvershotNotified) {
                    WarningNotificationHelper.showBudgetWarning(context, currentPlan)
                    currentPlan = currentPlan.copy(budgetOvershotNotified = true)
                } else if (remainingBudget > 0L && currentPlan.budgetOvershotNotified) {
                    currentPlan = currentPlan.copy(budgetOvershotNotified = false)
                }
            }

            if (currentPlan.safetyWarning) {
                val safetyState = dataPlanLogic.getDataSafety(currentPlan, refreshedPlan.snapshot)
                val stateInt = safetyState.ordinal
                if (currentPlan.lastSafetyState != stateInt) {
                    WarningNotificationHelper.showSafetyWarning(context, currentPlan, safetyState)
                    currentPlan = currentPlan.copy(lastSafetyState = stateInt)
                }
            }
            currentPlan.takeIf { it != refreshedPlan.plan }
        }
        if (updatedPlans.isNotEmpty()) {
            dataPlanManager.addPlans(updatedPlans)
        }
    }
}
