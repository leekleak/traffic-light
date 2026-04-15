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
import com.leekleak.trafficlight.database.HistoricalData
import com.leekleak.trafficlight.database.HistoricalDataDao
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
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.format.TextStyle
import java.time.temporal.ChronoUnit
import java.time.temporal.WeekFields
import java.util.Locale

data class UsageData(
    val upload: Long = 0,
    val download: Long = 0,
    val uid: Int? = null,
    val start: LocalDateTime? = null,
    val end: LocalDateTime? = null
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
    private val historicalDataDao: HistoricalDataDao,
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
        networkStatsManager.querySummary(type.queryIndex, subscriberId, startStamp, endStamp).use { summary ->
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
            networkStatsManager.queryDetails(
                type.queryIndex,
                subscriberId,
                startStamp,
                endStamp
            )
        } else {
            networkStatsManager.queryDetailsForUid(
                type.queryIndex,
                subscriberId,
                startStamp,
                endStamp,
                uid
            )
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
            coroutineScope {
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
            }
        }.flowOn(Dispatchers.IO)

    fun getAllHourUsage(dateParams: DateParams, query1: UsageQuery, query2: UsageQuery): Flow<List<HourUsage>> =
        flow {
            coroutineScope {
                val date = dateParams.day
                val startTime = date.atStartOfDay().toTimestamp()
                val endTime = date.plusDays(1).atStartOfDay().toTimestamp()

                val usage1 = getNetworkDataForTypeHourly(startTime, endTime, null, query1.dataType, query1.dataUID.uidQuery)
                val usage2 = getNetworkDataForTypeHourly(startTime, endTime, null, query2.dataType, query2.dataUID.uidQuery)

                val times = usage1.map { it.start }.union(usage2.map { it.start }).filterNotNull()

                val list = times.map { time ->
                    val usage1 = usage1.find { it.start == time } ?: UsageData()
                    val usage2 = usage2.find { it.start == time } ?: UsageData()
                    HourUsage(
                        start = usage1.start ?: usage2.start ?: return@map null,
                        end = usage1.end ?: usage2.end ?: return@map null,
                        usage = DayUsage(
                            date = dateParams.day,
                            usage1 = usage1.forDirection(query1.dataDirection),
                            usage2 = usage2.forDirection(query2.dataDirection)
                        ),
                    )
                }.filterNotNull().sortedBy { it.start.hour }

                emit(list)
            }
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

    fun predictUsage(): Flow<Double> = flow {
        populateHistoryCache()

        val hour = LocalDateTime.now().hour
        val hoursLeft = 23 - hour
        val nowStamp = LocalDateTime.now().toTimestamp() + 3_600_000
        val last24HourUsage = getNetworkDataForType(nowStamp - 24 * 3_600_000, nowStamp, null, DataType.Mobile).sumOf { it.total }.toDouble()
        val todayUsage = totalDayUsage(UsageQuery(DataType.Mobile), LocalDate.now()).toDouble()
        val data = historicalDataDao.getAll().map { it.usage }

        if (data.size < 5 * 24 * 7) { emit(0.0); return@flow }

        var hourSum = 0.0
        var daySum = 0.0

        for (i in 1..4) {
            val offsetIndex = data.size - i * 24 * 7
            for (k in -24..hoursLeft) {
                daySum += data[offsetIndex + k]
                if (k <= 0) hourSum += data[offsetIndex + k]
            }
        }

        if (hourSum == 0.0) {
            emit(todayUsage)
            return@flow
        }
        val multiplier = daySum / hourSum
        emit(last24HourUsage * (multiplier - 1) + todayUsage)
    }.flowOn(Dispatchers.IO)

    fun getTrend(): Flow<Double> = flow {
        populateHistoryCache()

        val nowStamp = LocalDateTime.now().toTimestamp()
        val last24HourAverage = getNetworkDataForType(nowStamp - 24 * 3_600_000, nowStamp, null,
            DataType.Mobile
        ).sumOf { it.total } / 24.0
        val data = historicalDataDao.getAll().takeLast(24 * 7).map { it.usage }.average()

        emit((last24HourAverage / data - 1) * 100.0) // Return trend in percentage
    }.flowOn(Dispatchers.IO)

    fun populateHistoryCache() {
        val lastHour = LocalDateTime.now(ZoneOffset.UTC).truncatedTo(ChronoUnit.HOURS)
        val n = 24 * 80
        val allData = historicalDataDao.getAll()

        for (i in n downTo 0) {
            val stamp = lastHour.minusHours(i - 1L).toTimestamp()
            if (allData.find { it.stamp == stamp } != null && i > 1) continue

            // We need to use querySummaryForDevice because regular querySummary is not very accurate hour-wise
            val bucket = networkStatsManager.querySummaryForDevice(DataType.Mobile.queryIndex, null, stamp - 3_600_000, stamp)
            historicalDataDao.add(
                HistoricalData(
                    stamp = stamp,
                    usage = bucket.rxBytes + bucket.txBytes,
                )
            )
        }
    }

    companion object {
        const val NULL_SUBSCRIBER = "null"
    }
}