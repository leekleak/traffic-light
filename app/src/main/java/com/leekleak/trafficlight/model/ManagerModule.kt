package com.leekleak.trafficlight.model

import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val managerModule = module {
    single { AppManager(get()) }
    single { ShizukuDataManager(get()) }
    single { PermissionManager(androidContext(), get(), get()) }
    single { NetworkUsageManager(get(), get(), get(), get()) }
    factory { AppIconFetcher.Factory(get()) }
}