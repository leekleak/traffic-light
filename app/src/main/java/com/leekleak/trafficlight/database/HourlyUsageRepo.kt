package com.leekleak.trafficlight.database

import android.app.usage.NetworkStats
import android.app.usage.NetworkStatsManager
import android.content.Context
import android.content.Context.NETWORK_STATS_SERVICE
import com.leekleak.trafficlight.charts.model.BarData
import com.leekleak.trafficlight.charts.model.ScrollableBarData
import com.leekleak.trafficlight.model.AppDatabase
import com.leekleak.trafficlight.services.PermissionManager
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
import java.time.format.TextStyle
import java.time.temporal.ChronoUnit

enum class UsageMode {
    Unlimited,
    NoPermission,
    Limited
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

    fun singleDayUsage(date: LocalDate): DayUsage {
        val dayStamp = date.atStartOfDay().truncatedTo(ChronoUnit.DAYS).toTimestamp()
        val hours: MutableMap<Long, HourData> = mutableMapOf()

        for (k in 0..11) {
            val globalHour = dayStamp + k * 3_600_000L * 2
            hours[k * 2L] = calculateHourData(globalHour, globalHour + 3_600_000L * 2)
        }

        return DayUsage(date, hours).also { it.categorizeUsage() }
    }

    fun calculateDayUsageBasic(startDate: LocalDate, endDate: LocalDate = startDate, uid: Int? = null): DayUsage {
        val startStamp = startDate.atStartOfDay().truncatedTo(ChronoUnit.DAYS).toTimestamp()
        val endStamp = endDate.plusDays(1).atStartOfDay().truncatedTo(ChronoUnit.DAYS).toTimestamp()
        val stats = calculateHourData(startStamp, endStamp, uid)
        return DayUsage(startDate, mutableMapOf(), stats.wifi, stats.cellular)
    }

    fun calculateHourData(startTime: Long, endTime: Long, uid: Int? = null): HourData {
        val mobileBuckets = mutableListOf<NetworkStats.Bucket>()
        val wifiBuckets = mutableListOf<NetworkStats.Bucket>()

        if (uid == null) {
            mobileBuckets.add(
                networkStatsManager.querySummaryForDevice(0, null, startTime, endTime)
            )
            wifiBuckets.add(
                networkStatsManager.querySummaryForDevice(1, null, startTime, endTime)
            )
        } else {
            val statsMobile = networkStatsManager.queryDetailsForUid(0, null, startTime, endTime, uid)
            val statsWifi = networkStatsManager.queryDetailsForUid(1, null, startTime, endTime, uid)
            while (statsMobile.hasNextBucket()) {
                val bucket = NetworkStats.Bucket()
                statsMobile.getNextBucket(bucket)
                mobileBuckets.add(bucket)
            }
            while (statsWifi.hasNextBucket()) {
                val bucket = NetworkStats.Bucket()
                statsWifi.getNextBucket(bucket)
                wifiBuckets.add(bucket)
            }
        }

        val hourData = HourData()
        for (bucket in mobileBuckets) {
            hourData.cellular += bucket.txBytes + bucket.rxBytes
            hourData.upload += bucket.txBytes
            hourData.download += bucket.rxBytes
        }

        for (bucket in wifiBuckets) {
            hourData.wifi += bucket.txBytes + bucket.rxBytes
            hourData.upload += bucket.txBytes
            hourData.download += bucket.rxBytes
        }
        return hourData
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

    companion object {
        fun dayUsageToBarData(usage: DayUsage): List<BarData> {
            val data: MutableList<BarData> = mutableListOf()
            val hours = usage.hours
            for (i in 0..22 step 2) {
                data.add(BarData(padHour(i), 0.0, 0.0))
            }

            if (hours.isNotEmpty()) {
                for (i in hours.entries) {
                    val ii = i.key.toInt() / 2
                    data[ii] = BarData(
                        padHour(ii * 2),
                        i.value.cellular.toDouble(),
                        i.value.wifi.toDouble()
                    )
                }
            }
            return data
        }
    }
}