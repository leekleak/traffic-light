package com.leekleak.trafficlight.services

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.leekleak.trafficlight.model.PermissionManager
import com.leekleak.trafficlight.services.notifications.NotificationService
import com.leekleak.trafficlight.widget.WidgetReceiver.Companion.setForceUpdateWidgets
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
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED -> {
                val pendingResult = goAsync()
                applicationScope.launch {
                    try {
                        try {
                            permissionManager.update()
                        } catch (e: Exception) {
                            Timber.e(e, "Failed to update permissions")
                        }

                        try {
                            NotificationService.startService(context)
                        } catch (e: Exception) {
                            Timber.e(e, "Failed to start notification service")
                        }

                        try {
                            setForceUpdateWidgets(context)
                            startAlarmManager(context)
                        } catch (e: Exception) {
                            Timber.e(e, "Failed to start alarm manager service")
                        }
                    } finally {
                        pendingResult.finish()
                    }
                }
            }
        }
    }
}