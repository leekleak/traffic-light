package com.leekleak.trafficlight.model

import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val managerModule = module {
    single { AppManager(get()) }
    single(createdAtStart = true) { ShizukuDataManager(get(), get(), get(), get()) }
    single { PermissionManager(androidContext()) }
    single { NetworkUsageManager(get(), get(), get()) }
    factory { AppIconFetcher.Factory(get()) }
}