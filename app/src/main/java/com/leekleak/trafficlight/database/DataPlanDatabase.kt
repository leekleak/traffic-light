package com.leekleak.trafficlight.database

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import androidx.room.Update

enum class TimeInterval {
    DAY,
    WEEK,
    MONTH,
    YEAR
}

@Entity
data class DataPlan(
    @PrimaryKey val subscriberID: String,

    @ColumnInfo val setupComplete: Boolean,
    @ColumnInfo val data: Boolean,

    // Recurring data plan settings

    // If false, the plan is pre-paid.
    // Crucially the other settings are still valid as
    @ColumnInfo val recurring: Boolean,
    @ColumnInfo val startDate: Long, // LocalDate as timestamp
    @ColumnInfo val interval: TimeInterval,
    @ColumnInfo val intervalMultiplier: Int,

    @ColumnInfo val excludedApps: List<Int>, // List of excluded app UIDs
    @ColumnInfo val unlimitedDataPeriod: List<Int>, // List of 2 items. Start and end hours of the range in UTC

    /**
     * Updatables
     *
     * The following data should be updated whenever the lastUpdateDate is not within the current period.
     */

    @ColumnInfo val lastUpdateDate: Long, // LocalDate as timestamp

    // Can be both positive and negative.
    // If positive it implies something we don't know has used data. (Maybe it has been gifted to someone)
    // If negative it implies the user may be informing the app of rollover from the time before the app was installed.
    @ColumnInfo val periodUsageOffset: Int,

    @ColumnInfo val rollover: Boolean,
    @ColumnInfo val rolloverLeftover: Int,


    /**
     * Customization
     */
    @ColumnInfo val uiBackground: Int,
)

@Dao
interface DataPlanDao {
    @Query("SELECT * FROM dataplan")
    fun getAll(): List<DataPlan>

    @Query("SELECT * FROM dataplan WHERE subscriberID = :subscriberID")
    fun get(subscriberID: String): DataPlan?

    @Update
    fun update(dataPlan: DataPlan)
}

@Database(entities = [DataPlan::class], version = 1)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun dataPlanDao(): DataPlanDao
}

class Converters {
    @TypeConverter
    fun fromListInt(list: List<Int>): String {
        return list.joinToString(",")
    }

    @TypeConverter
    fun toListInt(data: String): List<Int> {
        return listOf(*data.split(",").map { it.toInt() }.toTypedArray())
    }
}