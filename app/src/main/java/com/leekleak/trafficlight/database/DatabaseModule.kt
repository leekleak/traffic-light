package com.leekleak.trafficlight.database

import androidx.room3.Room
import com.leekleak.trafficlight.database.migrations.MIGRATION_1_2
import com.leekleak.trafficlight.database.migrations.MIGRATION_2_3
import com.leekleak.trafficlight.database.migrations.MIGRATION_3_4
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module
import org.koin.plugin.module.dsl.single


val databaseModule = module {
    single<AppPreferenceRepo>()
    single<HistoryPreferenceRepo>()

    single<AppDatabase> {
        Room.databaseBuilder(
            androidContext(),
            AppDatabase::class.java,
            "database"
        )
            .addMigrations(
                MIGRATION_1_2,
                MIGRATION_2_3,
                MIGRATION_3_4,
            )
            .build()
    }
    single<DataPlanDao> { get<AppDatabase>().dataPlanDao() }
    single<DataPlanRepository>()

    single<IPerfEntryDatabase> {
        Room.databaseBuilder(
            androidContext(),
            IPerfEntryDatabase::class.java,
            "iperf_servers"
        )
            .addMigrations()
            .build()
    }
    single<IPerfEntryDao> { get<IPerfEntryDatabase>().iPerfEntryDao() }
}
