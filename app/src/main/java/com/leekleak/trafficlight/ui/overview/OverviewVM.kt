package com.leekleak.trafficlight.ui.overview

import androidx.appstate.transform.transform
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.leekleak.trafficlight.database.AppPreferenceRepo
import com.leekleak.trafficlight.database.AppUsage
import com.leekleak.trafficlight.database.DataPlan
import com.leekleak.trafficlight.database.DataPlanSnapshot
import com.leekleak.trafficlight.database.DataType
import com.leekleak.trafficlight.database.UsageQuery
import com.leekleak.trafficlight.model.DataPlanManager
import com.leekleak.trafficlight.model.NetworkUsageManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber

data class OverviewUiState(
    val query: UsageQuery = UsageQuery(DataType.Mobile),
    val range: OverviewRange = OverviewRange.Today,
    val usage: Long = 0L,
    val graphUsage: List<GraphPoint> = emptyList(),
    val topApps: List<AppUsage> = emptyList(),
    val plans: List<DataPlan> = emptyList(),
    val planSnapshots: Map<DataPlan, DataPlanSnapshot?> = emptyMap(),
)

class OverviewVM(
    networkUsageManager: NetworkUsageManager,
    private val appPreferenceRepo: AppPreferenceRepo,
    private val dataPlanManager: DataPlanManager,
) : ViewModel() {
    private val overviewLogic = OverviewLogic(networkUsageManager)
    private val refreshTrigger = MutableStateFlow(0)

    val uiState: State<OverviewUiState> = transform(
        initialValue = OverviewUiState(),
        scope = viewModelScope,
        dispatcher = Dispatchers.Main,
    ) {
        val refresh by refreshTrigger.collectAsState()
        val dataType by appPreferenceRepo.overviewDataType.collectAsState(DataType.Mobile)
        val range by appPreferenceRepo.overviewRange.collectAsState(OverviewRange.Today)
        val query = UsageQuery(dataType)

        val usage by produceState(0L, refresh, query, range) {
            value = overviewLogic.getUsage(query, range)
        }
        val graphUsage by produceState(emptyList(), refresh, query, range) {
            value = overviewLogic.getGraphUsage(query, range)
        }
        val topApps by produceState(emptyList(), refresh, query, range) {
            value = overviewLogic.getTopAppUsage(query, range)
        }
        val plans by dataPlanManager.activePlans.collectAsState(emptyList())
        val snapshots by dataPlanManager.snapshots.collectAsState()

        OverviewUiState(
            query = query,
            range = range,
            usage = usage,
            graphUsage = graphUsage,
            topApps = topApps,
            plans = plans,
            planSnapshots = plans.associateWith { snapshots[it.hashedSubscriberID] },
        )
    }

    fun refresh() {
        refreshTrigger.value += 1
    }

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

    init {
        viewModelScope.launch {
            refreshTrigger.collect {
                try {
                    dataPlanManager.ensureSnapshots()
                } catch (e: Exception) {
                    Timber.e(e, "Failed to load data plan snapshots")
                }
            }
        }
    }
}
