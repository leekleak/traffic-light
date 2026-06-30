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
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            val pendingResult = goAsync()
            applicationScope.launch {
                try {
                    permissionManager.update()
                    NotificationService.startService(context, this)
                    startAlarmManager(context)
                } catch (e: SecurityException) {
                    Timber.e(e, "Failed to start service or alarm")
                } catch (e: IllegalStateException) {
                    Timber.e(e, "Background execution limits prevented service start")
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }
}