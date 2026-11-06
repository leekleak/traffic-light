package com.leekleak.trafficlight.integrations

import android.content.pm.PackageManager.PERMISSION_GRANTED
import android.telephony.SubscriptionInfo
import com.leekleak.shizukuintegration.ShizukuHelper
import com.leekleak.trafficlight.BuildConfig
import com.leekleak.trafficlight.database.AppPreferenceRepo
import com.leekleak.trafficlight.model.DataPlanManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class ShizukuServicesProviderImpl(
    private val dataPlanManager: DataPlanManager,
    private val appPreferenceRepo: AppPreferenceRepo,
    private val scope: CoroutineScope,
): ShizukuServicesProvider {

    val shizukuHelper = ShizukuHelper(
        BuildConfig.APPLICATION_ID,
        BuildConfig.DEBUG,
        BuildConfig.VERSION_CODE
    ) {
        scope.launch { updateSimData() }
    }

    private fun getSubscriptionInfos(): List<SubscriptionInfo> = shizukuHelper.getSubscriptionInfos()

    private fun getSubscriberID(subscriptionId: Int): String? = shizukuHelper.getSubscriberID(subscriptionId)

    override suspend fun updateSimData() {
        if (appPreferenceRepo.shizukuTracking.first() && shizukuRunning() && shizukuPermission() == PERMISSION_GRANTED) {
            val infos = getSubscriptionInfos().sortedBy { it.simSlotIndex }
            val activeSubscriberIDs = infos.map { getSubscriberID(it.subscriptionId) }
            val plans = dataPlanManager.getPlans().map { plan ->
                plan.copy(simIndex = activeSubscriberIDs.indexOf(plan.decryptedID))
            }
            activeSubscriberIDs.forEachIndexed { index, activeID ->
                if (activeID !in plans.map { it.decryptedID } && activeID != null) {
                    dataPlanManager.createPlan(
                        plainSubscriberID = activeID,
                        simIndex = infos[index].simSlotIndex,
                        carrierName = infos[index].carrierName?.toString() ?: ""
                    )
                }
            }
            dataPlanManager.addPlans(plans)
        } else {
            updateSimDataBasic(dataPlanManager)
        }
    }

    override fun shizukuRunning(): Boolean = shizukuHelper.shizukuRunning()
    override fun shizukuPermission(): Int = shizukuHelper.shizukuPermission()
    override fun shizukuRequestPermission(): Unit = shizukuHelper.shizukuRequestPermission()

    override fun enable() {
        shizukuHelper.bind()
    }

    override fun disable() {
        shizukuHelper.unbind()
    }
}