package com.leekleak.trafficlight.services

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.leekleak.trafficlight.model.PermissionManager
import com.leekleak.trafficlight.services.notifications.NotificationService
import com.leekleak.trafficlight.widget.startAlarmManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import timber.log.Timber

class AutoStarter : BroadcastReceiver(), KoinComponent {
    private val permissionManager: PermissionManager by inject()
    private val applicationScope: CoroutineScope by inject()

    override fun onReceive(context: Context, intent: Intent) {
        val pendingResult = goAsync()
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            applicationScope.launch {
                try {
                    permissionManager.update()
                    NotificationService.startService(context, this)
                    startAlarmManager(context)
                } catch (e: Exception) {
                    Timber.e(e)
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }
}