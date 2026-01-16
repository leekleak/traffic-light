package com.leekleak.trafficlight.services

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import kotlin.getValue

class AutoStarter : BroadcastReceiver(), KoinComponent
{
    val permissionManager: PermissionManager by inject()
    override fun onReceive(context: Context?, intent: Intent?) {
        if (intent?.action == Intent.ACTION_BOOT_COMPLETED) {
            context?.let {
                permissionManager.update()
                UsageService.startService(it)
            }
        }
    }
}