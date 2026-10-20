package com.leekleak.trafficlight.integrations.iperf

import org.koin.dsl.module
import org.koin.plugin.module.dsl.single

val iPerfIntegrationModule = module {
    single<IPerf3Provider>()
}