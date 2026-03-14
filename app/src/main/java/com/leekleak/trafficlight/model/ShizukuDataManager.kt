package com.leekleak.trafficlight.model

import android.content.ComponentName
import android.content.ServiceConnection
import android.os.IBinder
import android.telephony.SubscriptionInfo
import com.leekleak.trafficlight.BuildConfig
import com.leekleak.trafficlight.ITrafficLightShizukuService
import com.leekleak.trafficlight.database.DataPlan
import com.leekleak.trafficlight.database.DataPlanDao
import com.leekleak.trafficlight.database.HourlyUsageRepo.Companion.NULL_SUBSCRIBER
import kotlinx.coroutines.delay
import rikka.shizuku.Shizuku


class ShizukuDataManager(
    private val dataPlanDao: DataPlanDao
) {
    private var enabled = false
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
        ComponentName(BuildConfig.APPLICATION_ID, TrafficLightShizukuService::class.java.name)
    )
        .processNameSuffix("traffic_light_shizuku_service")
        .debuggable(BuildConfig.DEBUG)
        .version(BuildConfig.VERSION_CODE)

    fun setEnabled(value: Boolean) {
        if (enabled == value) return
        if (value) {
            Shizuku.unbindUserService(serviceArgs, connection, true) // Force update service
            Shizuku.bindUserService(serviceArgs, connection)
        }
        enabled = value
    }

    private fun getSubscriptionInfos(): List<SubscriptionInfo> {
        if (!enabled) return emptyList()
        return binderMine?.subscriptionInfos ?: emptyList()
    }

    private fun getSubscriberID(subscriptionId: Int): String? {
        if (!enabled) return null
        return binderMine?.getSubscriberID(subscriptionId)
    }

    suspend fun updateSimData() {
        if (enabled) {
            while (binderMine == null) delay(10)
            val infos = getSubscriptionInfos().sortedBy { it.simSlotIndex }
            val activeSubscriberIDs = infos.map { getSubscriberID(it.subscriptionId) }
            var plans = dataPlanDao.getAll()
            plans = plans.map { plan ->
                plan.copy(simIndex = activeSubscriberIDs.indexOf(plan.subscriberID))
            }.toMutableList()
            activeSubscriberIDs.forEachIndexed { index, activeID ->
                if (activeID !in plans.map { it.subscriberID } && activeID != null) {
                    plans.add(
                        DataPlan(
                            subscriberID = activeID,
                            simIndex = infos[index].simSlotIndex,
                            carrierName = infos[index].carrierName?.toString() ?: ""
                        )
                    )
                }
            }
            dataPlanDao.addAll(plans)
        } else if (dataPlanDao.getActive().isEmpty()) {
            dataPlanDao.add(
                DataPlan(
                    subscriberID = NULL_SUBSCRIBER,
                    simIndex = 0
                )
            )
        }
    }
}