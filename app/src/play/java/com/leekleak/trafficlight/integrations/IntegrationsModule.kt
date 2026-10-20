package com.leekleak.trafficlight.integrations

import com.leekleak.iperfintegration.iPerfIntegrationModule
import com.leekleak.play_integration.playModule
import org.koin.dsl.bind
import org.koin.dsl.module
import org.koin.plugin.module.dsl.single

val integrationsModule = module {
    includes(playModule, iPerfIntegrationModule)
    single<PlayServicesProviderImpl>() bind PlayServicesProvider::class
    single<ShizukuServicesProviderImpl>() bind ShizukuServicesProvider::class
}
