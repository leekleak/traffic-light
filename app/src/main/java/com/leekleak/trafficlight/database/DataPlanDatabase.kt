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
import kotlinx.coroutines.flow.Flow
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime

enum class TimeInterval {
    DAY,
    MONTH
}

@Entity
data class DataPlan(
    @PrimaryKey
    val hashedSubscriberID: String,

    @ColumnInfo val encryptedSubscriberID: String,

    @ColumnInfo val simIndex: Int = -1,
    @ColumnInfo val carrierName: String = "",

    @ColumnInfo val dataMax: Long = 0,

    // Recurring data plan settings
    @ColumnInfo val startDate: Long = LocalDate.now().withDayOfMonth(1).atStartOfDay().toTimestamp(), // LocalDate as timestamp
    @ColumnInfo val interval: TimeInterval = TimeInterval.MONTH,
    @ColumnInfo val intervalMultiplier: Int = 1,

    @ColumnInfo val excludedApps: List<Int> = listOf(), // List of excluded app UIDs

    @ColumnInfo val notification: Boolean = false,
    @ColumnInfo val liveNotification: Boolean = false,

    /**
     * Customization
     */
    @ColumnInfo val uiBackground: Int = 0,
) {
    fun getDecryptedID(): String {
        return CryptoManager.decrypt(encryptedSubscriberID)
    }
}

@Dao
interface DataPlanDao {
    @Query("SELECT * FROM dataplan")
    suspend fun getAll(): List<DataPlan>?

    @Query("SELECT * FROM dataplan WHERE hashedSubscriberID = :hashedID")
    suspend fun getByHash(hashedID: String): DataPlan?

    @Query("SELECT * FROM dataplan WHERE simIndex != -1 ORDER BY simIndex ASC")
    fun getActivePlansFlow(): Flow<List<DataPlan>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun add(dataPlan: DataPlan)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addAll(plans: List<DataPlan>)
}

@Database(entities = [DataPlan::class], version = 3, exportSchema = true)
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
                startDate = startDate.plusDays(intervalMultiplier.toLong())
            }
            startDate
        }
    }
    val duration = Duration.between(now, startDate).toDays().toInt() + 1
    return context.resources.getQuantityString(R.plurals.resets_in_days, duration, duration)
}

class DataPlanRepository(private val dao: DataPlanDao) {
    suspend fun savePlan(plainSubscriberID: String, simIndex: Int, carrierName: String) {
        val plan = DataPlan(
            hashedSubscriberID = CryptoManager.hashIdentifier(plainSubscriberID),
            encryptedSubscriberID = CryptoManager.encrypt(plainSubscriberID),
            simIndex = simIndex,
            carrierName = carrierName
        )
        dao.add(plan)
    }

    suspend fun getPlan(plainSubscriberID: String): DataPlan? {
        val hashedID = CryptoManager.hashIdentifier(plainSubscriberID)
        return dao.getByHash(hashedID)
    }
}