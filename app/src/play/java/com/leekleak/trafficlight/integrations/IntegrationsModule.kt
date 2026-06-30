package com.leekleak.trafficlight.integrations

import android.content.pm.PackageManager
import com.leekleak.play_integration.playModule
import com.leekleak.trafficlight.database.DataPlanRepository
import org.koin.dsl.module

val integrationsModule = module {
    includes(playModule)
    single<PlayServicesProvider> { PlayServicesProviderImpl(get(), get(), get(), get()) }
    single<ShizukuServicesProvider> {
        val dataPlanRepository: DataPlanRepository = get()
        object : ShizukuServicesProvider {
            override suspend fun updateSimData() {
                updateSimDataBasic(dataPlanRepository)
            }
            override fun shizukuRunning(): Boolean = false
            override fun shizukuPermission(): Int = PackageManager.PERMISSION_DENIED
            override fun shizukuRequestPermission() = Unit
            override fun enable() = Unit
            override fun disable() = Unit
        }
    }
}