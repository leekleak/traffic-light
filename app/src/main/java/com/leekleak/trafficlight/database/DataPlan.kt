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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import kotlin.math.min

@Serializable
data class DataPlanSnapshot(
    val mainDataUsed: Long = 0,
    val mainDataSizeUnit: DataSizeUnit = DataSizeUnit.GB,
    val extras: List<DataPlanExtra> = emptyList()
) {
    val totalUsage: Long = mainDataUsed + extras.filter { !it.expired }.sumOf { it.dataUsed }
}

@Serializable
@Entity
data class DataPlan(
    @PrimaryKey
    val hashedSubscriberID: String,

    @ColumnInfo val encryptedSubscriberID: String,

    @ColumnInfo val simIndex: Int = -1,
    @ColumnInfo val carrierName: String = "",
    @ColumnInfo val configured: Boolean = false,

    @ColumnInfo val startDate: Long = LocalDate.now().withDayOfMonth(1).atStartOfDay().toTimestamp(),
    @ColumnInfo val interval: TimeInterval = TimeInterval.MONTH,
    @ColumnInfo val intervalMultiplier: Int = 1, // Intended only for use when TimeInterval.DAY. Otherwise should be ignored.
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

    @Ignore
    @Transient
    private var _decryptedID: String? = null

    @Ignore
    @Transient
    private var _decryptedIDInitialized = false

    init {
        require(intervalMultiplier > 0) {
            "intervalMultiplier must be positive, got $intervalMultiplier"
        }
    }

    val decryptedID: String?
        get() {
            if (!_decryptedIDInitialized) {
                val decrypted = CryptoManager.decrypt(encryptedSubscriberID)
                _decryptedID = if (decrypted == NULL_SUBSCRIBER) null else decrypted
                _decryptedIDInitialized = true
            }
            return _decryptedID
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
        val startDateTime = fromTimestamp(startDate)
        return when (interval) {
            TimeInterval.MONTH -> {
                val monthsBetween = ChronoUnit.MONTHS.between(startDateTime, now)
                var currentPeriodStart = startDateTime.plusMonths(monthsBetween)
                if (currentPeriodStart > now) {
                    currentPeriodStart = currentPeriodStart.minusMonths(1)
                }
                if (next) currentPeriodStart.plusMonths(1) else currentPeriodStart
            }
            TimeInterval.DAY -> {
                val daysBetween = ChronoUnit.DAYS.between(startDateTime, now)
                val periodsBetween = daysBetween / intervalMultiplier
                var currentPeriodStart = startDateTime.plusDays(periodsBetween * intervalMultiplier)
                if (currentPeriodStart > now) {
                    currentPeriodStart = currentPeriodStart.minusDays(intervalMultiplier.toLong())
                }
                if (next) currentPeriodStart.plusDays(intervalMultiplier.toLong()) else currentPeriodStart
            }
        }
    }

    private fun calculateNextReset(from: Long): Long {
        val date = fromTimestamp(from)
        return when (interval) {
            TimeInterval.MONTH -> date.plusMonths(1).toTimestamp()
            TimeInterval.DAY -> date.plusDays(intervalMultiplier.toLong()).toTimestamp()
        }
    }

    suspend fun updateUsage(networkUsageManager: NetworkUsageManager): Long = withContext(Dispatchers.Default) {
        mutex.withLock {
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
            bestEnd = maxOf(bestEnd, bucket.endTimeStamp)
        }
        usageBuckets.close()

        val timelineEnd = maxOf(bestEnd, currentStart)

        if (timelineEnd > lastUpdateStamp) {
            val stamps = mutableSetOf(lastUpdateStamp, timelineEnd)
            for (extra in extras) {
                if (!extra.expired) {
                    if (extra.startStamp in (lastUpdateStamp + 1)..<timelineEnd) {
                        stamps.add(extra.startStamp)
                    }
                    if (extra.expiryStamp in (lastUpdateStamp + 1)..<timelineEnd) {
                        stamps.add(extra.expiryStamp)
                    }
                }
            }

            var nextReset = mainExpiryStamp
            while (nextReset in lastUpdateStamp..timelineEnd) {
                stamps.add(nextReset)
                nextReset = calculateNextReset(nextReset)
            }

            val sortedStamps = stamps.sorted()

            for (i in 0 until sortedStamps.size - 1) {
                processInterval(networkUsageManager, sortedStamps[i], sortedStamps[i + 1])
            }

            lastUpdateStamp = timelineEnd
        }

        markExpiredExtras(now)
        lastUpdateStamp
    } }

    private fun markExpiredExtras(now: Long) {
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

    suspend fun getUsageSnapshot(networkUsageManager: NetworkUsageManager): DataPlanSnapshot {
        updateUsage(networkUsageManager)

        val now = LocalDateTime.now().toTimestamp()
        val tomorrow = LocalDate.now().plusDays(1).atStartOfDay().toTimestamp()
        var mainUsed = mainDataUsed
        val snapshotExtras = extras.map { it.copy() }.toMutableList()

        if (now > lastUpdateStamp) {
            val volatileUsage = getFilteredUsage(networkUsageManager, lastUpdateStamp, tomorrow, decryptedID)
            
            var usageToDistribute = volatileUsage

            val activeIndices = snapshotExtras.indices.filter { idx ->
                val extra = snapshotExtras[idx]
                extra.startStamp <= lastUpdateStamp && extra.expiryStamp >= now
            }.sortedBy { snapshotExtras[it].expiryStamp }

            for (idx in activeIndices) {
                val extra = snapshotExtras[idx]
                val used = min(extra.dataRemaining, usageToDistribute)
                usageToDistribute -= used
                snapshotExtras[idx] = extra.copy(dataUsed = extra.dataUsed + used)
            }
            mainUsed += usageToDistribute
        }

        return DataPlanSnapshot(
            mainDataUsed = mainUsed,
            mainDataSizeUnit = mainDataSizeUnit,
            extras = snapshotExtras
        )
    }

    private suspend fun processInterval(networkUsageManager: NetworkUsageManager, start: Long, end: Long) {
        var currentStart = start
        while (end > mainExpiryStamp || (currentStart < mainExpiryStamp && end == mainExpiryStamp)) {
            val usageBeforeReset = getFilteredUsage(networkUsageManager, currentStart, mainExpiryStamp, decryptedID)
            distributeUsage(usageBeforeReset, currentStart, mainExpiryStamp)

            val nextExpiry = calculateNextReset(mainExpiryStamp)
            if (recurring) {
                val remaining = mainDataSize.byteValue - mainDataUsed
                if (remaining > 0) {
                    extras = extras + DataPlanExtra(
                        dataAmount = DataSize(remaining),
                        unit = mainDataSizeUnit,
                        startStamp = mainExpiryStamp,
                        expiryStamp = nextExpiry
                    )
                }
            }
            mainDataUsed = 0
            mainStartStamp = mainExpiryStamp
            mainExpiryStamp = nextExpiry
            currentStart = mainStartStamp
        }

        if (end > currentStart) {
            val usage = getFilteredUsage(networkUsageManager, currentStart, end, decryptedID)
            distributeUsage(usage, currentStart, end)
        }
    }

    private fun distributeUsage(usage: Long, start: Long, end: Long) {
        var usageToDistribute = usage

        val updatedExtras = extras.toMutableList()
        val activeIndices = updatedExtras.indices.filter { idx ->
            val extra = updatedExtras[idx]
            extra.startStamp <= start && extra.expiryStamp >= end
        }.sortedBy { updatedExtras[it].expiryStamp }

        for (idx in activeIndices) {
            val extra = updatedExtras[idx]
            val used = min(extra.dataRemaining, usageToDistribute)
            usageToDistribute -= used
            updatedExtras[idx] = extra.copy(dataUsed = extra.dataUsed + used)
        }
        mainDataUsed += usageToDistribute
        extras = updatedExtras
    }

    private suspend fun getFilteredUsage(networkUsageManager: NetworkUsageManager, start: Long, end: Long, id: String?): Long {
        val usageData = networkUsageManager.getNetworkDataForType(start, end, id, DataType.Mobile)
        return usageData.filter { !excludedApps.contains(it.uid) }.sumOf { it.total }
    }

    companion object {
        const val NULL_SUBSCRIBER = "__shizuku_disabled_sim_fallback__"
    }
}