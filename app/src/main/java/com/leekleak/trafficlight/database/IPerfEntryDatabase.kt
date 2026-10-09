package com.leekleak.trafficlight.database

import androidx.room3.ColumnInfo
import androidx.room3.ColumnTypeConverters
import androidx.room3.Dao
import androidx.room3.Database
import androidx.room3.Delete
import androidx.room3.Entity
import androidx.room3.PrimaryKey
import androidx.room3.Query
import androidx.room3.RoomDatabase
import androidx.room3.Upsert
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.Serializable

@Serializable
@Entity
data class IPerfEntry(
    @PrimaryKey val uuid: String,
    @ColumnInfo val name: String,
    @ColumnInfo val ip: String,
    @ColumnInfo val port: String,
    @ColumnInfo val selected: Boolean
)

@Dao
interface IPerfEntryDao {
    @get:Query("SELECT * FROM iperfentry")
    val allEntries: Flow<List<IPerfEntry>>

    @Upsert
    suspend fun upsert(entry: IPerfEntry)

    @Delete
    suspend fun delete(entry: IPerfEntry)
}

@Database(entities = [IPerfEntry::class], version = 1, exportSchema = true)
@ColumnTypeConverters(Converters::class)
abstract class IPerfEntryDatabase : RoomDatabase() {
    abstract fun iPerfEntryDao(): IPerfEntryDao
}
