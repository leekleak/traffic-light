package com.leekleak.trafficlight.model

import org.koin.dsl.module

val preferenceRepoModule = module {
    single { PreferenceRepo(get(), get()) }
    single { AppDatabase(get()) }
    single { ShizukuDataManager(get(), get()) }
    factory { AppIconFetcher.Factory(get()) }
}