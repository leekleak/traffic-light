package com.leekleak.trafficlight.integrations

import com.leekleak.trafficlight.model.DataPlanManager

interface ShizukuServicesProvider {
    suspend fun updateSimData()
    fun shizukuRunning(): Boolean
    fun shizukuPermission(): Int
    fun shizukuRequestPermission()
    fun enable()
    fun disable()
}

suspend fun updateSimDataBasic(dataPlanManager: DataPlanManager) {
    val plans = dataPlanManager.getPlans()
    val newPlans = plans.map { it.copy(simIndex = if (it.decryptedID == null) 0 else -1) }
    if (plans.count { it.simIndex >= 0 } == 0) {
        dataPlanManager.createPlan(
            plainSubscriberID = null,
            simIndex = 0,
            carrierName = ""
        )
    }
    dataPlanManager.addPlans(newPlans)
}
