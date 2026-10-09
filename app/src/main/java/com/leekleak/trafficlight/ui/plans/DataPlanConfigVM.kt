package com.leekleak.trafficlight.ui.plans

import android.os.Build
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.leekleak.trafficlight.database.AppPreferenceRepo
import com.leekleak.trafficlight.database.DataPlan
import com.leekleak.trafficlight.database.DataPlanDao
import com.leekleak.trafficlight.integrations.ShizukuServicesProvider
import com.leekleak.trafficlight.model.AppManager
import com.leekleak.trafficlight.model.DataUIDApp
import com.leekleak.trafficlight.model.NetworkUsageManager
import com.leekleak.trafficlight.model.PermissionManager
import com.leekleak.trafficlight.util.toTimestamp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.core.annotation.InjectedParam

class DataPlanConfigVM (
    val appManager: AppManager,
    val dataPlanDao: DataPlanDao,
    val appPreferenceRepo: AppPreferenceRepo,
    val networkUsageManager: NetworkUsageManager,
    val shizukuServicesProvider: ShizukuServicesProvider,
    permissionManager: PermissionManager,
    @InjectedParam initialPlan: DataPlan,
): ViewModel() {
    val newPlan: StateFlow<DataPlan>
        field = MutableStateFlow(initialPlan.copy())

    val showForegroundNotificationWarning: StateFlow<Boolean>
        field = MutableStateFlow(false)

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
            withContext(Dispatchers.Default) {
                val lastUpdateStamp = newPlan.value.updateUsage(networkUsageManager)
                val planToCalculate = newPlan.value.copy()
                val snapshot = planToCalculate.getUsageSnapshot(networkUsageManager)

                updatePlan {
                    planToCalculate.copy(
                        mainDataUsed = snapshot.mainDataUsed,
                        extras = snapshot.extras,
                        lastUpdateStamp = lastUpdateStamp
                    )
                }
            }
        }

    }

    fun onCalculateUsage() {
        viewModelScope.launch {
            val planToCalculate = newPlan.value.copy()
            withContext(Dispatchers.Default) {
                planToCalculate.mainDataUsed = 0
                planToCalculate.lastUpdateStamp = 0
                planToCalculate.extras = planToCalculate.extras.map { it.copy(dataUsed = 0) }

                val snapshot = planToCalculate.getUsageSnapshot(networkUsageManager)

                updatePlan {
                    planToCalculate.copy(
                        mainDataUsed = snapshot.mainDataUsed,
                        extras = snapshot.extras,
                        lastUpdateStamp = planToCalculate.lastUpdateStamp
                    )
                }
            }
        }
    }

    fun delete() {
        viewModelScope.launch(Dispatchers.IO) {
            dataPlanDao.delete(newPlan.value.hashedSubscriberID)
            shizukuServicesProvider.updateSimData()
        }
    }

    fun save() {
        viewModelScope.launch(Dispatchers.IO) {
            val planToSnapshot = newPlan.value.copy()
            val snapshot = planToSnapshot.getUsageSnapshot(networkUsageManager)

            val volatileMain = snapshot.mainDataUsed - planToSnapshot.mainDataUsed
            val volatileExtras = snapshot.extras.associate { it.id to (it.dataUsed - (planToSnapshot.extras.find { e -> e.id == it.id }?.dataUsed ?: 0L)) }
            val expiry = newPlan.value.getStartDate(true)

            val planToSave = newPlan.value.copy(
                mainDataUsed = newPlan.value.mainDataUsed - volatileMain,
                extras = newPlan.value.extras.map { it.copy(dataUsed = it.dataUsed - (volatileExtras[it.id] ?: 0L)) },
                lastUpdateStamp = planToSnapshot.lastUpdateStamp,
                lastSafetyState = -1,
                mainExpiryStamp = expiry.toTimestamp(),
                budgetOvershotNotified = false,
                configured = true
            )
            dataPlanDao.add(planToSave)
            shizukuServicesProvider.updateSimData()
        }
    }

    fun updatePlan(function: (DataPlan) -> DataPlan) = newPlan.update(function)

    fun onForegroundNotificationValueChanged(enabled: Boolean) {
        if (enabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.BAKLAVA) {
            viewModelScope.launch {
                val speedNotif = appPreferenceRepo.notification.first()
                val activePlanNotifs = dataPlanDao.getActivePlansWithNotificationsCountFlow().first()
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