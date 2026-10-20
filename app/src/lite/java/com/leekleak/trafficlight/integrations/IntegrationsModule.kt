package com.leekleak.trafficlight.integrations

import org.koin.dsl.bind
import org.koin.dsl.module
import org.koin.plugin.module.dsl.single

val integrationsModule = module {
    single<ShizukuServicesProviderImpl>() bind ShizukuServicesProvider::class
}
