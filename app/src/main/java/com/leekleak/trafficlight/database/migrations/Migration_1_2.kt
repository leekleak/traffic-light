package com.leekleak.trafficlight.database.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("""
            CREATE TABLE DataPlan_new (
                subscriberID TEXT PRIMARY KEY NOT NULL,
                simIndex INTEGER NOT NULL DEFAULT -1,
                carrierName TEXT NOT NULL DEFAULT '',
                dataMax INTEGER NOT NULL,
                recurring INTEGER NOT NULL,
                startDate INTEGER NOT NULL,
                interval TEXT NOT NULL,
                intervalMultiplier INTEGER NOT NULL DEFAULT 1,
                excludedApps TEXT NOT NULL,
                unlimitedDataPeriod TEXT,
                lastUpdateDate INTEGER,
                periodUsageOffset INTEGER,
                rollover INTEGER,
                rolloverLeftover INTEGER,
                uiBackground INTEGER NOT NULL
            )
        """.trimIndent())

        db.execSQL("""
            INSERT INTO DataPlan_new SELECT 
                subscriberID, -1, '', dataMax, recurring, startDate,
                interval, COALESCE(intervalMultiplier, 1), excludedApps, unlimitedDataPeriod,
                lastUpdateDate, periodUsageOffset, rollover, rolloverLeftover, uiBackground
            FROM DataPlan
        """.trimIndent())

        db.execSQL("DROP TABLE DataPlan")
        db.execSQL("ALTER TABLE DataPlan_new RENAME TO DataPlan")
    }
}