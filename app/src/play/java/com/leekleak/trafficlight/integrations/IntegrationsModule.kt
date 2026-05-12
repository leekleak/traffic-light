package com.leekleak.trafficlight.integrations

import com.leekleak.play_integration.playModule
import org.koin.dsl.module

val integrationsModule = module {
    includes(playModule)
    single<PlayServicesProvider> { PlayServicesProviderImpl() }
}