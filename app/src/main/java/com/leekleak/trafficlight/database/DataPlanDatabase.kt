package com.leekleak.trafficlight.database

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters

enum class TimeInterval {
    DAY,
    WEEK,
    MONTH,
    YEAR
}

@Entity
data class DataPlan(
    @PrimaryKey val subscriberID: String,

    @ColumnInfo val data: Long = 0,

    // Recurring data plan settings

    // If false, the plan is pre-paid.
    // Crucially the other settings are still valid as
    @ColumnInfo val recurring: Boolean = false,
    @ColumnInfo val startDate: Long? = null, // LocalDate as timestamp
    @ColumnInfo val interval: TimeInterval? = null,
    @ColumnInfo val intervalMultiplier: Int? = null,

    @ColumnInfo val excludedApps: List<Int>? = null, // List of excluded app UIDs
    @ColumnInfo val unlimitedDataPeriod: List<Int>? = null, // List of 2 items. Start and end hours of the range in UTC

    /**
     * Updatables
     *
     * The following data should be updated whenever the lastUpdateDate is not within the current period.
     */

    @ColumnInfo val lastUpdateDate: Long? = null, // LocalDate as timestamp

    // Can be both positive and negative.
    // If positive it implies something we don't know has used data. (Maybe it has been gifted to someone)
    // If negative it implies the user may be informing the app of rollover from the time before the app was installed.
    @ColumnInfo val periodUsageOffset: Int? = null,

    @ColumnInfo val rollover: Boolean? = null,
    @ColumnInfo val rolloverLeftover: Int? = null,


    /**
     * Customization
     */
    @ColumnInfo val uiBackground: Int = 0,
)

@Dao
interface DataPlanDao {
    @Query("SELECT * FROM dataplan")
    fun getAll(): List<DataPlan>

    @Query("SELECT * FROM dataplan WHERE subscriberID = :subscriberID")
    fun get(subscriberID: String): DataPlan?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun add(dataPlan: DataPlan)
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