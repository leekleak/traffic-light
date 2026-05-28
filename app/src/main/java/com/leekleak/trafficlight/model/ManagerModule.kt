package com.leekleak.trafficlight.model

import coil3.ImageLoader
import coil3.request.crossfade
import com.leekleak.trafficlight.ui.plans.DataPlanLogic
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val managerModule = module {
    single { AppManager(get(), get()) }
    single { AppIconFetcher.Factory(get()) }
    single {
        ImageLoader.Builder(get())
            .components {
                add(get<AppIconFetcher.Factory>())
            }
            .crossfade(true)
            .build()
    }

    single { PermissionManager(androidContext(), get(), get(), get(), get()) }
    single { NetworkUsageManager(get(), get()) }
    single { DataPlanLogic(get()) }
}
