package com.leekleak.trafficlight.database

import android.app.usage.NetworkStats
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
import com.leekleak.trafficlight.model.NetworkUsageManager
import com.leekleak.trafficlight.util.fromTimestamp
import com.leekleak.trafficlight.util.toTimestamp
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID
import kotlin.math.min

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

    @ColumnInfo var mainUsage: DataPlanMain = DataPlanMain(0, 0, startDate, Long.MAX_VALUE),
    @ColumnInfo val extras: List<DataPlanExtra> = listOf(),
    @ColumnInfo var lastUpdateStamp: Long = 0,
    /**
     * Customization
     */
    @ColumnInfo val uiBackground: Int = if (simIndex != -1) simIndex + 1 else 0,
    @ColumnInfo val uiColor: Int = 0,
    @ColumnInfo val note: String = "",
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

    fun getRemainingDuration(): Duration {
        val now = LocalDateTime.now()
        val startDate = getStartDate(true)
        return Duration.between(now, startDate)
    }

    fun resetString(context: Context): String {
        val remaining = getRemainingDuration()

        val days = remaining.toDays().toInt() + 1
        return if (days == 1) {
            val hours = remaining.toHours().toInt() + 1
            context.resources.getQuantityString(R.plurals.resets_in_hours, hours, hours)
        } else {
            context.resources.getQuantityString(R.plurals.resets_in_days, days,days)
        }
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

    suspend fun updateUsage(networkUsageManager: NetworkUsageManager) {
        val now = System.currentTimeMillis()

        val currentStart = getStartDate(false).toTimestamp()
        val currentEnd = getStartDate(true).toTimestamp()

        if (lastUpdateStamp == 0L) {
            lastUpdateStamp = currentStart
        }

        if (mainUsage.startStamp != currentStart) {
            mainUsage = DataPlanMain(
                dataAmount = mainUsage.dataAmount,
                dataUsed = 0,
                startStamp = currentStart,
                expiryStamp = currentEnd,
            )
            lastUpdateStamp = maxOf(lastUpdateStamp, currentStart)
        }

        val activeExtras = extras.filter { !it.expired }.sortedBy { it.expiryStamp }
        val usageBuckets = networkUsageManager.queryDetails(DataType.Mobile.queryIndex!!, decryptedID, lastUpdateStamp, now)

        var bestEnd = lastUpdateStamp
        while (usageBuckets.hasNextBucket()) {
            val bucket = NetworkStats.Bucket()
            usageBuckets.getNextBucket(bucket)
            if (bucket.endTimeStamp >= now) {
                bestEnd = bucket.startTimeStamp
                break
            }
            bestEnd = bucket.endTimeStamp
        }

        if (bestEnd > lastUpdateStamp) {
            val usageData = networkUsageManager.getNetworkDataForType(lastUpdateStamp, bestEnd, decryptedID, DataType.Mobile)
            var usageToDistribute = usageData.filter { !excludedApps.contains(it.uid) }.sumOf { it.total }

            for (extra in activeExtras) {
                val used = min(extra.dataRemaining, usageToDistribute)
                usageToDistribute -= used
                extra.dataUsed += used
            }

            mainUsage.dataUsed += usageToDistribute // Push remaining usage to mainUsage

            lastUpdateStamp = bestEnd
        }
        
        for (extra in extras) {
            if (!extra.expired && extra.expiryStamp <= now) {
                extra.expired = true
            }
        }
    }

    fun getTotalMax(): Long {
        return mainUsage.dataAmount + extras.filter { !it.expired }.sumOf { it.dataAmount }
    }

    suspend fun getUsage(networkUsageManager: NetworkUsageManager): Long {
        updateUsage(networkUsageManager)

        val activeExtras = extras.filter { !it.expired }
        val committedUsage = mainUsage.dataUsed + activeExtras.sumOf { it.dataUsed }

        val now = System.currentTimeMillis()
        var volatileUsage = 0L
        if (now > lastUpdateStamp) {
            val data = networkUsageManager.getNetworkDataForType(lastUpdateStamp, now, decryptedID, DataType.Mobile)
            volatileUsage = data.filter { !excludedApps.contains(it.uid) }.sumOf { it.total }
        }

        return committedUsage + volatileUsage
    }
  companion object {
        const val NULL_SUBSCRIBER = "__shizuku_disabled_sim_fallback__"
    }
}

interface IDataBucket {
    val dataAmount: Long
    var dataUsed: Long
    val startStamp: Long
    val expiryStamp: Long
    val id: String
    var expired: Boolean

    val dataRemaining: Long
        get() = if (dataAmount <= 0) Long.MAX_VALUE else dataAmount - dataUsed

    val timeRange: LongRange
        get() = startStamp..expiryStamp
}

@Serializable
data class DataPlanExtra(
    override val dataAmount: Long,
    override var dataUsed: Long = 0,
    override val startStamp: Long,
    override val expiryStamp: Long,
    override val id: String = UUID.randomUUID().toString(),
    override var expired: Boolean = false
) : IDataBucket

@Serializable
data class DataPlanMain(
    override val dataAmount: Long,
    override var dataUsed: Long = 0,
    override val startStamp: Long,
    override val expiryStamp: Long,
    override val id: String = "MAIN",
    override var expired: Boolean = false
) : IDataBucket

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

    @TypeConverter
    fun fromMainUsage(bucket: DataPlanMain): String {
        return Json.encodeToString(bucket)
    }

    @TypeConverter
    fun toMainUsage(data: String): DataPlanMain {
        return try {
            Json.decodeFromString(data)
        } catch (_: Exception) {
            DataPlanMain(0, 0, 0, 0)
        }
    }

    @TypeConverter
    fun fromListExtras(list: List<DataPlanExtra>): String {
        return Json.encodeToString(list)
    }

    @TypeConverter
    fun toListExtras(data: String): List<DataPlanExtra> {
        return try {
            Json.decodeFromString(data)
        } catch (_: Exception) {
            listOf()
        }
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