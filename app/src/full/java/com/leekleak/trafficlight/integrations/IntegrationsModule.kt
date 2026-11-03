package com.leekleak.trafficlight.integrations

import com.leekleak.iperf.di.iperfModule
import com.leekleak.trafficlight.iperf.iperfNavigationModule
import org.koin.dsl.bind
import org.koin.dsl.module
import org.koin.plugin.module.dsl.single

val integrationsModule = module {
    includes(iperfModule, iperfNavigationModule)
    single<ShizukuServicesProviderImpl>() bind ShizukuServicesProvider::class
}
