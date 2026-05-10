package com.leekleak.trafficlight.ui.plans

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.leekleak.trafficlight.database.DataPlan
import com.leekleak.trafficlight.model.NetworkUsageManager
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

    private val selectedDataPlan = MutableSharedFlow<DataPlan?>(replay = 1).apply { tryEmit(null) }
    fun selectDataPlan(dataPlan: DataPlan?) = selectedDataPlan.tryEmit(dataPlan)

    val planFlow = combine(selectedDataPlan, refreshTrigger) { plan, _ -> plan }.filterNotNull()

    val todayBudget = planFlow.map { dataPlansLogic.getRemainingDailyBudgetToday(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val remainingDailyBudget = planFlow.map { dataPlansLogic.getRemainingDailyBudget(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)
}