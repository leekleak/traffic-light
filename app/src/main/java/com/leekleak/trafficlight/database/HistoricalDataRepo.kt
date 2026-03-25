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
import androidx.room.TypeConverters
import kotlinx.coroutines.flow.Flow


@Entity
data class HistoricalData(
    @PrimaryKey val stamp: Long,
    @ColumnInfo val usage: Long
)

@Dao
interface HistoricalDataDao {
    @Query("SELECT * FROM historicaldata")
    fun getAll(): List<HistoricalData>

    @Query("SELECT * FROM historicaldata")
    fun getAllFlow(): Flow<List<HistoricalData>>

    @Query("SELECT COUNT(*) FROM historicaldata WHERE stamp = :stamp")
    fun contains(stamp: Long): Boolean

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun add(data: HistoricalData)
}

@Database(entities = [HistoricalData::class], version = 1, exportSchema = false)
@TypeConverters(Converters::class)
abstract class HistoricalDataCache : RoomDatabase() {
    abstract fun historicalDataDao(): HistoricalDataDao
}