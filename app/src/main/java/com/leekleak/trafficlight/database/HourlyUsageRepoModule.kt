package com.leekleak.trafficlight.database

import androidx.room.Room
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val databaseModule = module {
    single { HourlyUsageRepo(get()) }

    single {
        Room.databaseBuilder(
            androidContext(),
            AppDatabase::class.java,
            "database"
        ).allowMainThreadQueries().build()
    }
    single { get<AppDatabase>().dataPlanDao() }
}
