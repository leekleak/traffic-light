package com.leekleak.trafficlight.database.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.leekleak.trafficlight.database.CryptoManager
import com.leekleak.trafficlight.model.NetworkUsageManager.Companion.NULL_SUBSCRIBER

val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS `DataPlan_new` (
                `hashedSubscriberID` TEXT NOT NULL, 
                `encryptedSubscriberID` TEXT NOT NULL, 
                `simIndex` INTEGER NOT NULL, 
                `carrierName` TEXT NOT NULL, 
                `dataMax` INTEGER NOT NULL, 
                `startDate` INTEGER NOT NULL, 
                `interval` TEXT NOT NULL, 
                `intervalMultiplier` INTEGER NOT NULL, 
                `excludedApps` TEXT NOT NULL, 
                `notification` INTEGER NOT NULL DEFAULT 0, 
                `liveNotification` INTEGER NOT NULL DEFAULT 0, 
                `uiBackground` INTEGER NOT NULL, 
                PRIMARY KEY(`hashedSubscriberID`)
            )
        """.trimIndent())
        val cursor = db.query("SELECT * FROM DataPlan")

        if (cursor.moveToFirst()) {
            do {
                val oldSubscriberID = cursor.getString(cursor.getColumnIndexOrThrow("subscriberID"))
                val finalID = if (oldSubscriberID == "null") NULL_SUBSCRIBER else oldSubscriberID
                val simIndex = cursor.getInt(cursor.getColumnIndexOrThrow("simIndex"))
                val carrierName = cursor.getString(cursor.getColumnIndexOrThrow("carrierName"))
                val dataMax = cursor.getLong(cursor.getColumnIndexOrThrow("dataMax"))
                val startDate = cursor.getLong(cursor.getColumnIndexOrThrow("startDate"))
                val interval = cursor.getString(cursor.getColumnIndexOrThrow("interval"))
                val intervalMultiplier = cursor.getInt(cursor.getColumnIndexOrThrow("intervalMultiplier"))
                val excludedApps = cursor.getString(cursor.getColumnIndexOrThrow("excludedApps"))
                val uiBackground = cursor.getInt(cursor.getColumnIndexOrThrow("uiBackground"))
                
                val hashedID = CryptoManager.hashIdentifier(finalID)
                val encryptedID = CryptoManager.encrypt(finalID)

                db.execSQL(
                    """
                    INSERT INTO DataPlan_new (
                        hashedSubscriberID, encryptedSubscriberID, simIndex, carrierName, 
                        dataMax, startDate, interval, intervalMultiplier, 
                        excludedApps, notification, liveNotification, uiBackground
                    ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, 0, 0, ?)
                    """.trimIndent(),
                    arrayOf(
                        hashedID, encryptedID, simIndex, carrierName,
                        dataMax, startDate, interval, intervalMultiplier,
                        excludedApps, uiBackground
                    )
                )
            } while (cursor.moveToNext())
        }
        cursor.close()
        db.execSQL("DROP TABLE DataPlan")
        db.execSQL("ALTER TABLE DataPlan_new RENAME TO DataPlan")
    }
}