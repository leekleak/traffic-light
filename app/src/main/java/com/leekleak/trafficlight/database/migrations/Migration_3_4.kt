package com.leekleak.trafficlight.database.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS `DataPlan_new` (
                `hashedSubscriberID` TEXT NOT NULL, 
                `encryptedSubscriberID` TEXT NOT NULL, 
                `simIndex` INTEGER NOT NULL, 
                `carrierName` TEXT NOT NULL, 
                `configured` INTEGER NOT NULL, 
                `startDate` INTEGER NOT NULL, 
                `interval` TEXT NOT NULL, 
                `intervalMultiplier` INTEGER NOT NULL, 
                `recurring` INTEGER NOT NULL, 
                `excludedApps` TEXT NOT NULL, 
                `notification` INTEGER NOT NULL, 
                `liveNotification` INTEGER NOT NULL, 
                `budgetWarning` INTEGER NOT NULL, 
                `safetyWarning` INTEGER NOT NULL, 
                `lastSafetyState` INTEGER NOT NULL, 
                `budgetOvershotNotified` INTEGER NOT NULL, 
                `mainDataSize` INTEGER NOT NULL, 
                `mainDataSizeUnit` TEXT NOT NULL, 
                `mainDataUsed` INTEGER NOT NULL, 
                `mainStartStamp` INTEGER NOT NULL, 
                `mainExpiryStamp` INTEGER NOT NULL, 
                `extras` TEXT NOT NULL, 
                `lastUpdateStamp` INTEGER NOT NULL, 
                `uiBackground` INTEGER NOT NULL, 
                `uiColor` INTEGER NOT NULL, 
                `note` TEXT NOT NULL, 
                PRIMARY KEY(`hashedSubscriberID`)
            )
        """.trimIndent())

        db.execSQL("""
            INSERT INTO `DataPlan_new` (
                `hashedSubscriberID`, `encryptedSubscriberID`, `simIndex`, `carrierName`, `configured`,
                `startDate`, `interval`, `intervalMultiplier`, `recurring`, `excludedApps`, 
                `notification`, `liveNotification`, `budgetWarning`, `safetyWarning`, 
                `lastSafetyState`, `budgetOvershotNotified`, 
                `mainDataSize`, `mainDataSizeUnit`, `mainDataUsed`, `mainStartStamp`, `mainExpiryStamp`,
                `extras`, `lastUpdateStamp`, `uiBackground`, `uiColor`, `note`
            )
            SELECT 
                `hashedSubscriberID`, `encryptedSubscriberID`, `simIndex`, `carrierName`, CASE WHEN `dataMax` > 0 THEN 1 ELSE 0 END, 
                `startDate`, `interval`, `intervalMultiplier`, 0, `excludedApps`, 
                `notification`, `liveNotification`, 0, 0, 
                -1, 0,
                `dataMax`, 'GB', 0, `startDate`, 9223372036854775807,
                '[]', 0, `uiBackground`, 0, ''
            FROM `DataPlan`
        """.trimIndent())

        db.execSQL("DROP TABLE `DataPlan`")

        db.execSQL("ALTER TABLE `DataPlan_new` RENAME TO `DataPlan`")
    }
}
