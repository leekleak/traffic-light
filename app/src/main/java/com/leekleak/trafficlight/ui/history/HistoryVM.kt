package com.leekleak.trafficlight.ui.history

import androidx.annotation.IntRange
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.leekleak.trafficlight.charts.model.ScrollableBarData
import com.leekleak.trafficlight.database.AppUsage
import com.leekleak.trafficlight.database.DataDirection
import com.leekleak.trafficlight.database.DataType
import com.leekleak.trafficlight.database.DayUsage
import com.leekleak.trafficlight.database.Mobile
import com.leekleak.trafficlight.database.UsageQuery
import com.leekleak.trafficlight.database.Wifi
import com.leekleak.trafficlight.model.NetworkUsageManager
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
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
            dataDirection = DataDirection.Both,
            dataUID = null
        )
    )
    private val query2 = MutableStateFlow(
        UsageQuery(
            dataType = listOf(Mobile),
            dataDirection = DataDirection.Both,
            dataUID = null
        )
    )

    val query1Flow = query1.asStateFlow()
    val query2Flow = query2.asStateFlow()

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
    val appList: StateFlow<List<AppUsage>> = dateParams
        .flatMapLatest { (day, isMonth) ->
            if (!isMonth) {
                networkUsageManager.getAllAppUsage(day, day)
            } else {
                val start = day.withDayOfMonth(1)
                val end = start.plusMonths(1).minusDays(1)
                networkUsageManager.getAllAppUsage(start, end)
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    val totalUsage: Flow<DayUsage> = appList
        .flatMapLatest {
            flow {
                val (day, isMonth) = dateParams.value
                if (!isMonth) {
                    val data = networkUsageManager.calculateDayUsageBasic(day, day, listOf(Mobile, Wifi))
                    emit(data)
                } else {
                    val start = day.withDayOfMonth(1)
                    val end = start.plusMonths(1).minusDays(1)
                    emit(networkUsageManager.calculateDayUsageBasic(start, end, listOf(Mobile, Wifi)))
                }
            }
        }

    fun appUsageSum(usage: List<AppUsage>): DayUsage {
        val map = mutableMapOf<DataType, Long>(Wifi to 0, Mobile to 0)
        for (i in usage) {
            map.merge(Wifi, i.usage.usages[Wifi] ?: 0L, Long::plus)
            map.merge(Mobile, i.usage.usages[Mobile] ?: 0L, Long::plus)
        }
        return DayUsage(usages = map)
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

data class DateParams(val day: LocalDate, val showMonth: Boolean)