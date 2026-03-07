package com.leekleak.trafficlight.database

import android.app.usage.NetworkStats
import android.app.usage.NetworkStatsManager
import com.leekleak.trafficlight.charts.model.BarData
import com.leekleak.trafficlight.charts.model.ScrollableBarData
import com.leekleak.trafficlight.model.AppDatabase
import com.leekleak.trafficlight.model.AppDatabase.Companion.specialUIDs
import com.leekleak.trafficlight.model.HoltWintersTripleExponential
import com.leekleak.trafficlight.services.PermissionManager
import com.leekleak.trafficlight.util.fromTimestamp
import com.leekleak.trafficlight.util.getName
import com.leekleak.trafficlight.util.toTimestamp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.format.TextStyle
import java.time.temporal.ChronoUnit

enum class UsageMode {
    Unlimited,
    NoPermission,
    Limited
}

data class UsageData(
    val upload: Long = 0,
    val download: Long = 0,
    val uid: Int? = null
) {
    val total: Long
        get() = upload + download
}

class HourlyUsageRepo(
    private var networkStatsManager: NetworkStatsManager,
    private val permissionManager: PermissionManager,
    private val historicalDataDao: HistoricalDataDao,
    private val appDatabase: AppDatabase,
) : KoinComponent {

    fun usageModeFlow(): Flow<UsageMode> = permissionManager.usagePermissionFlow.map {
        val millis = System.currentTimeMillis()

        if (!it) UsageMode.NoPermission
        else if (calculateHourData(millis - 2_592_000_000L, millis).total == 0L) UsageMode.Limited
        else UsageMode.Unlimited
    }

    fun calculateDayUsageBasic(startDate: LocalDate, endDate: LocalDate = startDate, uid: Int? = null): DayUsage {
        val startStamp = startDate.atStartOfDay().truncatedTo(ChronoUnit.DAYS).toTimestamp()
        val endStamp = endDate.plusDays(1).atStartOfDay().truncatedTo(ChronoUnit.DAYS).toTimestamp()
        val stats = calculateHourData(startStamp, endStamp, uid)
        return DayUsage(startDate, HourData(), stats.wifi, stats.cellular)
    }

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
            else -> throw Exception("Unsupported time interval")
        }

        val startStamp = startDate.toTimestamp()
        val endStamp = now.toTimestamp()
        val subscriberId = if (dataPlan.subscriberID == "null") null else dataPlan.subscriberID

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
        val stats = networkStatsManager.querySummary(type, subscriberId, startStamp, endStamp)
        val list = mutableListOf<UsageData>()
        while (stats.hasNextBucket()) {
            val bucket = NetworkStats.Bucket()
            stats.getNextBucket(bucket)
            val item = list.find { it.uid == bucket.uid }
            item?.let {
                list.add(UsageData(it.upload + bucket.txBytes, it.download + bucket.rxBytes, bucket.uid))
                list.remove(item)
            } ?: list.add(UsageData(bucket.txBytes, bucket.rxBytes, bucket.uid))
        }
        return list.toList()
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
                    val name = appDatabase.getNameForUID(uid)
                    val packageName = appDatabase.getPackageNamesForUID(uid)?.firstOrNull()
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
                        drawableResource = appDatabase.getDrawableResourceForUID(uid)
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
        val data: MutableList<BarData> = MutableList(7) { i ->
            val x = DayOfWeek.entries[i].getName(TextStyle.SHORT_STANDALONE)
            BarData(x, 0.0, 0.0)
        }
        val now = LocalDate.now()

        for (i in 0..<now.dayOfWeek.value) {
            val usage = calculateDayUsageBasic(now.minusDays(i.toLong()))

            data[now.dayOfWeek.value - i - 1] += BarData(
                "",
                usage.totalCellular.toDouble(),
                usage.totalWifi.toDouble()
            )
        }
        emit(data.toList())
    }.flowOn(Dispatchers.IO)

    fun predictUsage(predictHours: Int): Flow<Double> = flow {
        val alpha = 0.04398965471871171
        val beta = 0.0006227714063965939
        val gamma = 0.033027304992884844
        val period = 24

        val data = historicalDataDao.getAll().map { it.usage }

        if (data.size < 1000) {
            emit(0.0)
        } else {
            val prediction: DoubleArray = HoltWintersTripleExponential.forecast(data.toLongArray(), alpha, beta, gamma, period, predictHours)
            emit(prediction.drop(data.size).sum())
        }
    }.flowOn(Dispatchers.IO)

    fun populateHistoryCache() = CoroutineScope(Dispatchers.IO).launch {
        val lastHour = LocalDateTime.now(ZoneOffset.UTC).truncatedTo(ChronoUnit.HOURS)
        val n = 24 * 80

        for (i in n downTo 0) {
            val stamp = lastHour.minusHours(i.toLong()).toTimestamp()
            if (historicalDataDao.contains(stamp)) continue

            val data = getNetworkDataForType(stamp, stamp + 3_600_000, null, NETWORK_TYPE_MOBILE).sumOf { it.total }
            historicalDataDao.add(
                HistoricalData(
                    stamp = stamp,
                    usage = data,
                )
            )
        }
    }

    companion object {
        const val NETWORK_TYPE_MOBILE = 0
        const val NETWORK_TYPE_WIFI = 1
    }
}