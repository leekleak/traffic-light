package com.leekleak.trafficlight.database

import android.app.usage.NetworkStatsManager
import android.content.Context
import android.content.Context.NETWORK_STATS_SERVICE
import com.leekleak.trafficlight.services.UsageService
import com.leekleak.trafficlight.util.NetworkType
import com.leekleak.trafficlight.util.toTimestamp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import org.koin.core.component.KoinComponent
import java.time.LocalDate
import java.time.temporal.ChronoUnit

class HourlyUsageRepo(context: Context) : KoinComponent {
    private var networkStatsManager: NetworkStatsManager = context.getSystemService(NETWORK_STATS_SERVICE) as NetworkStatsManager

    fun limitedMode(): Flow<Boolean> = UsageService.todayUsageFlow.map {
        // Check whether there's any usage over the past month
        calculateHourData(System.currentTimeMillis() - 2_592_000_000L, System.currentTimeMillis()).total == 0L
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
}