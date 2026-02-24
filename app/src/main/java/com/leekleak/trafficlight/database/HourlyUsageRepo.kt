package com.leekleak.trafficlight.database

import android.app.usage.NetworkStats
import android.app.usage.NetworkStatsManager
import android.content.Context
import android.content.Context.NETWORK_STATS_SERVICE
import com.leekleak.trafficlight.charts.model.BarData
import com.leekleak.trafficlight.charts.model.ScrollableBarData
import com.leekleak.trafficlight.model.AppDatabase
import com.leekleak.trafficlight.services.PermissionManager
import com.leekleak.trafficlight.util.fromTimestamp
import com.leekleak.trafficlight.util.getName
import com.leekleak.trafficlight.util.padHour
import com.leekleak.trafficlight.util.toTimestamp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.TextStyle
import java.time.temporal.ChronoUnit

enum class UsageMode {
    Unlimited,
    NoPermission,
    Limited
}

data class UsageData(
    val upload: Long,
    val download: Long,
) {
    val total: Long
        get() = upload + download
}

class HourlyUsageRepo(context: Context) : KoinComponent {
    private var networkStatsManager: NetworkStatsManager = context.getSystemService(NETWORK_STATS_SERVICE) as NetworkStatsManager
    private val permissionManager: PermissionManager by inject()
    private val appDatabase: AppDatabase by inject()

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
        return DayUsage(startDate, mutableMapOf(), stats.wifi, stats.cellular)
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

        var stats = getNetworkDataForType(startStamp, endStamp, null, dataPlan.subscriberID, NETWORK_TYPE_MOBILE).total

        for (uid in dataPlan.excludedApps) {
            stats -= getNetworkDataForType(startStamp, endStamp, uid, dataPlan.subscriberID, NETWORK_TYPE_MOBILE).total
        }

        return DayUsage(startDate.toLocalDate(), mutableMapOf(), 0, stats)
    }

    fun calculateHourData(startTime: Long, endTime: Long, uid: Int? = null, subscriberId: String? = null): HourData {
        val mobileData = getNetworkDataForType(startTime, endTime, uid, subscriberId, NETWORK_TYPE_MOBILE)
        val wifiData = getNetworkDataForType(startTime, endTime, uid, subscriberId, NETWORK_TYPE_WIFI)
        return HourData(
            upload = mobileData.upload + wifiData.upload,
            download = mobileData.download + wifiData.download,
            wifi = wifiData.total,
            cellular = mobileData.total
        )
    }

    fun getNetworkDataForType(startStamp: Long, endStamp: Long, uid: Int?, subscriberId: String?, type: Int): UsageData {
        if (uid == null) {
            val bucket = networkStatsManager.querySummaryForDevice(type, subscriberId, startStamp, endStamp)
            return UsageData(bucket.txBytes, bucket.rxBytes)
        } else {
            val stats = networkStatsManager.queryDetailsForUid(type, subscriberId, startStamp, endStamp, uid)
            var totalUp = 0L
            var totalDown = 0L
            while (stats.hasNextBucket()) {
                val bucket = NetworkStats.Bucket()
                stats.getNextBucket(bucket)
                totalUp += bucket.txBytes
                totalDown += bucket.rxBytes
            }
            return UsageData(totalUp, totalDown)
        }
    }

    fun getAllAppUsage(startDate: LocalDate, endDate: LocalDate = startDate): Flow<List<AppUsage>> =
        flow {
            coroutineScope {
                val requestSemaphore = Semaphore(permits = 3)
                val jobs = appDatabase.suspiciousApps.map { app ->
                    async(Dispatchers.IO) {
                        requestSemaphore.withPermit {
                            val dayUsage = calculateDayUsageBasic(startDate, endDate, app.uid)
                            return@async AppUsage(
                                usage = dayUsage,
                                uid = app.uid,
                                name = appDatabase.getLabel(app),
                                packageName = app.packageName,
                            )
                        }
                    }
                }

                val list = jobs.awaitAll().toMutableList()
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

    fun todayUsage(): Flow<List<BarData>> = flow {
        val data: MutableList<BarData> = MutableList(12) { i ->
            val x = padHour(i * 2)
            BarData(x, 0.0, 0.0)
        }
        val now = LocalDate.now().atStartOfDay()

        for (i in 0..11) {
            val startStamp = now.plusHours(i * 2L).toTimestamp()
            val endStamp = now.plusHours((i + 1L) * 2).toTimestamp()
            val wifi = getNetworkDataForType(startStamp, endStamp, null, null, NETWORK_TYPE_WIFI)
            val mobile = getNetworkDataForType(startStamp, endStamp, null, null, NETWORK_TYPE_MOBILE)

            data[i] += BarData(
                "",
                mobile.total.toDouble(),
                wifi.total.toDouble()
            )
        }
        emit(data.toList())
    }.flowOn(Dispatchers.IO)

    fun singleDayUsage(date: LocalDate): DayUsage {
        val dayStamp = date.atStartOfDay().truncatedTo(ChronoUnit.DAYS).toTimestamp()
        val hours: MutableMap<Long, HourData> = mutableMapOf()

        for (k in 0..11) {
            val globalHour = dayStamp + k * 3_600_000L * 2
            hours[k * 2L] = calculateHourData(globalHour, globalHour + 3_600_000L * 2)
        }

        return DayUsage(date, hours).also { it.categorizeUsage() }
    }

    companion object {
        const val NETWORK_TYPE_MOBILE = 0
        const val NETWORK_TYPE_WIFI = 1
    }
}