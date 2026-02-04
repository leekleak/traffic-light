package com.leekleak.trafficlight.database

import android.content.Context
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
import com.leekleak.trafficlight.R
import com.leekleak.trafficlight.util.fromTimestamp
import com.leekleak.trafficlight.util.toTimestamp
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime

enum class TimeInterval {
    DAY,
    WEEK,
    MONTH,
    YEAR
}

@Entity
data class DataPlan(
    @PrimaryKey val subscriberID: String,

    @ColumnInfo val dataMax: Long = 0,

    // Recurring data plan settings

    // If false, the plan is pre-paid.
    // Crucially the other settings are still valid as
    @ColumnInfo val recurring: Boolean = true,
    @ColumnInfo val startDate: Long = LocalDate.now().withDayOfMonth(1).atStartOfDay().toTimestamp(), // LocalDate as timestamp
    @ColumnInfo val interval: TimeInterval = TimeInterval.MONTH,
    @ColumnInfo val intervalMultiplier: Int? = null,

    @ColumnInfo val excludedApps: List<Int> = listOf(), // List of excluded app UIDs
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
        if (data == "") return listOf()
        return listOf(*data.split(",").map { it.toInt() }.toTypedArray())
    }
}

fun DataPlan.resetString(context: Context): String {
    val now = LocalDateTime.now()
    val startDate = when (interval) {
        TimeInterval.MONTH -> {
            var startDate = fromTimestamp(startDate).toLocalDate().atStartOfDay()
            while (startDate <= now) {
                startDate = startDate.plusMonths(1)
            }
            startDate
        }
        TimeInterval.DAY -> {
            var startDate = fromTimestamp(startDate).toLocalDate().atStartOfDay()
            while (startDate <= now) {
                startDate = startDate.plusDays(intervalMultiplier?.toLong() ?: 1)
            }
            startDate
        }
        else -> throw Exception("Unsupported time interval")
    }
    val duration = Duration.between(now, startDate).toDays().toInt() + 1
    return context.resources.getQuantityString(R.plurals.resets_in_days, duration, duration)
}