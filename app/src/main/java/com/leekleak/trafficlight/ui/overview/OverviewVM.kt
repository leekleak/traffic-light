package com.leekleak.trafficlight.ui.overview

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.leekleak.core.util.DataSize
import com.leekleak.trafficlight.database.AppPreferenceRepo
import com.leekleak.trafficlight.database.DataType
import com.leekleak.trafficlight.database.UsageQuery
import com.leekleak.trafficlight.model.NetworkUsageManager
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class OverviewVM(
    networkUsageManager: NetworkUsageManager,
    private val appPreferenceRepo: AppPreferenceRepo
) : ViewModel() {
    private val overviewLogic = OverviewLogic(networkUsageManager)
    private val refreshTrigger = MutableSharedFlow<Unit>(replay = 1).apply { tryEmit(Unit) }
    val query = appPreferenceRepo.overviewDataType.map { UsageQuery(it) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, UsageQuery(DataType.Mobile))

    val range = appPreferenceRepo.overviewRange
        .stateIn(viewModelScope, SharingStarted.Eagerly, OverviewRange.Today)

    private val refresh = combine(query, range, refreshTrigger) { q, r, _ -> q to r }

    fun refresh() = refreshTrigger.tryEmit(Unit)

    fun setDataType(dataType: DataType) {
        viewModelScope.launch {
            appPreferenceRepo.setOverviewDataType(dataType)
        }
    }

    fun setRange(range: OverviewRange) {
        viewModelScope.launch {
            appPreferenceRepo.setOverviewRange(range)
        }
    }

    val usage = refresh.map { (query, range) -> overviewLogic.getUsage(query, range) }.distinctUntilChanged()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val graphUsage = refresh.map { (query, range) -> overviewLogic.getGraphUsage(query, range) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val topApps = refresh.map { (query, range) -> overviewLogic.getTopAppUsage(query, range) }
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