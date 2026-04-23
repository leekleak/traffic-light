package com.leekleak.trafficlight.database.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `DataPlan_new` (
                `subscriberID` TEXT NOT NULL, 
                `simIndex` INTEGER NOT NULL, 
                `carrierName` TEXT NOT NULL, 
                `dataMax` INTEGER NOT NULL, 
                `recurring` INTEGER NOT NULL, 
                `startDate` INTEGER NOT NULL, 
                `interval` TEXT NOT NULL, 
                `intervalMultiplier` INTEGER NOT NULL, 
                `excludedApps` TEXT NOT NULL, 
                `unlimitedDataPeriod` TEXT, 
                `notification` INTEGER NOT NULL DEFAULT 0, 
                `liveNotification` INTEGER NOT NULL DEFAULT 0, 
                `uiBackground` INTEGER NOT NULL, 
                PRIMARY KEY(`subscriberID`)
            )
        """.trimIndent()
        )

        db.execSQL(
            """
            INSERT INTO `DataPlan_new` (
                `subscriberID`, `simIndex`, `carrierName`, `dataMax`, `recurring`, 
                `startDate`, `interval`, `intervalMultiplier`, `excludedApps`, 
                `unlimitedDataPeriod`, `notification`, `liveNotification`, `uiBackground`
            )
            SELECT 
                `subscriberID`, `simIndex`, `carrierName`, `dataMax`, `recurring`, 
                `startDate`, `interval`, `intervalMultiplier`, `excludedApps`, 
                `unlimitedDataPeriod`, 0, 0, `uiBackground`
            FROM `DataPlan`
        """.trimIndent()
        )

        db.execSQL("DROP TABLE `DataPlan`")
        db.execSQL("ALTER TABLE `DataPlan_new` RENAME TO `DataPlan`")
    }
}