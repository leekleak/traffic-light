package com.leekleak.trafficlight.model

import android.content.ComponentName
import android.content.ServiceConnection
import android.os.IBinder
import android.telephony.SubscriptionInfo
import com.leekleak.trafficlight.BuildConfig
import com.leekleak.trafficlight.ITrafficLightShizukuService
import com.leekleak.trafficlight.services.PermissionManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import rikka.shizuku.Shizuku


class ShizukuDataManager : KoinComponent {
    val preferenceRepo: PreferenceRepo by inject()
    val permissionManager: PermissionManager by inject()
    var enabled = false

    private var binderMine: ITrafficLightShizukuService? = null

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(componentName: ComponentName, binder: IBinder?) {
            if (binder != null && binder.pingBinder()) {
                binderMine = ITrafficLightShizukuService.Stub.asInterface(binder)
            }
        }

        override fun onServiceDisconnected(componentName: ComponentName) {
            binderMine = null
        }
    }

    private val serviceArgs = Shizuku.UserServiceArgs(
        ComponentName(BuildConfig.APPLICATION_ID, TrafficLightShizukuService::class.java.name))
        .processNameSuffix("traffic_light_shizuku_service")
        .debuggable(BuildConfig.DEBUG)
        .version(BuildConfig.VERSION_CODE)

    init {
        CoroutineScope(Dispatchers.IO).launch {
            permissionManager.shizukuPermissionFlow.collect {
                enabled = it
                if (enabled) {
                    Shizuku.unbindUserService(serviceArgs, connection, true) // Force update service
                    Shizuku.bindUserService(serviceArgs, connection)
                }
            }
        }
    }

    suspend fun getSubscriptionInfos(): List<SubscriptionInfo> {
        if (!enabled) return emptyList()
        while (binderMine == null) delay(10)
        return binderMine!!.subscriptionInfos
    }

    suspend fun getSubscriberID(subscriptionId: Int): String? {
        if (!enabled) return null
        while (binderMine == null) delay(10)
        return binderMine!!.getSubscriberID(subscriptionId)
    }
}