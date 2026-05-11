package com.leekleak.trafficlight.ui.overview

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.leekleak.trafficlight.model.NetworkUsageManager
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

class OverviewVM(
    networkUsageManager: NetworkUsageManager
) : ViewModel() {
    private val overviewLogic = OverviewLogic(networkUsageManager)
    private val refreshTrigger = MutableSharedFlow<Unit>(replay = 1).apply { tryEmit(Unit) }
    fun refresh() = refreshTrigger.tryEmit(Unit)

    val weekUsage = refreshTrigger.map { overviewLogic.getWeekUsage() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val todayUsage = refreshTrigger.map { overviewLogic.getTodayUsage() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val prediction = refreshTrigger.map { overviewLogic.getPrediction() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val trend = refreshTrigger.map { overviewLogic.getTrend() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val topApps = refreshTrigger.map { overviewLogic.getTopAppUsage() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
}