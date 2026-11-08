package com.leekleak.trafficlight.database.migrations

import androidx.room3.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL

val MIGRATION_4_5 = object : Migration(4, 5) {
    override suspend fun migrate(connection: SQLiteConnection) {
        connection.execSQL(
            "ALTER TABLE `DataPlan` ADD COLUMN `createdAt` INTEGER NOT NULL DEFAULT 0"
        )
    }
}
