package com.leekleak.trafficlight.services

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.leekleak.trafficlight.widget.startAlarmManager
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class AutoStarter : BroadcastReceiver(), KoinComponent {
    private val permissionManager: PermissionManager by inject()
    @OptIn(DelicateCoroutinesApi::class)
    override fun onReceive(context: Context, intent: Intent) {
        val pendingResult = goAsync()
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            GlobalScope.launch(Dispatchers.IO) {
                context.let {
                    permissionManager.update()
                    UsageService.startService(it)
                    startAlarmManager(context)
                }
                pendingResult.finish()
            }
        }
    }
}