package com.leekleak.trafficlight.ui.history

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.annotation.IntRange
import androidx.appstate.transform.transform
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.core.net.toUri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.leekleak.core.R
import com.leekleak.core.charts.model.ScrollableBarData
import com.leekleak.core.util.toTimestamp
import com.leekleak.trafficlight.BuildConfig
import com.leekleak.trafficlight.database.AppUsage
import com.leekleak.trafficlight.database.DataType
import com.leekleak.trafficlight.database.HistoryPreferenceRepo
import com.leekleak.trafficlight.database.HourUsage
import com.leekleak.trafficlight.database.UsageQuery
import com.leekleak.trafficlight.model.AppManager
import com.leekleak.trafficlight.model.NetworkUsageManager
import com.leekleak.trafficlight.model.PermissionManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import java.time.LocalDate

const val MAX_DAYS = 90

private data class HistoryInputs(
    val query1: UsageQuery = UsageQuery(DataType.Mobile),
    val query2: UsageQuery = UsageQuery(DataType.Wifi),
    val listParam: ListParam = ListParam.AppList,
    val dateParams: DateParams = DateParams(LocalDate.now(), showMonth = false),
) {
    val forceHourList: Boolean
        get() = (query1.dataUID.uidQuery != null) || (query2.dataUID.uidQuery != null)
}

data class HistoryUiState(
    val usage: List<ScrollableBarData> = List(MAX_DAYS) { ScrollableBarData(LocalDate.MIN) },
    val query1: UsageQuery = UsageQuery(DataType.Mobile),
    val query2: UsageQuery = UsageQuery(DataType.Wifi),
    val listParam: ListParam = ListParam.AppList,
    val dateParams: DateParams = DateParams(LocalDate.now(), showMonth = false),
    val filtersChanged: Boolean = false,
    val forceHourList: Boolean = false,
    val appList: List<AppUsage> = emptyList(),
    val hourList: List<HourUsage> = emptyList(),
    val shizukuPermissionGranted: Boolean = false,
    val shizukuRunning: Boolean = false,
    val timespanStartDate: LocalDate = LocalDate.now().plusDays(1).minusDays(MAX_DAYS.toLong()),
) {
    val queries: Pair<UsageQuery, UsageQuery>
        get() = query1 to query2

    val showShizukuNotRunning: Boolean
        get() = BuildConfig.SHIZUKU && shizukuPermissionGranted && !shizukuRunning
}

class HistoryVM(
    private val networkUsageManager: NetworkUsageManager,
    private val appManager: AppManager,
    private val prefs: HistoryPreferenceRepo,
    private val permissionManager: PermissionManager,
): ViewModel() {
    private val refreshTrigger = MutableStateFlow(0)
    private val inputs = MutableStateFlow(HistoryInputs())

    val uiState: State<HistoryUiState> = transform(
        initialValue = HistoryUiState(),
        scope = viewModelScope,
        dispatcher = Dispatchers.Main.immediate,
    ) {
        val current by inputs.collectAsState(HistoryInputs())
        val query1 = current.query1
        val query2 = current.query2
        val listParam = current.listParam
        val dateParams = current.dateParams
        val refresh by refreshTrigger.collectAsState()

        val savedQuery1 by prefs.query1.collectAsState(UsageQuery(DataType.Mobile))
        val savedQuery2 by prefs.query2.collectAsState(UsageQuery(DataType.Wifi))
        val savedListParam by prefs.listParam.collectAsState(ListParam.AppList)

        val shizukuPermissionGranted by permissionManager.shizukuPermissionGranted.collectAsState()
        val shizukuRunning by permissionManager.shizukuRunning.collectAsState()

        val usage by produceState(
            initialValue = List(MAX_DAYS) { ScrollableBarData(LocalDate.MIN) },
            refresh, query1, query2,
        ) {
            val (startDate, endDate) = getDatesForTimespan()
            value = networkUsageManager.daysUsage(
                startDate = startDate,
                endDate = endDate,
                usageQuery1 = query1,
                usageQuery2 = query2,
            ).first()
        }

        val appList: List<AppUsage> = if (listParam == ListParam.AppList) {
            val list by produceState(emptyList(), refresh, dateParams, query1, query2) {
                val (startDate, endDate) = dateParams.getStartEndDates()
                value = networkUsageManager.getAllAppUsage(
                    startStamp = startDate.atStartOfDay().toTimestamp(),
                    endStamp = endDate.atStartOfDay().toTimestamp(),
                    query1 = query1,
                    query2 = query2,
                )
            }
            list
        } else {
            emptyList()
        }

        val hourList: List<HourUsage> = if (listParam == ListParam.HourList) {
            val list by produceState(emptyList(), refresh, dateParams, query1, query2) {
                value = networkUsageManager.getAllHourUsage(dateParams, query1, query2)
            }
            list
        } else {
            emptyList()
        }

        HistoryUiState(
            usage = usage,
            query1 = query1,
            query2 = query2,
            listParam = listParam,
            dateParams = dateParams,
            filtersChanged = (query1 != savedQuery1) ||
                (query2 != savedQuery2) ||
                (listParam != savedListParam),
            forceHourList = current.forceHourList,
            appList = appList,
            hourList = hourList,
            shizukuPermissionGranted = shizukuPermissionGranted,
            shizukuRunning = shizukuRunning,
            timespanStartDate = getDatesForTimespan().first,
        )
    }

    init {
        viewModelScope.launch {
            val savedQuery1 = prefs.query1.first()
            val savedQuery2 = prefs.query2.first()
            val savedListParam = prefs.listParam.first()
            inputs.update {
                it.copy(
                    query1 = savedQuery1,
                    query2 = savedQuery2,
                    listParam = savedListParam,
                )
            }
        }
        inputs
            .map { it.forceHourList }
            .distinctUntilChanged()
            .onEach { forced ->
                if (forced) {
                    inputs.update {
                        it.copy(
                            listParam = ListParam.HourList,
                            dateParams = it.dateParams.copy(showMonth = false),
                        )
                    }
                }
            }
            .launchIn(viewModelScope)
    }

    fun refresh() {
        refreshTrigger.value += 1
        inputs.update {
            it.copy(dateParams = it.dateParams.copy(day = LocalDate.now()))
        }
    }

    fun getDatesForTimespan(): Pair<LocalDate, LocalDate> {
        val now = LocalDate.now().plusDays(1)
        val base = now.minusDays(MAX_DAYS.toLong())
        return base to now
    }

    fun updateQuery(@IntRange(1, 2) n: Int, newQuery: UsageQuery) {
        inputs.update {
            when (n) {
                1 -> it.copy(query1 = newQuery)
                2 -> it.copy(query2 = newQuery)
                else -> throw IllegalArgumentException("Invalid query index: $n")
            }
        }
    }
    
    fun updateQueries(query1: UsageQuery, query2: UsageQuery) {
        inputs.update { it.copy(query1 = query1, query2 = query2) }
    }

    fun updateDateQuery(
        day: LocalDate = inputs.value.dateParams.day,
        showMonth: Boolean = inputs.value.dateParams.showMonth,
    ) {
        inputs.update { it.copy(dateParams = DateParams(day, showMonth)) }
    }

    fun updateListQuery(newList: ListParam = inputs.value.listParam) {
        inputs.update {
            if (it.forceHourList) it.copy(listParam = ListParam.HourList)
            else it.copy(listParam = newList)
        }
    }

    fun persistFilters() {
        val current = inputs.value
        viewModelScope.launch { prefs.saveQuery(1, current.query1) }
        viewModelScope.launch { prefs.saveQuery(2, current.query2) }
        viewModelScope.launch { prefs.saveListParam(current.listParam) }
    }

    fun resetFilters() = viewModelScope.launch {
        val savedQuery1 = prefs.query1.first()
        val savedQuery2 = prefs.query2.first()
        val savedListParam = prefs.listParam.first()
        inputs.update {
            it.copy(
                query1 = savedQuery1,
                query2 = savedQuery2,
                listParam = savedListParam,
            )
        }
    }

    suspend fun openPackageSettings(activity: Activity?, uid: Int) {
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
            } catch (e: android.content.ActivityNotFoundException) {
                Timber.e(e, "App not found for launch intent")
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
