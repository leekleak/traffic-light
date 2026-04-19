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
import com.leekleak.trafficlight.database.TimeInterval
import com.leekleak.trafficlight.database.UsageQuery
import com.leekleak.trafficlight.model.AppManager.Companion.allApp
import com.leekleak.trafficlight.model.AppManager.Companion.specialUIDs
import com.leekleak.trafficlight.model.AppManager.Companion.unknownApp
import com.leekleak.trafficlight.ui.history.DateParams
import com.leekleak.trafficlight.util.fromTimestamp
import com.leekleak.trafficlight.util.getName
import com.leekleak.trafficlight.util.toTimestamp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.TextStyle
import java.time.temporal.ChronoUnit
import java.time.temporal.WeekFields
import java.util.Locale
import kotlin.math.max

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
    fun totalDayUsage(
        query: UsageQuery,
        startDate: LocalDate,
        endDate: LocalDate = startDate.plusDays(1),
    ): Long {
        val startStamp = startDate.atStartOfDay().truncatedTo(ChronoUnit.DAYS).toTimestamp()
        val endStamp = endDate.atStartOfDay().truncatedTo(ChronoUnit.DAYS).toTimestamp()
        return getNetworkDataForType(startStamp, endStamp, null, query.dataType).sumOf {
                if (it.uid == query.dataUID.uid || query.dataUID.uidQuery == null) {
                    return@sumOf it.forDirection(query.dataDirection)
                } else {
                    return@sumOf 0
                }
            }
    }

    fun todayMobileUsage(): Flow<Long> = flow {
        val now = LocalDate.now()
        emit(totalDayUsage(UsageQuery(DataType.Mobile), now))
    }.flowOn(Dispatchers.IO)

    fun planUsage(dataPlan: DataPlan): Long {
        val now = LocalDateTime.now()
        val startDate = when (dataPlan.interval) {
            TimeInterval.MONTH -> {
                var startDate = fromTimestamp(dataPlan.startDate)
                while (startDate <= now) {
                    startDate = startDate.plusMonths(1)
                }
                startDate.minusMonths(1)
            }
            TimeInterval.DAY -> {
                var startDate = fromTimestamp(dataPlan.startDate)
                while (startDate <= now) {
                    startDate = startDate.plusDays(dataPlan.intervalMultiplier.toLong())
                }
                startDate.minusDays(dataPlan.intervalMultiplier.toLong())
            }
        }

        val startStamp = startDate.toTimestamp()
        val endStamp = now.toTimestamp()
        val subscriberId = if (dataPlan.subscriberID == NULL_SUBSCRIBER) null else dataPlan.subscriberID

        val networkData = getNetworkDataForType(startStamp, endStamp, subscriberId, DataType.Mobile)
        val stats = networkData.filter { !dataPlan.excludedApps.contains(it.uid) }.sumOf { it.total }

        return stats
    }

    fun getNetworkDataForType(startStamp: Long, endStamp: Long, subscriberId: String?, type: DataType): List<UsageData> {
        networkStatsManager.querySummary(type.queryIndex ?: return listOf(), subscriberId, startStamp, endStamp).use { summary ->
            val list = mutableListOf<UsageData>()
            while (summary.hasNextBucket()) {
                val bucket = NetworkStats.Bucket()
                summary.getNextBucket(bucket)
                val item = list.find { it.uid == bucket.uid }
                item?.let {
                    list.add(UsageData(it.upload + bucket.txBytes, it.download + bucket.rxBytes, bucket.uid))
                    list.remove(item)
                } ?: list.add(UsageData(bucket.txBytes, bucket.rxBytes, bucket.uid))
            }
            return list.toList()
        }
    }

    fun getNetworkDataForTypeHourly(
        startStamp: Long,
        endStamp: Long,
        subscriberId: String?,
        type: DataType,
        uid: Int?
    ): List<UsageData> {
        if (uid == null) {
            networkStatsManager.queryDetails(type.queryIndex ?: return listOf(), subscriberId, startStamp, endStamp)
        } else {
            networkStatsManager.queryDetailsForUid(type.queryIndex ?: return listOf(), subscriberId, startStamp, endStamp, uid)
        }.use { summary ->
            val list = mutableListOf<UsageData>()
            while (summary.hasNextBucket()) {
                val bucket = NetworkStats.Bucket()
                summary.getNextBucket(bucket)
                val end = fromTimestamp(bucket.endTimeStamp)
                val start = fromTimestamp(bucket.startTimeStamp)
                val item = list.find { it.start == start }

                item?.let {
                    list.add(UsageData(
                        upload = it.upload + bucket.txBytes,
                        download = it.download + bucket.rxBytes,
                        start = start,
                        end = end
                    ))
                    list.remove(item)
                } ?: list.add(UsageData(
                    upload = bucket.txBytes,
                    download = bucket.rxBytes,
                    start = start,
                    end = end
                ))
            }
            return list.toList()
        }
    }

    fun getAllAppUsage(dateParams: DateParams, query1: UsageQuery, query2: UsageQuery): Flow<List<AppUsage>> =
        flow {
            val dates = dateParams.getStartEndDates()
            val startTime = dates.first.atStartOfDay().toTimestamp()
            val endTime = dates.second.atStartOfDay().toTimestamp()

            val usage1 = getNetworkDataForType(startTime, endTime, null, query1.dataType)
            val usage2 = getNetworkDataForType(startTime, endTime, null, query2.dataType)

            val uids = usage1.map { it.uid }.union(usage2.map { it.uid }).union(specialUIDs).filterNotNull()

            val list = uids.map { uid ->
                val uid1 = usage1.find { it.uid == uid } ?: UsageData()
                val uid2 = usage2.find { it.uid == uid } ?: UsageData()
                AppUsage(
                    app = appManager.getAppForUID(uid),
                    usage = DayUsage(
                        date = dateParams.day,
                        usage1 = uid1.forDirection(query1.dataDirection),
                        usage2 = uid2.forDirection(query2.dataDirection)
                    ),
                )
            }.toMutableList()

            val totalUsage = DayUsage(
                date = dateParams.day,
                usage1 = totalDayUsage(query1, dates.first, dates.second),
                usage2 = totalDayUsage(query2, dates.first, dates.second)
            )

            list.sortByDescending { it.usage.totalUsage }
            list.add(0, AppUsage(
                app = allApp,
                usage = totalUsage
            ))
            list.sortWith(compareBy { it.app == unknownApp })
            list.removeAll { it.usage.totalUsage == 0L }
            emit(list.distinctBy { it.app.uid }.toList())
        }.flowOn(Dispatchers.IO)

    fun getAllHourUsage(dateParams: DateParams, query1: UsageQuery, query2: UsageQuery): Flow<List<HourUsage>> =
        flow {
            val (startDate, endDate) = dateParams.getStartEndDates()
            val startTime = startDate.atStartOfDay().toTimestamp()
            val endTime = endDate.atStartOfDay().toTimestamp()

            val usage1 = getNetworkDataForTypeHourly(startTime, endTime, null, query1.dataType, query1.dataUID.uidQuery)
            val usage2 = getNetworkDataForTypeHourly(startTime, endTime, null, query2.dataType, query2.dataUID.uidQuery)

            val twoHoursMilli = 2 * 60 * 60 * 1000L
            val slots = 0..11
            val map1 = slots.associateWith { 0L }.toMutableMap()
            val map2 = slots.associateWith { 0L }.toMutableMap()

            for ((usageList, query, map) in listOf(
                Triple(usage1, query1, map1),
                Triple(usage2, query2, map2),
            )) {
                usageList.forEach { usage ->
                    val stampStart = usage.start.toTimestamp() - startTime
                    val stampEnd = usage.end.toTimestamp() - startTime
                    val bucket1 = ((stampStart - stampStart % twoHoursMilli) / twoHoursMilli).toInt() % 12
                    val bucket2 = ((stampEnd - stampEnd % twoHoursMilli) / twoHoursMilli).toInt() % 12
                    val bucket2ratio = (stampStart % twoHoursMilli) / twoHoursMilli.toFloat()

                    map[bucket1] = map.getValue(bucket1) + (usage.forDirection(query.dataDirection) * (1-bucket2ratio)).toLong()
                    map[bucket2] = map.getValue(bucket2) + (usage.forDirection(query.dataDirection) * bucket2ratio).toLong()
                }
            }

            val start = fromTimestamp(startTime)
            val result = (0..11).map { i ->
                HourUsage(
                    start = start.withHour(i * 2),
                    end = start.withHour(i * 2).plusHours(2),
                    usage = DayUsage(dateParams.day, map1.getValue(i), map2.getValue(i)),
                )
            }

            emit(result)
        }.flowOn(Dispatchers.IO)

    fun daysUsage(
        startDate: LocalDate,
        endDate: LocalDate,
        usageQuery1: UsageQuery?,
        usageQuery2: UsageQuery? = null
    ): Flow<List<ScrollableBarData>> = flow {
        val data: MutableList<ScrollableBarData> = mutableListOf()
        val range = startDate.toEpochDay()..<endDate.toEpochDay()

        for (i in range) {
            val now = LocalDate.ofEpochDay(i)
            data.add(ScrollableBarData(now))
        }
        emit(data.toList())
        for (i in 0..<data.size) {
            val now = LocalDate.ofEpochDay(i + startDate.toEpochDay())
            val usage1 = usageQuery1?.let { totalDayUsage(it, now) }
            val usage2 = usageQuery2?.let { totalDayUsage(it, now) }
            data[i] = data[i].copy(
                y1 = usage1?.toDouble() ?: 0.0,
                y2 = usage2?.toDouble() ?: 0.0,
            )
        }
        emit(data.toList())
    }.flowOn(Dispatchers.IO)

    fun weekUsage(): Flow<List<BarData>> = flow {
        val field = WeekFields.of(Locale.getDefault())
        val firstDay = field.firstDayOfWeek
        val data: MutableList<BarData> = MutableList(7) { i ->
            val x = firstDay.plus(i.toLong()).getName(TextStyle.SHORT_STANDALONE)
            BarData(x, 0.0, 0.0)
        }
        val now = LocalDate.now()
        val daysPassed = now.get(field.dayOfWeek()) - 1

        for (i in 0..daysPassed) {
            val usage1 = totalDayUsage(
                startDate = now.minusDays(i.toLong()),
                query = UsageQuery(DataType.Mobile),
            )

            val usage2 = totalDayUsage(
                startDate = now.minusDays(i.toLong()),
                query = UsageQuery(DataType.Wifi),
            )

            data[daysPassed - i] += BarData(
                "",
                usage1.toDouble(),
                usage2.toDouble()
            )
        }
        emit(data.toList())
    }.flowOn(Dispatchers.IO)

    suspend fun predictUsage(): Double = withContext(Dispatchers.IO) {
        val hour = LocalDateTime.now().hour
        val hoursLeft = 23 - hour
        val nowStamp = LocalDateTime.now().toTimestamp()
        val last24HourUsage = getNetworkDataForType(nowStamp - 24 * 3_600_000, nowStamp, null, DataType.Mobile).sumOf { it.total }.toDouble()
        val todayUsage = totalDayUsage(UsageQuery(DataType.Mobile), LocalDate.now()).toDouble()

        var hourSum = 0.0
        var daySum = 0.0

        for (i in 1..4) {
            val pivotStamp = nowStamp - i * 24 * 7 * 3_600_000
            val futureHours = getNetworkDataForType(pivotStamp, pivotStamp + hoursLeft * 3_600_000, null, DataType.Mobile)
                .sumOf { it.total }.toDouble()
            val pastHours = getNetworkDataForType(pivotStamp - 24 * 3_600_000, pivotStamp, null, DataType.Mobile)
                .sumOf { it.total }.toDouble()

            daySum += futureHours + pastHours
            hourSum += pastHours
        }

        if (hourSum == 0.0) {
            return@withContext todayUsage
        }
        val multiplier = daySum / hourSum
        return@withContext last24HourUsage * (multiplier - 1) + todayUsage
    }

    suspend fun getTrend(): Double = withContext(Dispatchers.IO) {
        val nowStamp = LocalDateTime.now().toTimestamp()
        // Last 24 hours
        val hourAverage24 = getNetworkDataForType(nowStamp - 24 * 3_600_000, nowStamp, null, DataType.Mobile).sumOf { it.total } / 24.0
        // Last week average excluding last 24 hours
        val weekAverage = getNetworkDataForType(nowStamp - 168 * 3_600_000, nowStamp - 24 * 3_600_000, null, DataType.Mobile).sumOf { it.total } / 144.0

        return@withContext (hourAverage24 / max(weekAverage, 1.0) - 1) * 100.0
    }

    companion object {
        const val NULL_SUBSCRIBER = "null"
    }
}