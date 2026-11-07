package com.leekleak.trafficlight.ui.plans

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.leekleak.core.charts.model.BarData
import com.leekleak.trafficlight.database.AppPreferenceRepo
import com.leekleak.trafficlight.database.AppUsage
import com.leekleak.trafficlight.database.DataPlan
import com.leekleak.trafficlight.database.DataPlanSnapshot
import com.leekleak.trafficlight.model.DataPlanManager
import com.leekleak.core.util.MiniCardState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import timber.log.Timber

data class DataPlansUiState(
    val activePlans: List<DataPlan> = emptyList(),
    val planSnapshots: Map<DataPlan, DataPlanSnapshot?> = emptyMap(),
    val selectedPlan: DataPlan? = null,
    val dataSafety: MiniCardState = MiniCardState.NEUTRAL,
    val trend: Int = 0,
    val todayBudget: Long = 0,
    val remainingDailyBudget: Long = 0,
    val weekUsage: List<BarData>? = null,
    val topApps: List<AppUsage>? = null,
    val adsEnabled: Boolean = false,
    val shizukuHint: Boolean = false,
    val shizukuTracking: Boolean = false,
)

class DataPlansVM(
    private val dataPlansLogic: DataPlanLogic,
    private val appPreferenceRepo: AppPreferenceRepo,
    private val dataPlanManager: DataPlanManager,
): ViewModel() {
    private val selectedDataPlan = MutableStateFlow<DataPlan?>(null)
    fun selectDataPlan(dataPlan: DataPlan?) {
        selectedDataPlan.value = dataPlan
    }

    init {
        viewModelScope.launch {
            dataPlanManager.activePlans.collect { plans ->
                updateSelectedDataPlan(plans)

                val missingSnapshotPlans = plans.filter { plan ->
                    dataPlanManager.snapshots.value[plan.hashedSubscriberID] == null
                }
                if (missingSnapshotPlans.isNotEmpty()) {
                    try {
                        dataPlanManager.getSnapshots(missingSnapshotPlans)
                    } catch (e: Exception) {
                        Timber.e(e, "Failed to load data plan snapshots")
                    }
                }
            }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            try {
                dataPlanManager.refresh()
            } catch (e: Exception) {
                Timber.e(e, "Failed to refresh data plans")
            }
        }
    }

    private fun updateSelectedDataPlan(plans: List<DataPlan>) {
        if (plans.isEmpty()) {
            selectedDataPlan.value = null
        } else if (selectedDataPlan.value == null) {
            selectedDataPlan.value = plans.first()
        } else if (!plans.contains(selectedDataPlan.value)) {
            selectedDataPlan.value =
                plans.find { it.hashedSubscriberID == selectedDataPlan.value?.hashedSubscriberID }
                    ?: plans.first()
        }
    }

    private val planFlow = combine(selectedDataPlan, dataPlanManager.snapshots) { plan, snapshots ->
        val snapshot = plan?.hashedSubscriberID?.let { snapshots[it] }
        if (plan != null && snapshot != null) {
            plan to snapshot
        }
        else null
    }.distinctUntilChanged()

    private val dataSafetyFlow = planFlow.map { pair ->
        pair?.let { (plan, snapshot) -> dataPlansLogic.getDataSafety(plan, snapshot) }
    }.distinctUntilChanged()

    private val todayBudgetFlow = planFlow.map { pair ->
        pair?.let { (plan, snapshot) -> dataPlansLogic.getRemainingDailyBudgetToday(plan, snapshot) }
    }.distinctUntilChanged()

    private val remainingDailyBudgetFlow = planFlow.map { pair ->
        pair?.let { (plan, snapshot) -> dataPlansLogic.getRemainingDailyBudget(plan, snapshot) }
    }.distinctUntilChanged()

    private val trendFlow = planFlow.map { pair ->
        pair?.let { (plan, _) -> dataPlansLogic.getTrend(plan) }
    }.distinctUntilChanged()

    private val weekUsageFlow = planFlow.map { pair ->
        pair?.let { (plan, _) -> dataPlansLogic.getWeekUsage(plan) }
    }.distinctUntilChanged()

    private val topAppsFlow = planFlow.map { pair ->
        pair?.let { (plan, _) -> dataPlansLogic.getTopAppUsage(plan) }
    }.distinctUntilChanged()

    @Suppress("UNCHECKED_CAST")
    val uiState = combine(
        dataPlanManager.activePlans,
        dataPlanManager.snapshots,
        selectedDataPlan,
        dataSafetyFlow,
        trendFlow,
        todayBudgetFlow,
        remainingDailyBudgetFlow,
        weekUsageFlow,
        topAppsFlow,
        appPreferenceRepo.ads,
        appPreferenceRepo.shizukuHint,
        appPreferenceRepo.shizukuTracking
    ) { flows ->
        val activePlans = flows[0] as List<DataPlan>
        val snapshotsByHash = flows[1] as Map<String, DataPlanSnapshot>
        val planSnapshots = activePlans.associateWith { plan -> snapshotsByHash[plan.hashedSubscriberID] }
        val plan = flows[2] as DataPlan?
        val safety = flows[3] as MiniCardState? ?: MiniCardState.NEUTRAL
        val trend = flows[4] as Int? ?: 0
        val today = flows[5] as Long? ?: 0L
        val remaining = flows[6] as Long? ?: 0L
        val week = flows[7] as List<BarData>?
        val apps = flows[8] as List<AppUsage>?
        val adsEnabled = flows[9] as Boolean
        val shizukuHint = flows[10] as Boolean
        val shizukuTracking = flows[11] as Boolean

        DataPlansUiState(
            activePlans = activePlans,
            selectedPlan = plan,
            planSnapshots = planSnapshots,
            dataSafety = safety,
            trend = trend,
            todayBudget = today,
            remainingDailyBudget = remaining,
            weekUsage = week,
            topApps = apps,
            adsEnabled = adsEnabled,
            shizukuHint = shizukuHint,
            shizukuTracking = shizukuTracking
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DataPlansUiState())

    fun disableShizukuHint() = viewModelScope.launch { appPreferenceRepo.setShizukuHint(false) }
}
