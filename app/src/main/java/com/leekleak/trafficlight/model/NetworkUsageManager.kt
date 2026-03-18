package com.leekleak.trafficlight.model

import android.app.usage.NetworkStats
import android.app.usage.NetworkStatsManager
import android.net.ConnectivityManager
import com.leekleak.trafficlight.charts.model.BarData
import com.leekleak.trafficlight.charts.model.ScrollableBarData
import com.leekleak.trafficlight.database.AppUsage
import com.leekleak.trafficlight.database.DataPlan
import com.leekleak.trafficlight.database.DayUsage
import com.leekleak.trafficlight.database.HistoricalData
import com.leekleak.trafficlight.database.HistoricalDataDao
import com.leekleak.trafficlight.database.HourData
import com.leekleak.trafficlight.database.TimeInterval
import com.leekleak.trafficlight.model.AppManager.Companion.specialUIDs
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
    val uid: Int? = null
) {
    val total: Long
        get() = upload + download
}

class NetworkUsageManager(
    private var networkStatsManager: NetworkStatsManager,
    private val permissionManager: PermissionManager,
    private val historicalDataDao: HistoricalDataDao,
    private val appManager: AppManager,
) {
    fun calculateDayUsageBasic(startDate: LocalDate, endDate: LocalDate = startDate, uid: Int? = null): DayUsage {
        val startStamp = startDate.atStartOfDay().truncatedTo(ChronoUnit.DAYS).toTimestamp()
        val endStamp = endDate.plusDays(1).atStartOfDay().truncatedTo(ChronoUnit.DAYS).toTimestamp()
        val stats = calculateHourData(startStamp, endStamp, uid)
        return DayUsage(startDate, HourData(), stats.wifi, stats.cellular)
    }

    fun todayMobileUsage() = flow {
        emit(calculateDayUsageBasic(LocalDate.now(), LocalDate.now()).totalCellular)
    }.flowOn(Dispatchers.IO)

    fun planUsage(dataPlan: DataPlan): DayUsage {
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

        var stats = getNetworkDataForType(startStamp, endStamp, subscriberId, NETWORK_TYPE_MOBILE).sumOf { it.total }
        stats -= getNetworkDataForType(startStamp, endStamp, subscriberId, NETWORK_TYPE_MOBILE)
            .filter { dataPlan.excludedApps.contains(it.uid) }.sumOf { it.total }

        return DayUsage(startDate.toLocalDate(), HourData(), 0, stats)
    }

    fun calculateHourData(startTime: Long, endTime: Long, uid: Int? = null, subscriberId: String? = null): HourData {
        val mobileData = getNetworkDataForType(startTime, endTime, subscriberId, NETWORK_TYPE_MOBILE)
        val wifiData = getNetworkDataForType(startTime, endTime, subscriberId, NETWORK_TYPE_WIFI)
        if (uid == null) {
            return HourData(
                upload = mobileData.sumOf { it.upload } + wifiData.sumOf { it.upload },
                download = mobileData.sumOf { it.download } + wifiData.sumOf { it.download },
                wifi = wifiData.sumOf { it.total },
                cellular = mobileData.sumOf { it.total }
            )
        }
        val mobileUsage =  mobileData.find { it.uid == uid } ?: UsageData()
        val wifiUsage = wifiData.find { it.uid == uid } ?: UsageData()
        return HourData(
            upload = mobileUsage.upload + wifiUsage.upload,
            download = mobileUsage.download + wifiUsage.download,
            wifi = wifiUsage.total,
            cellular = mobileUsage.total
        )
    }

    fun getNetworkDataForType(startStamp: Long, endStamp: Long, subscriberId: String?, type: Int): List<UsageData> {
        networkStatsManager.querySummary(type, subscriberId, startStamp, endStamp).use { summary ->
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

    fun getAllAppUsage(startDate: LocalDate, endDate: LocalDate = startDate): Flow<List<AppUsage>> =
        flow {
            coroutineScope {
                val startTime = startDate.atStartOfDay().toTimestamp()
                val endTime = endDate.plusDays(1).atStartOfDay().toTimestamp()

                val mobileData = getNetworkDataForType(startTime, endTime, null, NETWORK_TYPE_MOBILE)
                val wifiData = getNetworkDataForType(startTime, endTime, null, NETWORK_TYPE_WIFI)
                val uids = mobileData.map { it.uid }.union(wifiData.map { it.uid }).union(specialUIDs).filterNotNull()

                val list = uids.map { uid ->
                    val uidMobile = mobileData.find { it.uid == uid } ?: UsageData()
                    val uidWifi = wifiData.find { it.uid == uid } ?: UsageData()
                    val name = appManager.getNameForUID(uid)
                    val packageName = appManager.getPackageNamesForUID(uid)?.firstOrNull()
                    if (name == null || packageName == null) return@map null
                    AppUsage(
                        usage = DayUsage(
                            hours = HourData(
                                upload = uidMobile.upload + uidWifi.upload,
                                download = uidMobile.download + uidWifi.download,
                                wifi = uidWifi.total,
                                cellular = uidMobile.total
                            ),
                            date = startDate,
                            totalWifi = uidWifi.total,
                            totalCellular = uidMobile.total
                        ),
                        uid = uid,
                        name = name,
                        packageName = packageName,
                        drawableResource = appManager.getDrawableResourceForUID(uid)
                    )
                }.filterNotNull().toMutableList()

                list.removeAll { it.usage.totalCellular + it.usage.totalWifi == 0L }
                list.sortByDescending { it.usage.totalCellular + it.usage.totalWifi }
                emit(list.distinctBy { it.uid }.toList())
            }
        }.flowOn(Dispatchers.IO)

    fun daysUsage(startDate: LocalDate, endDate: LocalDate): Flow<List<ScrollableBarData>> = flow {
        val data: MutableList<ScrollableBarData> = mutableListOf()
        val range = startDate.toEpochDay()..<endDate.toEpochDay()

        for (i in range) {
            val now = LocalDate.ofEpochDay(i)
            data.add(ScrollableBarData(now))
        }
        emit(data.toList())
        for (i in 0..<data.size) {
            val now = LocalDate.ofEpochDay(i + startDate.toEpochDay())
            val usage = calculateDayUsageBasic(now)
            data[i] = data[i].copy(
                y1 = usage.totalCellular.toDouble(),
                y2 = usage.totalWifi.toDouble()
            )
        }
        emit(data.toList())
    }.flowOn(Dispatchers.IO)

    fun weekUsage(): Flow<List<BarData>> = flow {
        val firstDay = WeekFields.of(Locale.getDefault()).firstDayOfWeek
        val data: MutableList<BarData> = MutableList(7) { i ->
            val x = firstDay.plus(i.toLong()).getName(TextStyle.SHORT_STANDALONE)
            BarData(x, 0.0, 0.0)
        }
        val now = LocalDate.now()
        val daysPassed = (8 - firstDay.value) % 7 + now.dayOfWeek.value

        for (i in 0..<daysPassed) {
            val usage = calculateDayUsageBasic(now.minusDays(i.toLong()))

            data[daysPassed - i - 1] += BarData(
                "",
                usage.totalCellular.toDouble(),
                usage.totalWifi.toDouble()
            )
        }
        emit(data.toList())
    }.flowOn(Dispatchers.IO)

    fun predictUsage(): Flow<Double> = flow {
        populateHistoryCache()

        val hour = LocalDateTime.now().hour
        val hoursLeft = 23 - hour
        val nowStamp = LocalDateTime.now().toTimestamp() + 3_600_000
        val last24HourUsage = getNetworkDataForType(nowStamp - 24 * 3_600_000, nowStamp, null, NETWORK_TYPE_MOBILE).sumOf { it.total }.toDouble()
        val todayUsage = calculateDayUsageBasic(LocalDate.now(), LocalDate.now()).totalCellular.toDouble()
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
        val last24HourAverage = getNetworkDataForType(nowStamp - 24 * 3_600_000, nowStamp, null, NETWORK_TYPE_MOBILE).sumOf { it.total } / 24.0
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
            val bucket = networkStatsManager.querySummaryForDevice(NETWORK_TYPE_MOBILE, null, stamp - 3_600_000, stamp)
            historicalDataDao.add(
                HistoricalData(
                    stamp = stamp,
                    usage = bucket.rxBytes + bucket.txBytes,
                )
            )
        }
    }

    companion object {
        const val NETWORK_TYPE_MOBILE = ConnectivityManager.TYPE_MOBILE
        const val NETWORK_TYPE_WIFI = ConnectivityManager.TYPE_WIFI
        const val NULL_SUBSCRIBER = "null"
    }
}