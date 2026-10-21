package com.leekleak.trafficlight.ui.plans

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.leekleak.trafficlight.charts.model.BarData
import com.leekleak.trafficlight.database.AppPreferenceRepo
import com.leekleak.trafficlight.database.AppUsage
import com.leekleak.trafficlight.database.DataPlan
import com.leekleak.trafficlight.database.DataPlanDao
import com.leekleak.trafficlight.database.DataPlanSnapshot
import com.leekleak.trafficlight.model.NetworkUsageManager
import com.leekleak.trafficlight.util.MiniCardState
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.concurrent.ConcurrentHashMap

data class DataPlansUiState(
    val activePlans: List<DataPlan> = emptyList(),
    val planSnapshots: Map<DataPlan, DataPlanSnapshot?> = emptyMap(),
    val plan: DataPlan? = null,
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
    val dataPlansLogic: DataPlanLogic,
    val networkUsageManager: NetworkUsageManager,
    val appPreferenceRepo: AppPreferenceRepo,
    dataPlanDao: DataPlanDao,
): ViewModel() {
    private val selectedDataPlan = MutableStateFlow<DataPlan?>(null)
    fun selectDataPlan(dataPlan: DataPlan?) {
        selectedDataPlan.value = dataPlan
    }

    val planSnapshotFlow: StateFlow<Map<String, DataPlanSnapshot?>>
        field = MutableStateFlow<Map<String, DataPlanSnapshot?>>(emptyMap())

    private var currentActivePlans: List<DataPlan> = emptyList()
    private val calculationJobs = ConcurrentHashMap<String, Job>()

    init {
        viewModelScope.launch {
            dataPlanDao.activePlansFlow.collect { plans ->
                currentActivePlans = plans
                updateSelectedDataPlan(plans)

                val activeHashes = plans.map { it.hashedSubscriberID }.toSet()

                planSnapshotFlow.update { current ->
                    current.filterKeys { it in activeHashes }
                }

                val missingSnapshotPlans = plans.filter { plan ->
                    !planSnapshotFlow.value.containsKey(plan.hashedSubscriberID) || planSnapshotFlow.value[plan.hashedSubscriberID] == null
                }

                for (plan in missingSnapshotPlans) {
                    planSnapshotFlow.update { current ->
                        if (!current.containsKey(plan.hashedSubscriberID)) current + (plan.hashedSubscriberID to null) else current
                    }
                    loadSnapshotForPlan(plan)
                }
            }
        }
    }

    fun refresh() {
        val plans = currentActivePlans
        if (plans.isEmpty()) return

        planSnapshotFlow.update { current ->
            current + plans.associate { it.hashedSubscriberID to null }
        }

        for (plan in plans) {
            loadSnapshotForPlan(plan)
        }
    }

    private fun loadSnapshotForPlan(plan: DataPlan) {
        calculationJobs[plan.hashedSubscriberID]?.cancel()
        calculationJobs[plan.hashedSubscriberID] = viewModelScope.launch {
            val snapshot = try {
                plan.getUsageSnapshot(networkUsageManager)
            } catch (e: Exception) {
                DataPlanSnapshot()
            }
            planSnapshotFlow.update { current ->
                current + (plan.hashedSubscriberID to snapshot)
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

    private val planFlow = combine(selectedDataPlan, planSnapshotFlow) { plan, snapshots ->
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
        dataPlanDao.activePlansFlow,
        planSnapshotFlow,
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
        val snapshotsByHash = flows[1] as Map<String, DataPlanSnapshot?>
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
            plan = plan,
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
