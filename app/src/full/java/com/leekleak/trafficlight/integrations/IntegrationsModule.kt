package com.leekleak.trafficlight.integrations

import com.leekleak.iperfintegration.iPerfIntegrationModule
import org.koin.dsl.bind
import org.koin.dsl.module
import org.koin.plugin.module.dsl.single

val integrationsModule = module {
    includes(iPerfIntegrationModule)
    single<ShizukuServicesProviderImpl>() bind ShizukuServicesProvider::class
}
