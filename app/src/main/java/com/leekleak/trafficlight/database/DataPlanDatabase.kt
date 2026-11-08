package com.leekleak.trafficlight.database

import androidx.room3.ColumnTypeConverter
import androidx.room3.ColumnTypeConverters
import androidx.room3.Dao
import androidx.room3.Database
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.Query
import androidx.room3.RoomDatabase
import com.leekleak.core.util.DataSize
import com.leekleak.core.util.DataSizeUnit
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
        get() = if (dataAmount.byteValue <= 0) {
            Long.MAX_VALUE
        } else {
            (dataAmount.byteValue - dataUsed).coerceAtLeast(0L)
        }
}

@Dao
interface DataPlanDao {
    @Query("SELECT * FROM dataplan")
    suspend fun getAll(): List<DataPlan>

    @Query("SELECT * FROM dataplan WHERE hashedSubscriberID = :hashedID")
    suspend fun getByHash(hashedID: String): DataPlan?

    @get:Query("SELECT * FROM dataplan ORDER BY simIndex ASC")
    val allPlansFlow: Flow<List<DataPlan>>

    @get:Query("SELECT * FROM dataplan WHERE simIndex != -1 ORDER BY simIndex ASC")
    val activePlansFlow: Flow<List<DataPlan>>

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

class Converters {
    @ColumnTypeConverter
    fun fromDataSize(dataSize: DataSize): Long {
        return dataSize.byteValue
    }

    @ColumnTypeConverter
    fun toDataSize(byteValue: Long): DataSize {
        return DataSize(byteValue)
    }

    @ColumnTypeConverter
    fun toTimeInterval(name: String): TimeInterval {
        return try {
            TimeInterval.valueOf(name)
        } catch (_: Exception) {
            TimeInterval.MONTH
        }
    }

    @ColumnTypeConverter
    fun fromListInt(list: List<Int>): String {
        return list.joinToString(",")
    }

    @ColumnTypeConverter
    fun toListInt(data: String): List<Int> {
        if (data == "") return listOf()
        return data.split(",").mapNotNull { it.trim().toIntOrNull() }
    }

    @ColumnTypeConverter
    fun fromListExtras(list: List<DataPlanExtra>): String {
        return Json.encodeToString(list)
    }

    @ColumnTypeConverter
    fun toListExtras(data: String): List<DataPlanExtra> {
        return try {
            Json.decodeFromString(data)
        } catch (_: Exception) {
            listOf()
        }
    }
}

@Database(entities = [DataPlan::class], version = 5, exportSchema = true)
@ColumnTypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun dataPlanDao(): DataPlanDao
}