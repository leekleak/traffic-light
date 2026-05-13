package com.leekleak.trafficlight.model

import android.app.usage.NetworkStats
import android.app.usage.NetworkStatsManager
import com.leekleak.trafficlight.charts.model.BarData
import com.leekleak.trafficlight.charts.model.ScrollableBarData
import com.leekleak.trafficlight.database.AppUsage
import com.leekleak.trafficlight.database.DataDirection
import com.leekleak.trafficlight.database.DataDirection.Bidirectional
import com.leekleak.trafficlight.database.DataDirection.Download
import com.leekleak.trafficlight.database.DataDirection.Upload
import com.leekleak.trafficlight.database.DataPlan
import com.leekleak.trafficlight.database.DataType
import com.leekleak.trafficlight.database.DayUsage
import com.leekleak.trafficlight.database.HourUsage
import com.leekleak.trafficlight.database.UsageQuery
import com.leekleak.trafficlight.model.AppManager.Companion.allApp
import com.leekleak.trafficlight.model.AppManager.Companion.specialUIDs
import com.leekleak.trafficlight.model.AppManager.Companion.unknownApp
import com.leekleak.trafficlight.ui.history.DateParams
import com.leekleak.trafficlight.util.fromTimestamp
import com.leekleak.trafficlight.util.getName
import com.leekleak.trafficlight.util.toTimestamp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.TextStyle
import java.time.temporal.WeekFields
import java.util.Locale

data class UsageData(
    val upload: Long = 0,
    val download: Long = 0,
    val uid: Int? = null,
    val start: LocalDateTime = LocalDateTime.now(),
    val end: LocalDateTime = LocalDateTime.now()
) {
    val total: Long
        get() = upload + download

    fun forDirection(dataDirection: DataDirection): Long = when(dataDirection) {
        Upload -> upload
        Download -> download
        Bidirectional -> upload + download
    }
}

class NetworkUsageManager(
    private var networkStatsManager: NetworkStatsManager,
    private val appManager: AppManager,
) {
    suspend fun totalDayUsage(
        query: UsageQuery,
        startDate: LocalDate,
        subscriberId: String? = null,
        endDate: LocalDate = startDate.plusDays(1),
    ): Long {
        val startStamp = startDate.toTimestamp()
        val endStamp = endDate.toTimestamp()
        return getNetworkDataForType(startStamp, endStamp, subscriberId, query.dataType).sumOf {
                if (it.uid == query.dataUID.uid || query.dataUID.uidQuery == null) {
                    return@sumOf it.forDirection(query.dataDirection)
                } else {
                    return@sumOf 0
                }
            }
    }

    suspend fun planUsage(dataPlan: DataPlan): Long {
        val now = LocalDateTime.now()
        val startDate = dataPlan.getStartDate()
        val startStamp = startDate.toTimestamp()
        val endStamp = now.toTimestamp()
        val subscriberId = dataPlan.decryptedID

        val networkData = getNetworkDataForType(startStamp, endStamp, subscriberId, DataType.Mobile)
        val stats = networkData.filter { !dataPlan.excludedApps.contains(it.uid) }.sumOf { it.total }

        return stats
    }

    suspend fun getNetworkDataForType(
        startStamp: Long,
        endStamp: Long,
        subscriberId: String?,
        type: DataType
    ): List<UsageData> = withContext(Dispatchers.IO) {
        networkStatsManager.querySummary(type.queryIndex ?: return@withContext listOf(), subscriberId, startStamp, endStamp).use { summary ->
            val map = mutableMapOf<Int, UsageData>()
            while (summary.hasNextBucket()) {
                val bucket = NetworkStats.Bucket()
                summary.getNextBucket(bucket)
                map.merge(bucket.uid, UsageData(bucket.txBytes, bucket.rxBytes, bucket.uid)) { old, new ->
                    old.copy(upload = old.upload + new.upload, download = old.download + new.download)
                }
            }
            return@withContext map.values.toList()
        }
    }

    suspend fun getNetworkDataForTypeHourly(
        startStamp: Long,
        endStamp: Long,
        subscriberId: String?,
        type: DataType,
        uid: Int?
    ): List<UsageData> = withContext(Dispatchers.IO) {
        if (uid == null) {
            networkStatsManager.queryDetails(type.queryIndex ?: return@withContext listOf(), subscriberId, startStamp, endStamp)
        } else {
            networkStatsManager.queryDetailsForUid(type.queryIndex ?: return@withContext listOf(), subscriberId, startStamp, endStamp, uid)
        }.use { summary ->
            val map = mutableMapOf<LocalDateTime, UsageData>()
            while (summary.hasNextBucket()) {
                val bucket = NetworkStats.Bucket()
                summary.getNextBucket(bucket)
                val end = fromTimestamp(bucket.endTimeStamp)
                val start = fromTimestamp(bucket.startTimeStamp)

                map.merge(start, UsageData(bucket.txBytes, bucket.rxBytes, start = start, end = end)) { old, new ->
                    old.copy(upload = old.upload + new.upload, download = old.download + new.download)
                }
            }
            return@withContext map.values.toList()
        }
    }

    suspend fun getAllAppUsage(
        startStamp: Long,
        endStamp: Long,
        query1: UsageQuery,
        query2: UsageQuery,
        subscriberId: String? = null
    ): List<AppUsage> {
        val usage1 = getNetworkDataForType(startStamp, endStamp, subscriberId, query1.dataType)
        val usage2 = getNetworkDataForType(startStamp, endStamp, subscriberId, query2.dataType)

        val uids = usage1.map { it.uid }.union(usage2.map { it.uid }).union(specialUIDs).filterNotNull()

        val list = uids.map { uid ->
            val uid1 = usage1.find { it.uid == uid } ?: UsageData()
            val uid2 = usage2.find { it.uid == uid } ?: UsageData()
            AppUsage(
                app = appManager.getAppForUID(uid),
                usage = DayUsage(
                    usage1 = uid1.forDirection(query1.dataDirection),
                    usage2 = uid2.forDirection(query2.dataDirection)
                ),
            )
        }.toMutableList()

        val totalUsage = DayUsage(
            usage1 = list.sumOf { it.usage.usage1 },
            usage2 = list.sumOf { it.usage.usage2 }
        )

        list.sortByDescending { it.usage.totalUsage }
        list.add(0, AppUsage(
            app = allApp,
            usage = totalUsage
        ))
        list.sortWith(compareBy { it.app == unknownApp })
        list.removeAll { it.usage.totalUsage == 0L }
        return list.distinctBy { it.app.uid }.toList()
    }

    suspend fun getAllHourUsage(
        dateParams: DateParams,
        query1: UsageQuery,
        query2: UsageQuery
    ): List<HourUsage> {
        val (startDate, endDate) = dateParams.getStartEndDates()
        val startTime = startDate.atStartOfDay().toTimestamp()
        val endTime = endDate.atStartOfDay().toTimestamp()
        val oneHourMilli = 60 * 60 * 1000L
        val twoHoursMilli = oneHourMilli * 2

        val usage1 = getNetworkDataForTypeHourly(startTime, endTime, null, query1.dataType, query1.dataUID.uidQuery)
        val usage2 = getNetworkDataForTypeHourly(startTime, endTime, null, query2.dataType, query2.dataUID.uidQuery)
        val slots = 0..(endTime - startTime) step twoHoursMilli
        val map1 = slots.associateWith { 0L }.toMutableMap()
        val map2 = slots.associateWith { 0L }.toMutableMap()

        for ((usageList, query, map) in listOf(
            Triple(usage1, query1, map1),
            Triple(usage2, query2, map2),
        )) {
            usageList.forEach { usage ->
                val stampStart = usage.start.toTimestamp() - startTime
                val stampEnd = usage.end.toTimestamp() - startTime
                val bucketRange = stampStart..stampEnd

                for (slot in slots) {
                    val mapRange = slot..(slot+twoHoursMilli)
                    val overlap = overlapRatio(bucketRange, mapRange)
                    map[slot] = map.getValue(slot) + (usage.forDirection(query.dataDirection) * overlap).toLong()
                }
            }
        }

        val start = fromTimestamp(startTime)
        val result = (0..22 step 2).map { i ->
            HourUsage(
                start = start.withHour(i),
                end = start.withHour(i).plusHours(2),
                usage = DayUsage(
                    date = dateParams.day,
                    usage1 = map1.filter { it.key % (12 * twoHoursMilli) == i * oneHourMilli }.values.sum(),
                    usage2 = map2.filter { it.key % (12 * twoHoursMilli) == i * oneHourMilli }.values.sum()
                ),
            )
        }

        return result
    }

    fun overlapRatio(range1: ClosedRange<Long>, range2: ClosedRange<Long>): Double {
        val overlapStart = maxOf(range1.start, range2.start)
        val overlapEnd = minOf(range1.endInclusive, range2.endInclusive)

        val overlapLength = maxOf(0L, overlapEnd - overlapStart).toDouble()
        val range1Length = range1.endInclusive - range1.start

        if (range1Length == 0L) return if (range2.contains(range1.start)) 1.0 else 0.0

        return overlapLength / (range1Length.toDouble())
    }

    fun daysUsage(
        startDate: LocalDate,
        endDate: LocalDate,
        usageQuery1: UsageQuery?,
        usageQuery2: UsageQuery? = null
    ): Flow<List<ScrollableBarData>> = flow {
        val data: MutableList<ScrollableBarData> = mutableListOf()
        val range = startDate.toEpochDay() until endDate.toEpochDay()

        for (i in range) {
            val now = LocalDate.ofEpochDay(i)
            data.add(ScrollableBarData(now))
        }
        emit(data.toList())
        coroutineScope {
            data.indices.map { i ->
                async {
                    val now = LocalDate.ofEpochDay(i + startDate.toEpochDay())
                    val usage1 = usageQuery1?.let { totalDayUsage(it, now) }
                    val usage2 = usageQuery2?.let { totalDayUsage(it, now) }
                    data[i] = data[i].copy(
                        y1 = usage1 ?: 0L,
                        y2 = usage2 ?: 0L,
                    )
                }
            }.awaitAll()
        }
        emit(data.toList())
    }

    suspend fun getWeekUsage(subscriberId: String?, withWifi: Boolean): List<BarData> {
        val field = WeekFields.of(Locale.getDefault())
        val firstDay = field.firstDayOfWeek
        val data: MutableList<BarData> = MutableList(7) { i ->
            val x = firstDay.plus(i.toLong()).getName(TextStyle.SHORT_STANDALONE)
            BarData(x, 0, 0)
        }
        val now = LocalDate.now()
        val daysPassed = now.get(field.dayOfWeek()) - 1

        coroutineScope {
            (0..daysPassed).map { i ->
                async {
                    val now = now.minusDays(daysPassed.toLong() - i)
                    val usage1 = totalDayUsage(UsageQuery(DataType.Mobile), now, subscriberId)
                    val usage2 = if (withWifi) totalDayUsage(UsageQuery(DataType.Wifi), now, subscriberId) else 0
                    data[i] = data[i].copy(y1 = usage1, y2 = usage2)
                }
            }.awaitAll()
        }
        return data.toList()
    }
}