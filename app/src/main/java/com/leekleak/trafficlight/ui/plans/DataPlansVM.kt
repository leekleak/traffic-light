package com.leekleak.trafficlight.ui.plans

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.leekleak.trafficlight.database.DataPlan
import com.leekleak.trafficlight.model.NetworkUsageManager
import com.leekleak.trafficlight.util.MiniCardState
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

class DataPlansVM(networkUsageManager: NetworkUsageManager): ViewModel() {
    private val dataPlansLogic = DataPlanLogic(networkUsageManager)
    private val refreshTrigger = MutableSharedFlow<Unit>(replay = 1).apply { tryEmit(Unit) }
    fun refresh() = refreshTrigger.tryEmit(Unit)

    val selectedDataPlan = MutableSharedFlow<DataPlan?>(replay = 1).apply { tryEmit(null) }
    fun selectDataPlan(dataPlan: DataPlan?) = selectedDataPlan.tryEmit(dataPlan)

    val planFlow = combine(selectedDataPlan, refreshTrigger) { plan, _ -> plan }.filterNotNull()

    val dataSafety = planFlow.map { dataPlansLogic.getDataSafety(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), MiniCardState.NEUTRAL)

    val trend = planFlow.map { dataPlansLogic.getTrend(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val todayBudget = planFlow.map { dataPlansLogic.getRemainingDailyBudgetToday(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val remainingDailyBudget = planFlow.map { dataPlansLogic.getRemainingDailyBudget(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val weekUsage = planFlow.map { dataPlansLogic.getWeekUsage(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val topApps = planFlow.map { dataPlansLogic.getTopAppUsage(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
}