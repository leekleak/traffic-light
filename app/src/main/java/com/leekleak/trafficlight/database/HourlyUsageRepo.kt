package com.leekleak.trafficlight.database

import android.app.usage.NetworkStats
import android.app.usage.NetworkStatsManager
import android.content.Context
import android.content.Context.NETWORK_STATS_SERVICE
import android.content.pm.PackageManager
import android.os.Build
import com.leekleak.trafficlight.services.PermissionManager
import com.leekleak.trafficlight.util.NetworkType
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
import timber.log.Timber
import java.time.LocalDate
import java.time.temporal.ChronoUnit

enum class UsageMode {
    Unlimited,
    NoPermission,
    Limited
}

class HourlyUsageRepo(context: Context) : KoinComponent {
    private var networkStatsManager: NetworkStatsManager = context.getSystemService(NETWORK_STATS_SERVICE) as NetworkStatsManager
    private var packageManager: PackageManager = context.packageManager
    private val permissionManager: PermissionManager by inject()

    fun usageModeFlow(): Flow<UsageMode> = permissionManager.usagePermissionFlow.map {
        val millis = System.currentTimeMillis()

        if (!it) UsageMode.NoPermission
        else if (calculateHourData(millis - 2_592_000_000L, millis).total == 0L) UsageMode.Limited
        else UsageMode.Unlimited
    }

    fun calculateDayUsage(date: LocalDate): DayUsage {
        val dayStamp = date.atStartOfDay().truncatedTo(ChronoUnit.DAYS).toTimestamp()
        val hours: MutableMap<Long, HourData> = mutableMapOf()

        for (k in 0..11) {
            val globalHour = dayStamp + k * 3_600_000L * 2
            hours[k * 2L] = calculateHourData(globalHour, globalHour + 3_600_000L * 2)
        }

        return DayUsage(date, hours).also { it.categorizeUsage() }
    }

    fun calculateDayUsageFlow(date: LocalDate): Flow<DayUsage> = flow {
        val dayStamp = date.atStartOfDay().truncatedTo(ChronoUnit.DAYS).toTimestamp()
        val hours: MutableMap<Long, HourData> = mutableMapOf()

        for (k in 0..11) {
            val globalHour = dayStamp + k * 3_600_000L * 2
            hours[k * 2L] = calculateHourData(globalHour, globalHour + 3_600_000L * 2)
        }
        emit(DayUsage(date, hours).also { it.categorizeUsage() })
    }.flowOn(Dispatchers.Default)

    fun calculateDayUsageBasic(date: LocalDate): DayUsage {
        val dayStamp = date.atStartOfDay().truncatedTo(ChronoUnit.DAYS).toTimestamp()
        val stats = calculateHourData(dayStamp, dayStamp + 3_600_000L * 24)
        return DayUsage(date, mutableMapOf(), stats.wifi, stats.cellular)
    }

    fun calculateHourData(startTime: Long, endTime: Long): HourData {
        val statsWifi = networkStatsManager.querySummaryForDevice(NetworkType.Wifi.ordinal, null, startTime, endTime)
        val statsMobile = networkStatsManager.querySummaryForDevice(NetworkType.Cellular.ordinal, null, startTime, endTime)

        val hourData = HourData()
        statsMobile?.let {
            hourData.cellular += it.txBytes + it.rxBytes
            hourData.upload += it.txBytes
            hourData.download += it.rxBytes
        }

        statsWifi?.let {
            hourData.wifi += it.txBytes + it.rxBytes
            hourData.upload += it.txBytes
            hourData.download += it.rxBytes
        }
        return hourData
    }

    suspend fun getAllAppUsage(date: LocalDate): List<AppUsage> = coroutineScope {
        var time = System.currentTimeMillis()
        val apps = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            packageManager.getInstalledApplications(PackageManager.ApplicationInfoFlags.of(0L))
        } else {
            packageManager.getInstalledApplications(0)
        }
        Timber.e("Took ${System.currentTimeMillis() - time}ms to get apps")
        val jobs = apps.map { app ->
            async(Dispatchers.IO) {
                val dayUsage = calculateAppDayUsage(date, app.uid)
                return@async AppUsage(
                    usage = dayUsage,
                    uid = app.uid,
                    name = app.loadLabel(packageManager).toString(),
                    icon = app.icon,
                    drawable = app.loadIcon(packageManager)
                )
            }
        }

        val list = jobs.awaitAll().toMutableList()
        list.removeAll { it.usage.totalCellular + it.usage.totalWifi == 0L }
        list.sortByDescending { it.usage.totalCellular + it.usage.totalWifi }
        Timber.e("Took ${System.currentTimeMillis() - time}ms to do everything")
        return@coroutineScope list.toList()
    }

    fun calculateAppDayUsage(date: LocalDate, uid: Int): DayUsage {
        val dayStamp = date.atStartOfDay().truncatedTo(ChronoUnit.DAYS).toTimestamp()
        val stats = calculateAppHourData(dayStamp, dayStamp + 3_600_000L * 24, uid)
        return DayUsage(date, mutableMapOf(), stats.wifi, stats.cellular)
    }

    fun calculateAppHourData(startTime: Long, endTime: Long, uid: Int): HourData {
        val statsWifi = networkStatsManager.queryDetailsForUid(NetworkType.Wifi.ordinal, null, startTime, endTime, uid)
        val statsMobile = networkStatsManager.queryDetailsForUid(NetworkType.Cellular.ordinal, null, startTime, endTime, uid)

        val hourData = HourData()
        val bucket = NetworkStats.Bucket()
        while (statsMobile.hasNextBucket()) {
            statsMobile.getNextBucket(bucket)
            bucket.let {
                hourData.cellular += it.txBytes + it.rxBytes
                hourData.upload += it.txBytes
                hourData.download += it.rxBytes
            }
        }

        while (statsWifi.hasNextBucket()) {
            statsWifi.getNextBucket(bucket)
            bucket.let {
                hourData.wifi += it.txBytes + it.rxBytes
                hourData.upload += it.txBytes
                hourData.download += it.rxBytes
            }
        }
        return hourData
    }
}