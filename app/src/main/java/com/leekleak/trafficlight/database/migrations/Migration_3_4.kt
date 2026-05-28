package com.leekleak.trafficlight.database.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE DataPlan ADD COLUMN budgetWarning INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE DataPlan ADD COLUMN safetyWarning INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE DataPlan ADD COLUMN lastSafetyState INTEGER NOT NULL DEFAULT -1")
        db.execSQL("ALTER TABLE DataPlan ADD COLUMN budgetOvershotNotified INTEGER NOT NULL DEFAULT 0")
    }
}
