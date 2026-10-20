package com.leekleak.trafficlight.integrations

import com.leekleak.play_integration.playModule
import com.leekleak.trafficlight.integrations.iperf.iPerfIntegrationModule
import org.koin.dsl.bind
import org.koin.dsl.module
import org.koin.plugin.module.dsl.single

val integrationsModule = module {
    includes(playModule, iPerfIntegrationModule)
    single<PlayServicesProviderImpl>() bind PlayServicesProvider::class
    single<ShizukuServicesProviderImpl>() bind ShizukuServicesProvider::class
}
