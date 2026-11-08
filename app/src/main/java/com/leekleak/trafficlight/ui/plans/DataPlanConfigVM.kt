package com.leekleak.trafficlight.ui.plans

import android.os.Build
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.leekleak.trafficlight.database.AppPreferenceRepo
import com.leekleak.trafficlight.database.DataPlan
import com.leekleak.trafficlight.database.TimeInterval
import com.leekleak.trafficlight.integrations.ShizukuServicesProvider
import com.leekleak.trafficlight.model.AppManager
import com.leekleak.trafficlight.model.DataPlanManager
import com.leekleak.trafficlight.model.DataUIDApp
import com.leekleak.trafficlight.model.PermissionManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.koin.core.annotation.InjectedParam

class DataPlanConfigVM (
    val appManager: AppManager,
    val dataPlanManager: DataPlanManager,
    val appPreferenceRepo: AppPreferenceRepo,
    val shizukuServicesProvider: ShizukuServicesProvider,
    permissionManager: PermissionManager,
    @InjectedParam initialPlan: DataPlan,
): ViewModel() {
    val newPlan: StateFlow<DataPlan>
        field = MutableStateFlow(initialPlan.copy())

    val showForegroundNotificationWarning: StateFlow<Boolean>
        field = MutableStateFlow(false)

    private val estimationBaseline = MutableStateFlow(initialPlan.estimationParams)
    private val isEstimating = MutableStateFlow(false)

    val canEstimate: StateFlow<Boolean> = combine(
        newPlan,
        estimationBaseline,
        isEstimating
    ) { plan, baseline, estimating ->
        !estimating && plan.estimationParams != baseline
    }.stateIn(
        viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = false
    )

    val suspiciousApps: StateFlow<List<DataUIDApp>> = flow {
        emit(appManager.getAllApps())
    }.stateIn(
        viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val notificationPermission = permissionManager.notificationPermission

    init {
        viewModelScope.launch {
            val refresh = dataPlanManager.computeUsage(newPlan.value)
            updatePlan {
                refresh.plan.copy(
                    mainDataUsed = refresh.snapshot.mainDataUsed,
                    extras = refresh.snapshot.extras
                )
            }
            // The usage shown on screen open is already an estimate; require a parameter change
            // before the button becomes usable again.
            estimationBaseline.value = newPlan.value.estimationParams
        }
    }

    fun onCalculateUsage() {
        viewModelScope.launch {
            isEstimating.value = true
            try {
                val refresh = dataPlanManager.computeUsage(newPlan.value, resetUsage = true)
                updatePlan {
                    refresh.plan.copy(
                        mainDataUsed = refresh.snapshot.mainDataUsed,
                        extras = refresh.snapshot.extras
                    )
                }
                estimationBaseline.value = newPlan.value.estimationParams
            } finally {
                isEstimating.value = false
            }
        }
    }

    fun delete() {
        viewModelScope.launch(Dispatchers.IO) {
            dataPlanManager.deletePlan(newPlan.value.hashedSubscriberID)
            shizukuServicesProvider.updateSimData()
        }
    }

    fun save() {
        viewModelScope.launch(Dispatchers.IO) {
            dataPlanManager.saveConfiguredPlan(newPlan.value)
            shizukuServicesProvider.updateSimData()
        }
    }

    fun updatePlan(function: (DataPlan) -> DataPlan) = newPlan.update(function)

    fun onForegroundNotificationValueChanged(enabled: Boolean) {
        if (enabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.BAKLAVA) {
            viewModelScope.launch {
                val speedNotif = appPreferenceRepo.notification.first()
                val activePlanNotifs = dataPlanManager.activePlanNotificationsCount.first()
                val anotherPlanHasIt = if (newPlan.value.notification) activePlanNotifs > 1 else activePlanNotifs > 0
                if (speedNotif || anotherPlanHasIt) {
                    showForegroundNotificationWarning.update { true }
                }
            }
        }
        updatePlan {
            it.copy(notification = enabled)
        }
    }

    fun onLiveNotificationValueChanged(enabled: Boolean) = updatePlan { it.copy(liveNotification = enabled) }

    fun onBudgetNotificationValueChanged(enabled: Boolean) = updatePlan { it.copy(budgetWarning = enabled) }

    fun onSafetyNotificationValueChanged(enabled: Boolean) = updatePlan { it.copy(safetyWarning = enabled) }

    fun hideForegroundNotificationWarning() {
        showForegroundNotificationWarning.update { false }
    }
}

private data class UsageEstimationParams(
    val encryptedSubscriberID: String,
    val startDate: Long,
    val interval: TimeInterval,
    val intervalMultiplier: Int,
    val excludedApps: List<Int>,
    val extras: List<ExtraParams>,
) {
    data class ExtraParams(
        val dataAmount: Long,
        val startStamp: Long,
        val expiryStamp: Long,
    )
}

private val DataPlan.estimationParams: UsageEstimationParams
    get() = UsageEstimationParams(
        encryptedSubscriberID = encryptedSubscriberID,
        startDate = startDate,
        interval = interval,
        intervalMultiplier = if (interval == TimeInterval.DAY) intervalMultiplier else 1,
        excludedApps = excludedApps.sorted(),
        extras = extras
            .map {
                UsageEstimationParams.ExtraParams(
                    dataAmount = it.dataAmount.byteValue,
                    startStamp = it.startStamp,
                    expiryStamp = it.expiryStamp
                )
            }
            .sortedWith(compareBy({ it.startStamp }, { it.expiryStamp }, { it.dataAmount })),
    )
