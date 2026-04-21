package com.leekleak.trafficlight.database

import android.content.Context
import androidx.room.Room
import com.leekleak.trafficlight.database.migrations.MIGRATION_1_2
import com.leekleak.trafficlight.database.migrations.MIGRATION_2_3
import kotlinx.coroutines.runBlocking
import net.zetetic.database.sqlcipher.SQLiteDatabase
import net.zetetic.database.sqlcipher.SupportOpenHelperFactory
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module
import java.io.File

val databaseModule = module {
    single { AppPreferenceRepo(get(), get()) }
    single { HistoryPreferenceRepo(get(), get()) }

    single {
        System.loadLibrary("sqlcipher")
        val context = androidContext()
        val password = runBlocking { CryptoManager.getOrCreateDbPassphrase(context) }

        val dbName = "database"
        val dbFile = context.getDatabasePath(dbName)

        if (dbFile.exists()) {
            migrateIfUnencrypted(dbFile, context, dbName, password)
        }
        Room.databaseBuilder(context, AppDatabase::class.java, dbName)
            .addMigrations(
                MIGRATION_1_2,
                MIGRATION_2_3
            )
            .openHelperFactory(SupportOpenHelperFactory(password))
            .build()
    }
    single { get<AppDatabase>().dataPlanDao() }
}

private fun migrateIfUnencrypted(
    dbFile: File,
    context: Context,
    dbName: String,
    password: ByteArray
) {
    val isUnencrypted = try {
        android.database.sqlite.SQLiteDatabase.openDatabase(
            dbFile.path,
            null,
            android.database.sqlite.SQLiteDatabase.OPEN_READONLY
        ).use {
            it.version
        }
        true
    } catch (_: Exception) {
        false
    }

    if (isUnencrypted) {
        val tempDbFile = context.getDatabasePath("$dbName.temp")
        val database = SQLiteDatabase.openOrCreateDatabase(dbFile, null)

        database.rawExecSQL(
            "ATTACH DATABASE '${tempDbFile.path}' AS encrypted KEY '${
                String(
                    password
                )
            }';"
        )
        database.rawExecSQL("SELECT sqlcipher_export('encrypted');")
        database.rawExecSQL("DETACH DATABASE encrypted;")
        database.close()

        dbFile.delete()
        tempDbFile.renameTo(dbFile)
    }
}
