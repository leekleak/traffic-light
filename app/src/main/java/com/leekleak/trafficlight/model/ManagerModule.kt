package com.leekleak.trafficlight.model

import coil3.ImageLoader
import coil3.request.crossfade
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val managerModule = module {
    single(createdAtStart = true) { AppManager(get()) }
    single { AppIconFetcher.Factory(get()) }
    single {
        ImageLoader.Builder(get())
            .components {
                add(get<AppIconFetcher.Factory>())
            }
            .crossfade(true)
            .build()
    }

    single(createdAtStart = true) { ShizukuDataManager(get(), get(), get(), get(), get()) }
    single { PermissionManager(androidContext()) }
    single { NetworkUsageManager(get(), get()) }
}