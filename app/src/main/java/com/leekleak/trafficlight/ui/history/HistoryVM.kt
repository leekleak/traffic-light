package com.leekleak.trafficlight.ui.history

import androidx.annotation.IntRange
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.leekleak.trafficlight.charts.model.ScrollableBarData
import com.leekleak.trafficlight.database.AppUsage
import com.leekleak.trafficlight.database.DataDirection
import com.leekleak.trafficlight.database.DataUID
import com.leekleak.trafficlight.database.DayUsage
import com.leekleak.trafficlight.database.Mobile
import com.leekleak.trafficlight.database.UsageQuery
import com.leekleak.trafficlight.database.Wifi
import com.leekleak.trafficlight.model.NetworkUsageManager
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate

class HistoryVM(
    private val networkUsageManager: NetworkUsageManager
): ViewModel() {
    private val dateParams = MutableStateFlow(DateParams(LocalDate.now(), false))

    private val query1 = MutableStateFlow(
        UsageQuery(
            dataType = listOf(Wifi),
            dataDirection = DataDirection.Bidirectional,
            dataUID = DataUID()
        )
    )
    private val query2 = MutableStateFlow(
        UsageQuery(
            dataType = listOf(Mobile),
            dataDirection = DataDirection.Bidirectional,
            dataUID = DataUID()
        )
    )


    val query1Flow = query1.asStateFlow()
    val query2Flow = query2.asStateFlow()
    val queryFlow = query1Flow.combine(query2Flow) {q1, q2 -> Pair(q1, q2) }
    val dateQueryFlow = dateParams.combine(queryFlow) {date, queries-> Pair(date, queries) }

    fun updateQuery(@IntRange(1, 2) n: Int, newQuery: UsageQuery) {
        when (n) {
            1 -> query1.value = newQuery
            2 -> query2.value = newQuery
            else -> throw IllegalArgumentException("Invalid query index: $n")
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    val usageFlow = combine(query1Flow, query2Flow) { q1, q2 -> q1 to q2 }.flatMapLatest { (q1, q2) ->
        networkUsageManager.daysUsage(
            startDate = datesForTimespan.first,
            endDate = datesForTimespan.second,
            usageQuery1 = q1,
            usageQuery2 = q2
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = List(MAX_DAYS) { ScrollableBarData(LocalDate.now()) }
    )

    @OptIn(ExperimentalCoroutinesApi::class)
    val appList: StateFlow<List<AppUsage>> = dateQueryFlow
        .flatMapLatest { (date, queries) ->
            networkUsageManager.getAllAppUsage(date, queries.first, queries.second)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    val totalUsage: StateFlow<DayUsage> = dateQueryFlow
        .flatMapLatest { (date, queries) ->
            flow {
                val dates = date.getStartEndDates()
                val usage1 = networkUsageManager.calculateDayUsageBasic(dates.first, dates.second, queries.first)
                val usage2 = networkUsageManager.calculateDayUsageBasic(dates.first, dates.second, queries.second)
                emit(DayUsage(dates.first, usage1, usage2))
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DayUsage())

    fun appUsageSum(usage: List<AppUsage>): Pair<Long, Long> {
        return Pair(usage.sumOf { it.usage.usage1 }, usage.sumOf { it.usage.usage2 })
    }

    fun updateDateQuery(day: LocalDate, showMonth: Boolean) {
        dateParams.value = DateParams(day, showMonth)
    }

    val datesForTimespan: Pair<LocalDate, LocalDate> by lazy {
        val now = LocalDate.now().plusDays(1)
        val base = now.minusDays(MAX_DAYS.toLong())
        Pair(base, now)
    }
}

data class DateParams(val day: LocalDate, val showMonth: Boolean) {
    fun getStartEndDates(): Pair<LocalDate, LocalDate> {
        val end = if (showMonth) {
            day.withDayOfMonth(1).plusMonths(1).minusDays(1)
        } else {
            day.plusDays(1)
        }
        return Pair(day, end)
    }
}