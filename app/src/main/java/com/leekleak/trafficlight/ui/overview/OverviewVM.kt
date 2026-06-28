package com.leekleak.trafficlight.ui.overview

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.leekleak.trafficlight.database.AppPreferenceRepo
import com.leekleak.trafficlight.database.DataType
import com.leekleak.trafficlight.database.UsageQuery
import com.leekleak.trafficlight.model.NetworkUsageManager
import com.leekleak.trafficlight.util.DataSize
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

class OverviewVM(
    networkUsageManager: NetworkUsageManager,
    private val appPreferenceRepo: AppPreferenceRepo
) : ViewModel() {
    private val overviewLogic = OverviewLogic(networkUsageManager)
    private val refreshTrigger = MutableSharedFlow<Unit>(replay = 1).apply { tryEmit(Unit) }
    val query = appPreferenceRepo.overviewDataType.map { UsageQuery(it) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, UsageQuery(DataType.Mobile))

    @OptIn(FlowPreview::class)
    private val refresh = combine(query, refreshTrigger) { q, _ -> q }.debounce(100.milliseconds)
    fun refresh() = refreshTrigger.tryEmit(Unit)

    fun setDataType(dataType: DataType) {
        viewModelScope.launch {
            appPreferenceRepo.setOverviewDataType(dataType)
        }
    }

    val weekUsage = refresh.map { overviewLogic.getWeekUsage(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val todayUsage = refresh.map { overviewLogic.getTodayUsage(it) }.distinctUntilChanged()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val prediction = refresh.map { overviewLogic.getPrediction(it) }.distinctUntilChanged()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val trend = refresh.map { overviewLogic.getTrend(it) }.distinctUntilChanged()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val topApps = refresh.map { overviewLogic.getTopAppUsage(it) }
        .distinctUntilChanged { old, new ->
            if (old.size != new.size) return@distinctUntilChanged false
            old.indices.all { i ->
                val o = old[i]
                val n = new[i]
                (
                    o.app.uid == n.app.uid &&
                    DataSize(o.usage.usage1).toString() == DataSize(n.usage.usage1).toString() &&
                    DataSize(o.usage.usage2).toString() == DataSize(n.usage.usage2).toString()
                )
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
}