package com.leekleak.trafficlight.database

import android.app.usage.NetworkStats
import android.app.usage.NetworkStatsManager
import android.content.Context
import android.content.Context.NETWORK_STATS_SERVICE
import com.leekleak.trafficlight.charts.model.BarData
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

    fun singleDayUsageFlow(date: LocalDate): Flow<DayUsage> = flow {
        emit(singleDayUsage(date))
    }.flowOn(Dispatchers.IO)

    fun singleDayUsageFlowBar(date: LocalDate): Flow<List<BarData>> = flow {
        emit(dayUsageToBarData(singleDayUsage(date)))
    }.flowOn(Dispatchers.IO)


    fun calculateDayUsageBasic(date: LocalDate, uid: Int? = null): DayUsage {
        val dayStamp = date.atStartOfDay().truncatedTo(ChronoUnit.DAYS).toTimestamp()
        val stats = calculateHourData(dayStamp, dayStamp + 3_600_000L * 24, uid)
        return DayUsage(date, mutableMapOf(), stats.wifi, stats.cellular)
    }

    fun calculateHourData(startTime: Long, endTime: Long, uid: Int? = null): HourData {
        val wifiBuckets = mutableListOf<NetworkStats.Bucket>()
        val mobileBuckets = mutableListOf<NetworkStats.Bucket>()

        if (uid == null) {
            wifiBuckets.add(
                networkStatsManager.querySummaryForDevice(0, null, startTime, endTime)
            )
            mobileBuckets.add(
                networkStatsManager.querySummaryForDevice(1,null, startTime, endTime)
            )
        } else {
            val statsWifi = networkStatsManager.queryDetailsForUid(0, null, startTime, endTime, uid)
            val statsMobile = networkStatsManager.queryDetailsForUid(1, null, startTime, endTime, uid)
            while (statsWifi.hasNextBucket()) {
                val bucket = NetworkStats.Bucket()
                statsWifi.getNextBucket(bucket)
                wifiBuckets.add(bucket)
            }
            while (statsMobile.hasNextBucket()) {
                val bucket = NetworkStats.Bucket()
                statsMobile.getNextBucket(bucket)
                mobileBuckets.add(bucket)
            }
        }

        val hourData = HourData()
        for (bucket in wifiBuckets) {
            hourData.cellular += bucket.txBytes + bucket.rxBytes
            hourData.upload += bucket.txBytes
            hourData.download += bucket.rxBytes
        }

        for (bucket in mobileBuckets) {
            hourData.wifi += bucket.txBytes + bucket.rxBytes
            hourData.upload += bucket.txBytes
            hourData.download += bucket.rxBytes
        }
        return hourData
    }

    suspend fun getAllAppUsage(date: LocalDate): List<AppUsage> = coroutineScope {
        val jobs = appDatabase.suspiciousApps.map { app ->
            async(Dispatchers.IO) {
                val dayUsage = calculateDayUsageBasic(date, app.uid)
                return@async AppUsage(
                    usage = dayUsage,
                    uid = app.uid,
                    name = appDatabase.getLabel(app),
                    icon = app.icon,
                    drawable = appDatabase.getIcon(app),
                    appInfo = app
                )
            }
        }

        val list = jobs.awaitAll().toMutableList()
        list.removeAll { it.usage.totalCellular + it.usage.totalWifi == 0L }
        list.sortByDescending { it.usage.totalCellular + it.usage.totalWifi }
        return@coroutineScope list.map { it }
    }

    fun daysUsage(startDate: LocalDate, endDate: LocalDate): Flow<List<BarData>> = flow {
        val data: MutableList<BarData> = mutableListOf()
        for (i in startDate.toEpochDay()..endDate.toEpochDay()) {
            val now = LocalDate.ofEpochDay(i)
            val usage = calculateDayUsageBasic(now)

            data.add(
                BarData(
                    "",
                    usage.totalCellular.toDouble(),
                    usage.totalWifi.toDouble()
                )
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