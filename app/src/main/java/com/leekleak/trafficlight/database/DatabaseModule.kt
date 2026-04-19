package com.leekleak.trafficlight.database

import androidx.room.Room
import com.leekleak.trafficlight.database.migrations.MIGRATION_1_2
import com.leekleak.trafficlight.database.migrations.MIGRATION_2_3
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val databaseModule = module {
    single { AppPreferenceRepo(get(), get()) }
    single { HistoryPreferenceRepo(get(), get()) }

    single {
        Room.databaseBuilder(
            androidContext(),
            AppDatabase::class.java,
            "database"
        )
            .addMigrations(
                MIGRATION_1_2,
                MIGRATION_2_3
            )
            .build()
    }
    single { get<AppDatabase>().dataPlanDao() }

    single {
        Room.databaseBuilder(
            androidContext(),
            HistoricalDataCache::class.java,
            "historicalCache"
        ).build()
    }
    single { get<HistoricalDataCache>().historicalDataDao() }
}
