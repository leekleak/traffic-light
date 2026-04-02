package com.leekleak.trafficlight.ui.history

import android.content.Context
import androidx.annotation.IntRange
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.leekleak.trafficlight.R
import com.leekleak.trafficlight.charts.model.ScrollableBarData
import com.leekleak.trafficlight.database.AppUsage
import com.leekleak.trafficlight.database.HistoryPreferenceRepo
import com.leekleak.trafficlight.database.HourUsage
import com.leekleak.trafficlight.database.UsageQuery
import com.leekleak.trafficlight.model.NetworkUsageManager
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

class HistoryVM(
    private val networkUsageManager: NetworkUsageManager,
    private val prefs: HistoryPreferenceRepo,
    initialListParam: ListParam,
    initialQuery1: UsageQuery,
    initialQuery2: UsageQuery,
): ViewModel() {
    private val dateParams = MutableStateFlow(DateParams(LocalDate.now(), false))
    private val listParam = MutableStateFlow(initialListParam)
    private val query1 = MutableStateFlow(initialQuery1)
    private val query2 = MutableStateFlow(initialQuery2)

    val query1Flow = query1.asStateFlow()
    val query2Flow = query2.asStateFlow()
    val queryFlow = query1Flow.combine(query2Flow) {q1, q2 -> Pair(q1, q2) }
    val dateQueryFlow = dateParams.combine(queryFlow) {date, queries-> Pair(date, queries) }
    val listParamFlow = listParam.asStateFlow()
    val dateParamsFlow = dateParams.asStateFlow()

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
    val hourList: StateFlow<List<HourUsage>> = dateQueryFlow
        .flatMapLatest { (date, queries) ->
            networkUsageManager.getAllHourUsage(date, queries.first, queries.second)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun updateQuery(@IntRange(1, 2) n: Int, newQuery: UsageQuery) {
        when (n) {
            1 -> query1.value = newQuery
            2 -> query2.value = newQuery
            else -> throw IllegalArgumentException("Invalid query index: $n")
        }

        updateListQuery(listParam.value) // Force update of list
    }

    fun updateDateQuery(day: LocalDate= dateParams.value.day, showMonth: Boolean = dateParams.value.showMonth) {
        if ((forceHourList.value && showMonth) || !showMonth) {
            dateParams.value = DateParams(day, showMonth)
        }
    }

    fun updateListQuery(newList: ListParam) {
        if (forceHourList.value) listParam.value = newList
        else listParam.value = ListParam.HourList
    }

    val forceHourList: StateFlow<Boolean> = queryFlow.map { (query1, query2) ->
        query1.dataUID.uidQuery == null && query2.dataUID.uidQuery == null
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = false
    )

    val datesForTimespan: Pair<LocalDate, LocalDate> by lazy {
        val now = LocalDate.now().plusDays(1)
        val base = now.minusDays(MAX_DAYS.toLong())
        Pair(base, now)
    }

    fun persistFilters() {
        viewModelScope.launch { prefs.saveQuery(1, query1.value) }
        viewModelScope.launch { prefs.saveQuery(2, query2.value) }
        viewModelScope.launch { prefs.saveListParam(listParam.value) }
    }
}

data class DateParams(val day: LocalDate, val showMonth: Boolean) {
    fun getStartEndDates(): Pair<LocalDate, LocalDate> {
        val start = if (showMonth) { day.withDayOfMonth(1) } else { day }
        val end = if (showMonth) {
            day.withDayOfMonth(1).plusMonths(1).minusDays(1)
        } else {
            day.plusDays(1)
        }
        return Pair(start, end)
    }
}

enum class ListParam {
    AppList,
    HourList
}

fun ListParam.getString(context: Context): String {
    return context.getString(
        when (this) {
            ListParam.AppList -> R.string.app_list
            ListParam.HourList -> R.string.hour_list
        }
    )
}