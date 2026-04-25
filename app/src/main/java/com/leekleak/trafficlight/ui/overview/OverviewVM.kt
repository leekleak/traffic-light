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
    private val refreshTrigger = MutableSharedFlow<Unit>(replay = 1).apply { tryEmit(Unit) }
    val weekUsage = refreshTrigger.map { networkUsageManager.weekUsage() }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val todayUsage = refreshTrigger.map { networkUsageManager.todayMobileUsage() }
        .stateIn(viewModelScope, SharingStarted.Eagerly, 0)

    val prediction = refreshTrigger.map { networkUsageManager.predictUsage() }
        .stateIn(viewModelScope, SharingStarted.Eagerly, 0)

    val trend = refreshTrigger.map { networkUsageManager.getTrend() }
        .stateIn(viewModelScope, SharingStarted.Eagerly, 0.0)

    fun refresh() = refreshTrigger.tryEmit(Unit)
}