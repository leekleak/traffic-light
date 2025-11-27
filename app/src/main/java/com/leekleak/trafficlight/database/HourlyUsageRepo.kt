package com.leekleak.trafficlight.database

import android.app.usage.NetworkStatsManager
import android.content.Context
import android.content.Context.NETWORK_STATS_SERVICE
import com.leekleak.trafficlight.util.NetworkType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import timber.log.Timber
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit

class HourlyUsageRepo(context: Context) : KoinComponent {
    private val dao: HourlyUsageDao by inject()
    private var networkStatsManager: NetworkStatsManager? = null

    init {
        networkStatsManager = context.getSystemService(NETWORK_STATS_SERVICE) as NetworkStatsManager?
    }

    fun getDBSize(): Flow<Int> = dao.getDBSize()

    fun getUsage(startStamp: Long, endStamp: Long): Flow<List<HourUsage>> =
        dao.getUsageFlow(startStamp, endStamp)

    fun getLastDayWithData(): Flow<LocalDate> = dao.getLastUsage().map { hourUsage ->
        hourUsage?.let {
            Instant.ofEpochMilli(it.timestamp)
                .atZone(ZoneId.systemDefault().rules.getOffset(Instant.now()))
                .toLocalDate()
        } ?: LocalDate.now()
    }

    fun getMaxCombinedUsage(): Flow<Long> = dao.getMaxCombinedUsage()

    fun clearDB() {
        if (!populating) dao.clear()
    }

    var populating = false
    fun populateDb() {
        if (populating) return
        populating = true

        val suspiciousHours = mutableListOf<HourUsage>()
        val timezone = ZoneId.systemDefault().rules.getOffset(Instant.now())
        val date = LocalDate.now().atStartOfDay()
        val dayStamp = date.truncatedTo(ChronoUnit.DAYS).toInstant(timezone).toEpochMilli()

        for (i in 1..10000) {
            if (suspiciousHours.size == 31 * 24) {
                Timber.i("Reached maximum amount of empty hours")
                break
            }

            val hour = 3_600_000L
            val currentStamp = dayStamp - (i * hour)
            val hourData = calculateHourData(currentStamp, currentStamp + hour)

            if (
                dao.hourUsageExists(currentStamp) &&
                dao.getUsage(currentStamp, currentStamp + hour).first() == hourData.toHourUsage(currentStamp)
            )  {
                break
            }

            suspiciousHours.add(HourUsage(currentStamp,hourData.wifi, hourData.cellular))
            if (hourData.total != 0L) {
                for (hour in suspiciousHours) {
                    if (dao.hourUsageExists(hour.timestamp)) {
                        dao.updateHourUsage(hour)
                    } else {
                        dao.addHourUsage(hour)
                    }
                }
                suspiciousHours.clear()
            }
        }
        populating = false
    }

    fun calculateDayUsage(date: LocalDate): DayUsage {
        val timezone = ZoneId.systemDefault().rules.getOffset(Instant.now())
        val dayStamp = date.atStartOfDay().truncatedTo(ChronoUnit.DAYS).toInstant(timezone).toEpochMilli()
        val hours: MutableMap<Long, HourData> = mutableMapOf()

        for (k in 0..23) {
            val globalHour = dayStamp + k * 3_600_000L
            hours[globalHour] = calculateHourData(globalHour, globalHour + 3_600_000L)
        }

        return DayUsage(date, hours).also { it.categorizeUsage() }
    }

    fun calculateHourData(startTime: Long, endTime: Long): HourData {
        val statsWifi = networkStatsManager?.querySummaryForDevice(NetworkType.Wifi.ordinal, null, startTime, endTime)
        val statsMobile = networkStatsManager?.querySummaryForDevice(NetworkType.Cellular.ordinal, null, startTime, endTime)

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