package com.leekleak.trafficlight.model

import org.koin.dsl.module

val preferenceRepoModule = module {
    single { PreferenceRepo(get()) }
    single { AppDatabase(get()) }
    single { ShizukuDataManager() }
    factory { AppIconFetcher.Factory(get()) }
}