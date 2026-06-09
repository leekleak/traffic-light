package com.leekleak.trafficlight.database

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import com.leekleak.trafficlight.database.DataPlan.Companion.NULL_SUBSCRIBER
import com.leekleak.trafficlight.util.DataSize
import com.leekleak.trafficlight.util.DataSizeUnit
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.util.UUID

enum class TimeInterval {
    DAY,
    MONTH
}

@Serializable
data class DataPlanExtra(
    val dataAmount: DataSize,
    val unit: DataSizeUnit = DataSizeUnit.GB,
    val dataUsed: Long = 0,
    val startStamp: Long,
    val expiryStamp: Long,
    val id: String = UUID.randomUUID().toString(),
    val expired: Boolean = false
) {
    val dataRemaining: Long
        get() = if (dataAmount.byteValue <= 0) Long.MAX_VALUE else dataAmount.byteValue - dataUsed
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

    @Query("SELECT COUNT(*) FROM dataplan WHERE (simIndex != -1 AND notification == 1)")
    fun getActivePlansWithNotificationsCountFlow(): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun add(dataPlan: DataPlan)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addAll(plans: List<DataPlan>)

    @Query("DELETE FROM dataplan WHERE hashedSubscriberID = :hashedID")
    suspend fun delete(hashedID: String)
}

@Database(entities = [DataPlan::class], version = 4, exportSchema = true)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun dataPlanDao(): DataPlanDao
}

class Converters {
    @TypeConverter
    fun fromDataSize(dataSize: DataSize): Long {
        return dataSize.byteValue
    }

    @TypeConverter
    fun toDataSize(byteValue: Long): DataSize {
        return DataSize(byteValue)
    }

    @TypeConverter
    fun toTimeInterval(name: String): TimeInterval {
        return try {
            TimeInterval.valueOf(name)
        } catch (_: Exception) {
            TimeInterval.MONTH
        }
    }

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