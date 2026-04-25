package com.leekleak.trafficlight.ui.history

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.annotation.IntRange
import androidx.core.net.toUri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.leekleak.trafficlight.R
import com.leekleak.trafficlight.charts.model.ScrollableBarData
import com.leekleak.trafficlight.database.AppUsage
import com.leekleak.trafficlight.database.DataType
import com.leekleak.trafficlight.database.HistoryPreferenceRepo
import com.leekleak.trafficlight.database.HourUsage
import com.leekleak.trafficlight.database.UsageQuery
import com.leekleak.trafficlight.model.AppManager
import com.leekleak.trafficlight.model.NetworkUsageManager
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import timber.log.Timber
import java.time.LocalDate

class HistoryVM(
    private val networkUsageManager: NetworkUsageManager,
    private val appManager: AppManager,
    private val prefs: HistoryPreferenceRepo,
): ViewModel() {
    private val refreshTrigger = MutableSharedFlow<Unit>(replay = 1)//.apply { tryEmit(Unit) }
    private val dateParams = MutableStateFlow(DateParams(LocalDate.now(), false))
    private val listParam = MutableStateFlow(ListParam.AppList)
    private val query1 = MutableStateFlow(UsageQuery(DataType.Mobile))
    private val query2 = MutableStateFlow(UsageQuery(DataType.Wifi))
    private val savedQuery1 = prefs.query1.stateIn(viewModelScope, SharingStarted.Eagerly, UsageQuery(DataType.Mobile))
    private val savedQuery2 = prefs.query2.stateIn(viewModelScope, SharingStarted.Eagerly, UsageQuery(DataType.Wifi))
    private val savedListParam = prefs.listParam.stateIn(viewModelScope, SharingStarted.Eagerly, ListParam.AppList)
    val queryFlow = combine(query1, query2) { q1, q2 -> q1 to q2 }
        .stateIn(viewModelScope, SharingStarted.Eagerly, UsageQuery(DataType.Mobile) to UsageQuery(DataType.Wifi))
    val forceHourList = queryFlow.map { (query1, query2) ->
        query1.dataUID.uidQuery != null || query2.dataUID.uidQuery != null
    }.stateIn(viewModelScope, SharingStarted.Eagerly, false)
    val dateQueryFlow = combine(dateParams, queryFlow, refreshTrigger) {date, queries, _ -> Pair(date, queries) }
    val listParamFlow = listParam.asStateFlow()
    val dateParamsFlow = dateParams.asStateFlow()

    init {
        viewModelScope.launch {
            query1.value = prefs.query1.first()
            query2.value = prefs.query2.first()
            listParam.value = prefs.listParam.first()
            refresh()
            Timber.e("VM Launch refresh")
        }
        forceHourList
            .onEach { forced ->
                if (forced) {
                    listParam.value = ListParam.HourList
                    dateParams.value = DateParams(dateParams.value.day, false)
                }
            }
            .launchIn(viewModelScope)
    }

    fun refresh() {
        refreshTrigger.tryEmit(Unit)
        dateParams.value = DateParams(LocalDate.now(), dateParams.value.showMonth)
    }

    fun getDatesForTimespan(): Pair<LocalDate, LocalDate> {
        val now = LocalDate.now().plusDays(1)
        val base = now.minusDays(MAX_DAYS.toLong())
        return base to now
    }

    @OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
    val usageFlow = refreshTrigger.debounce(300).flatMapLatest {
        val dates = getDatesForTimespan()
        networkUsageManager.daysUsage(
            startDate = dates.first,
            endDate = dates.second,
            usageQuery1 = queryFlow.value.first,
            usageQuery2 = queryFlow.value.second
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Lazily,
        initialValue = List(MAX_DAYS) { ScrollableBarData(LocalDate.MIN) }
    )

    @OptIn(ExperimentalCoroutinesApi::class)
    val appList: StateFlow<List<AppUsage>> = dateQueryFlow.mapLatest { (date, queries) ->
            networkUsageManager.getAllAppUsage(date, queries.first, queries.second)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    val hourList: StateFlow<List<HourUsage>> = dateQueryFlow.mapLatest { (date, queries) ->
            networkUsageManager.getAllHourUsage(date, queries.first, queries.second)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val filtersChanged: StateFlow<Boolean> = combine(queryFlow, listParam, savedQuery1, savedQuery2, savedListParam) { q, lp, sq1, sq2, slp ->
            q.first != sq1 || q.second != sq2 || lp != slp
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)
    fun updateQuery(@IntRange(1, 2) n: Int, newQuery: UsageQuery) {
        when (n) {
            1 -> query1.value = newQuery
            2 -> query2.value = newQuery
            else -> throw IllegalArgumentException("Invalid query index: $n")
        }
    }

    fun updateDateQuery(day: LocalDate= dateParams.value.day, showMonth: Boolean = dateParams.value.showMonth) {
        dateParams.value = DateParams(day, showMonth)
    }

    fun updateListQuery(newList: ListParam = listParam.value) {
        if (!forceHourList.value) listParam.value = newList
        else listParam.value = ListParam.HourList
    }

    fun persistFilters() {
        viewModelScope.launch { prefs.saveQuery(1, query1.value) }
        viewModelScope.launch { prefs.saveQuery(2, query2.value) }
        viewModelScope.launch { prefs.saveListParam(listParam.value) }
    }

    fun resetFilters() = viewModelScope.launch {
        updateQuery(1, prefs.query1.first())
        updateQuery(2, prefs.query2.first())
        updateListQuery(prefs.listParam.first())
    }

    fun openPackageSettings(activity: Activity?, uid: Int) {
        val app = appManager.getAppForUID(uid)
        if (app.packageName.isNotEmpty()) {
            activity?.startActivity(
                Intent(
                    Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                    "package:${app.packageName}".toUri()
                )
            )
        }
    }

    fun openApp(activity: Activity?, launchIntent: Intent?) {
        launchIntent?.let {
            try {
                activity?.startActivity(it)
            } catch (e: Exception) {
                Timber.e(e)
            }
        }
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