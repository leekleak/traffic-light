package com.leekleak.trafficlight.ui.overview

import androidx.annotation.StringRes
import com.leekleak.core.R
import com.leekleak.core.util.toTimestamp
import com.leekleak.trafficlight.database.AppUsage
import com.leekleak.trafficlight.database.DataType
import com.leekleak.trafficlight.database.UsageQuery
import com.leekleak.trafficlight.model.AppManager.Companion.allApp
import com.leekleak.trafficlight.model.NetworkUsageManager
import com.leekleak.trafficlight.model.UsageData
import kotlinx.coroutines.flow.last
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.WeekFields
import java.util.Locale
import kotlin.math.abs

data class GraphPoint(val value: Float, val start: LocalDateTime)

fun highlightIndices(values: List<Float>, count: Int = 3, minGap: Int = 1): List<Int> {
    val gap = minGap.coerceAtLeast(1)
    val highlighted = mutableListOf<Int>()
    values.indices
        .filter { values[it] > 0f }
        .sortedByDescending { values[it] }
        .forEach { index ->
            if (highlighted.size < count && highlighted.all { abs(it - index) >= gap }) {
                highlighted += index
            }
        }
    return highlighted
}

enum class OverviewRange(@StringRes val nameRes: Int) {
    Today(R.string.today),
    Week(R.string.this_week),
    Month(R.string.this_month);

    fun startDate(): LocalDate {
        val today = LocalDate.now()
        return when (this) {
            Today -> today
            Week -> today.with(WeekFields.of(Locale.getDefault()).dayOfWeek(), 1)
            Month -> today.withDayOfMonth(1)
        }
    }
}

class OverviewLogic(val networkUsageManager: NetworkUsageManager) {
    suspend fun getUsage(query: UsageQuery, range: OverviewRange): Long {
        if (range == OverviewRange.Today) {
            return networkUsageManager.totalDayUsage(query, LocalDate.now())
        }
        return networkUsageManager.getNetworkDataForType(
            startStamp = range.startDate().toTimestamp(),
            endStamp = LocalDate.now().plusDays(1).toTimestamp(),
            subscriberId = null,
            type = query.dataType,
        ).sumForQuery(query)
    }

    suspend fun getGraphUsage(query: UsageQuery, range: OverviewRange): List<GraphPoint> {
        val start = range.startDate()
        if (range == OverviewRange.Today) {
            return networkUsageManager.getNetworkDataForTypeHourly(
                startStamp = start.toTimestamp(),
                endStamp = LocalDateTime.now().toTimestamp(),
                subscriberId = null,
                type = query.dataType,
                uid = null,
            )
                .sortedBy { it.start }
                .map { GraphPoint((it.upload + it.download).toFloat(), it.start) }
        }
        val end = if (range == OverviewRange.Week) start.plusWeeks(1) else start.plusMonths(1)
        val elapsedEnd = minOf(LocalDate.now().plusDays(1), end)
        val elapsed = networkUsageManager.daysUsage(start, elapsedEnd, query)
            .last()
            .map { GraphPoint((it.y1 + it.y2).toFloat(), it.x.atStartOfDay()) }
        return elapsed
    }

    suspend fun getTopAppUsage(query: UsageQuery, range: OverviewRange): List<AppUsage> {
        val usage = networkUsageManager.getAllAppUsage(
            startStamp = range.startDate().toTimestamp(),
            endStamp = LocalDateTime.now().toTimestamp(),
            query1 = query,
            query2 = UsageQuery(DataType.None),
        )
        return usage.filter { it.app != allApp }.take(3)
    }

    private fun List<UsageData>.sumForQuery(query: UsageQuery): Long = sumOf { usage ->
        if (query.dataUID.uidQuery == null || usage.uid == query.dataUID.uid) {
            usage.forDirection(query.dataDirection)
        } else {
            0L
        }
    }
}
