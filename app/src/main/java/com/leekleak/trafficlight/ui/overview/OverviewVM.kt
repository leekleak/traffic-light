package com.leekleak.trafficlight.ui.overview

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.leekleak.trafficlight.model.NetworkUsageManager
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

class OverviewVM(
    networkUsageManager: NetworkUsageManager
) : ViewModel() {
    private val refreshTrigger = MutableSharedFlow<Unit>(replay = 1).apply { tryEmit(Unit) }
    @OptIn(ExperimentalCoroutinesApi::class)
    val weekUsage = refreshTrigger.flatMapLatest { networkUsageManager.weekUsage() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val todayUsage = refreshTrigger.map { networkUsageManager.todayMobileUsage() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val prediction = refreshTrigger.map { networkUsageManager.predictUsage() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val trend = refreshTrigger.map { networkUsageManager.getTrend() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    fun refresh() = refreshTrigger.tryEmit(Unit)
}