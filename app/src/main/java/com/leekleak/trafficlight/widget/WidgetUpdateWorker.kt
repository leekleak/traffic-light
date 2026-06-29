package com.leekleak.trafficlight.widget

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Context.ALARM_SERVICE
import android.content.Intent
import android.os.Build
import android.os.VibrationEffect
import android.os.VibratorManager
import android.widget.Toast
import androidx.glance.appwidget.updateAll
import com.leekleak.trafficlight.BuildConfig
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
        if (BuildConfig.DEBUG) {
            Toast.makeText(context, "Updating data plans", Toast.LENGTH_LONG).show()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager =
                    context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                val vibrator = vibratorManager?.defaultVibrator
                vibrator?.vibrate(VibrationEffect.createOneShot(100, VibrationEffect.DEFAULT_AMPLITUDE))
            }
        }
        val updatedPlans = plans.map { plan ->
            var currentPlan = plan
            currentPlan.updateUsage(networkUsageManager)
            if (currentPlan.budgetWarning) {
                val remainingBudget = dataPlanLogic.getRemainingDailyBudgetToday(currentPlan)
                if (remainingBudget <= 0L && !currentPlan.budgetOvershotNotified) {
                    WarningNotificationHelper.showBudgetWarning(context, currentPlan)
                    currentPlan = currentPlan.copy(budgetOvershotNotified = true)
                } else if (remainingBudget > 0L && currentPlan.budgetOvershotNotified) {
                    currentPlan = currentPlan.copy(budgetOvershotNotified = false)
                }
            }

            if (currentPlan.safetyWarning) {
                val safetyState = dataPlanLogic.getDataSafety(currentPlan)
                val stateInt = safetyState.ordinal
                if (currentPlan.lastSafetyState != stateInt) {
                    WarningNotificationHelper.showSafetyWarning(context, currentPlan, safetyState)
                    currentPlan = currentPlan.copy(lastSafetyState = stateInt)
                }
            }
            currentPlan
        }
        dataPlanDao.addAll(updatedPlans)
    }
}
