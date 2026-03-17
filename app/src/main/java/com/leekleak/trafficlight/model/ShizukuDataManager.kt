package com.leekleak.trafficlight.model

import android.content.ComponentName
import android.content.ServiceConnection
import android.os.IBinder
import android.telephony.SubscriptionInfo
import com.leekleak.trafficlight.BuildConfig
import com.leekleak.trafficlight.ITrafficLightShizukuService
import com.leekleak.trafficlight.database.DataPlan
import com.leekleak.trafficlight.database.DataPlanDao
import com.leekleak.trafficlight.database.PreferenceRepo
import com.leekleak.trafficlight.services.TrafficLightShizukuService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import rikka.shizuku.Shizuku
import timber.log.Timber


class ShizukuDataManager(
    private val dataPlanDao: DataPlanDao,
    private val preferenceRepo: PreferenceRepo,
    private val permissionManager: PermissionManager,
    private val scope: CoroutineScope,
) {
    private var binderMine: ITrafficLightShizukuService? = null

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(componentName: ComponentName, binder: IBinder?) {
            if (binder != null && binder.pingBinder()) {
                binderMine = ITrafficLightShizukuService.Stub.asInterface(binder)
                updateSimData()
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

    init {
        scope.launch {
            combine(
                preferenceRepo.shizukuTracking,
                permissionManager.shizukuPermissionFlow,
                permissionManager.shizukuRunningFlow
            ) { setting, permission, running ->
                return@combine Triple(setting, permission, running)
            }.collectLatest {
                val setting = it.first
                val permission = it.second
                val running = it.third
                if (setting && permission && running) {
                    Shizuku.bindUserService(serviceArgs, connection)
                } else if (!setting) {
                    if (binderMine != null) {
                        Shizuku.unbindUserService(serviceArgs, connection, true)
                    }
                    updateSimDataBasic()
                }
            }
        }
    }

    private fun getSubscriptionInfos(): List<SubscriptionInfo> {
        try {
            return binderMine?.subscriptionInfos ?: emptyList()
        } catch (e: Exception) {
            Timber.w(e)
            return emptyList()
        }
    }

    private fun getSubscriberID(subscriptionId: Int): String? {
        try {
            return binderMine?.getSubscriberID(subscriptionId)
        } catch (e: Exception) {
            Timber.w(e)
            return null
        }
    }

    fun updateSimData() = scope.launch {
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
    }

    fun updateSimDataBasic() = scope.launch {
        val plans = dataPlanDao.getAll().toMutableList()
        if (plans.count { it.subscriberID == "null" } == 0) {
            plans.add(
                DataPlan(
                    subscriberID = "null",
                    simIndex = 0
                )
            )
        }
        dataPlanDao.addAll(
            plans.map {
                it.copy(
                    simIndex = if (it.subscriberID == "null") 0 else -1
                )
            }
        )
    }
}