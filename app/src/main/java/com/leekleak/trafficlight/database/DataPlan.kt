package com.leekleak.trafficlight.database

import android.app.usage.NetworkStats
import android.content.Context
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey
import com.leekleak.trafficlight.R
import com.leekleak.trafficlight.model.NetworkUsageManager
import com.leekleak.trafficlight.util.DataSize
import com.leekleak.trafficlight.util.DataSizeUnit
import com.leekleak.trafficlight.util.fromTimestamp
import com.leekleak.trafficlight.util.toTimestamp
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import kotlin.math.min

@Serializable
@Entity
data class DataPlan(
    @PrimaryKey
    val hashedSubscriberID: String,

    @ColumnInfo val encryptedSubscriberID: String,

    @ColumnInfo val simIndex: Int = -1,
    @ColumnInfo val carrierName: String = "",

    @ColumnInfo val startDate: Long = LocalDate.now().withDayOfMonth(1).atStartOfDay().toTimestamp(),
    @ColumnInfo val interval: TimeInterval = TimeInterval.MONTH,
    @ColumnInfo val intervalMultiplier: Int = 1,
    @ColumnInfo val recurring: Boolean = false,

    @ColumnInfo val excludedApps: List<Int> = listOf(),

    @ColumnInfo val notification: Boolean = false,
    @ColumnInfo val liveNotification: Boolean = false,

    @ColumnInfo val budgetWarning: Boolean = false,
    @ColumnInfo val safetyWarning: Boolean = false,

    @ColumnInfo val lastSafetyState: Int = -1,
    @ColumnInfo val budgetOvershotNotified: Boolean = false,

    @ColumnInfo var mainDataSize: DataSize = DataSize(0),
    @ColumnInfo var mainDataSizeUnit: DataSizeUnit = DataSizeUnit.GB,
    @ColumnInfo var mainDataUsed: Long = 0,
    @ColumnInfo var mainStartStamp: Long = startDate,
    @ColumnInfo var mainExpiryStamp: Long = Long.MAX_VALUE,

    @ColumnInfo var extras: List<DataPlanExtra> = listOf(),
    @ColumnInfo var lastUpdateStamp: Long = 0,
    /**
     * Customization
     */
    @ColumnInfo val uiBackground: Int = if (simIndex != -1) simIndex + 1 else 0,
    @ColumnInfo val uiColor: Int = 0,
    @ColumnInfo val note: String = "",
) {
    @Ignore
    @Transient
    private val mutex = Mutex()

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

    fun resetUsage() {
        mainDataUsed = 0
        extras = extras.map { it.copy(dataUsed = 0) }
        lastUpdateStamp = 0
    }

    private fun calculateNextReset(from: Long): Long {
        val date = fromTimestamp(from)
        return when (interval) {
            TimeInterval.MONTH -> date.plusMonths(1).toTimestamp()
            TimeInterval.DAY -> date.plusDays(intervalMultiplier.toLong()).toTimestamp()
        }
    }

    suspend fun updateUsage(networkUsageManager: NetworkUsageManager) = mutex.withLock {
        val now = LocalDateTime.now().toTimestamp()

        val currentStart = getStartDate(false).toTimestamp()
        val currentEnd = getStartDate(true).toTimestamp()

        if (lastUpdateStamp == 0L) {
            lastUpdateStamp = currentStart
        }

        if (mainExpiryStamp == Long.MAX_VALUE || mainExpiryStamp == 0L) {
            mainStartStamp = currentStart
            mainExpiryStamp = currentEnd
        }

        val usageBuckets = networkUsageManager.queryDetails(DataType.Mobile.queryIndex!!, decryptedID, lastUpdateStamp, now)

        var bestEnd = lastUpdateStamp
        while (usageBuckets.hasNextBucket()) {
            val bucket = NetworkStats.Bucket()
            usageBuckets.getNextBucket(bucket)
            if (bucket.endTimeStamp > now) {
                bestEnd = maxOf(bestEnd, bucket.startTimeStamp)
                break
            }
            bestEnd = bucket.endTimeStamp
        }

        if (bestEnd > lastUpdateStamp) {
            val stamps = mutableSetOf(lastUpdateStamp, bestEnd)
            for (extra in extras) {
                if (!extra.expired) {
                    if (extra.startStamp in (lastUpdateStamp + 1)..<bestEnd) {
                        stamps.add(extra.startStamp)
                    }
                    if (extra.expiryStamp in (lastUpdateStamp + 1)..<bestEnd) {
                        stamps.add(extra.expiryStamp)
                    }
                }
            }

            var nextReset = mainExpiryStamp
            while (nextReset in lastUpdateStamp..bestEnd) {
                stamps.add(nextReset)
                nextReset = calculateNextReset(nextReset)
            }

            val sortedStamps = stamps.sorted()
            val updatedExtras = extras.toMutableList()

            for (i in 0 until sortedStamps.size - 1) {
                val start = sortedStamps[i]
                val end = sortedStamps[i + 1]

                if (start >= mainExpiryStamp) {
                    val nextExpiry = calculateNextReset(mainExpiryStamp)
                    if (recurring) {
                        val remaining = mainDataSize.byteValue - mainDataUsed
                        if (remaining > 0) {
                            updatedExtras.add(
                                DataPlanExtra(
                                    dataAmount = DataSize(remaining),
                                    unit = mainDataSizeUnit,
                                    startStamp = mainExpiryStamp,
                                    expiryStamp = nextExpiry
                                )
                            )
                        }
                    }
                    mainDataUsed = 0
                    mainStartStamp = mainExpiryStamp
                    mainExpiryStamp = nextExpiry
                }

                val usageData = networkUsageManager.getNetworkDataForType(start, end, decryptedID, DataType.Mobile)
                var usageToDistribute = usageData.filter { !excludedApps.contains(it.uid) }.sumOf { it.total }

                val activeIndices = updatedExtras.indices.filter { idx ->
                    val extra = updatedExtras[idx]
                    !extra.expired && extra.startStamp <= start && extra.expiryStamp >= end
                }.sortedBy { updatedExtras[it].expiryStamp }

                for (idx in activeIndices) {
                    val extra = updatedExtras[idx]
                    val used = min(extra.dataRemaining, usageToDistribute)
                    usageToDistribute -= used
                    updatedExtras[idx] = extra.copy(dataUsed = extra.dataUsed + used)
                }
                mainDataUsed += usageToDistribute
            }

            extras = updatedExtras
            lastUpdateStamp = bestEnd
        }

        if (mainStartStamp < currentStart) {
            mainDataUsed = 0
            mainStartStamp = currentStart
            mainExpiryStamp = currentEnd
            lastUpdateStamp = maxOf(lastUpdateStamp, currentStart)
        }

        extras = extras.map { extra ->
            if (!extra.expired && extra.expiryStamp <= now) {
                extra.copy(expired = true)
            } else {
                extra
            }
        }
    }

    fun getTotalMax(): Long {
        return mainDataSize.byteValue + extras.filter { !it.expired }.sumOf { it.dataAmount.byteValue }
    }

    suspend fun getUsage(networkUsageManager: NetworkUsageManager): Long {
        updateUsage(networkUsageManager)

        val activeExtras = extras.filter { !it.expired }
        val committedUsage = mainDataUsed + activeExtras.sumOf { it.dataUsed }

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