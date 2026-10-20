package com.leekleak.trafficlight.integrations

import android.content.pm.PackageManager
import com.leekleak.trafficlight.database.DataPlanRepository

class ShizukuServicesProviderImpl(val dataPlanRepository: DataPlanRepository): ShizukuServicesProvider {
    override suspend fun updateSimData() { updateSimDataBasic(dataPlanRepository) }
    override fun shizukuRunning(): Boolean = false
    override fun shizukuPermission(): Int = PackageManager.PERMISSION_DENIED
    override fun shizukuRequestPermission() = Unit
    override fun enable() = Unit
    override fun disable() = Unit
}
