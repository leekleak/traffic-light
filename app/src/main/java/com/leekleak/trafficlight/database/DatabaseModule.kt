package com.leekleak.trafficlight.database

import androidx.room.Room
import kotlinx.coroutines.runBlocking
import net.zetetic.database.sqlcipher.SupportOpenHelperFactory
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module


val databaseModule = module {
    single { AppPreferenceRepo(get()) }
    single { HistoryPreferenceRepo(get(), get()) }

    single {
        System.loadLibrary("sqlcipher")
        val password = runBlocking { CryptoManager.getOrCreateDbPassphrase(androidContext()) }
        Room.databaseBuilder(androidContext(), AppDatabase::class.java, "database_encrypted")
            .openHelperFactory(SupportOpenHelperFactory(password))
            .build()
    }
    single { get<AppDatabase>().dataPlanDao() }
}
