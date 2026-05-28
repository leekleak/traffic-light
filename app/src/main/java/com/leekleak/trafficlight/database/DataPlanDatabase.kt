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
import com.leekleak.trafficlight.database.DataPlan.Companion.NULL_SUBSCRIBER
import com.leekleak.trafficlight.util.fromTimestamp
import com.leekleak.trafficlight.util.toTimestamp
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.Serializable
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime

enum class TimeInterval {
    DAY,
    MONTH
}

@Serializable
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

    @ColumnInfo val budgetWarning: Boolean = false,
    @ColumnInfo val safetyWarning: Boolean = false,

    @ColumnInfo val lastSafetyState: Int = -1,
    @ColumnInfo val budgetOvershotNotified: Boolean = false,

    /**
     * Customization
     */
    @ColumnInfo val uiBackground: Int = 0,
) {
    init {
        require(intervalMultiplier > 0) {
            "intervalMultiplier must be positive, got $intervalMultiplier"
        }
    }
    val decryptedID: String?
        get() {
            val decrypted = CryptoManager.decrypt(encryptedSubscriberID)
            return if (decrypted == NULL_SUBSCRIBER) null else decrypted
        }

    fun getRemainingDays(): Int {
        val now = LocalDateTime.now()
        val startDate = getStartDate(true)
        return Duration.between(now, startDate).toDays().toInt() + 1
    }

    fun resetString(context: Context): String {
        val remaining = getRemainingDays()
        return context.resources.getQuantityString(R.plurals.resets_in_days, remaining, remaining)
    }

    fun getStartDate(next: Boolean = false): LocalDateTime {
        val now = LocalDateTime.now()
        var startDate = fromTimestamp(startDate)
        return when (interval) {
            TimeInterval.MONTH -> {
                while (startDate <= now) {
                    startDate = startDate.plusMonths(1)
                }
                if (!next) startDate.minusMonths(1) else startDate
            }
            TimeInterval.DAY -> {
                while (startDate <= now) {
                    startDate = startDate.plusDays(intervalMultiplier.toLong())
                }
                if (!next) startDate.minusDays(intervalMultiplier.toLong()) else startDate
            }
        }
    }

    companion object {
        const val NULL_SUBSCRIBER = "__shizuku_disabled_sim_fallback__"
    }
}

@Dao
interface DataPlanDao {
    @Query("SELECT * FROM dataplan")
    suspend fun getAll(): List<DataPlan>

    @Query("SELECT * FROM dataplan WHERE hashedSubscriberID = :hashedID")
    suspend fun getByHash(hashedID: String): DataPlan?

    @Query("SELECT * FROM dataplan WHERE simIndex != -1 ORDER BY simIndex ASC")
    fun getActivePlansFlow(): Flow<List<DataPlan>>

    @Query("SELECT * FROM dataplan WHERE simIndex != -1 ORDER BY simIndex ASC")
    suspend fun getActivePlans(): List<DataPlan>

    @Query("SELECT * FROM dataplan WHERE (simIndex != -1 AND notification == 1) ORDER BY simIndex ASC")
    fun getActivePlansWithNotificationsFlow(): Flow<List<DataPlan>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun add(dataPlan: DataPlan)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addAll(plans: List<DataPlan>)
}

@Database(entities = [DataPlan::class], version = 4, exportSchema = true)
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
        return data.split(",").mapNotNull { it.trim().toIntOrNull() }
    }
}

class DataPlanRepository(val dao: DataPlanDao) {
    suspend fun savePlan(plainSubscriberID: String?, simIndex: Int, carrierName: String) {
        val plan = DataPlan(
            hashedSubscriberID = CryptoManager.hashIdentifier(plainSubscriberID ?: NULL_SUBSCRIBER),
            encryptedSubscriberID = CryptoManager.encrypt(plainSubscriberID ?: NULL_SUBSCRIBER),
            simIndex = simIndex,
            carrierName = carrierName
        )
        dao.add(plan)
    }
}