package com.leekleak.trafficlight.integrations

import com.leekleak.trafficlight.database.DataPlanRepository

interface ShizukuServicesProvider {
    fun updateSimData()
    fun shizukuRunning(): Boolean
    fun shizukuPermission(): Int
    fun shizukuRequestPermission()
    fun enable()
    fun disable()
}

suspend fun updateSimDataBasic(dataPlanRepository: DataPlanRepository) {
    val dataPlanDao = dataPlanRepository.dao
    val plans = dataPlanDao.getAll()
    val newPlans = plans.map { it.copy(simIndex = if (it.decryptedID == null) 0 else -1) }
    if (plans.isEmpty()) {
        dataPlanRepository.savePlan(
            null,
            0,
            ""
        )
    }
    dataPlanDao.addAll(newPlans)
}
