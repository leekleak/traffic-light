package com.leekleak.trafficlight.integrations

import android.app.Activity
import org.koin.dsl.module

val integrationsModule = module {
    single<PlayServicesProvider> {
        object : PlayServicesProvider {
            override suspend fun onAppLaunch(activity: Activity) = Unit
        }
    }
}