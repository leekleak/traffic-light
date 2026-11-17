package com.leekleak.trafficlight.ui.settings

import androidx.appstate.transform.transform
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.leekleak.trafficlight.database.AppPreferenceRepo
import com.leekleak.trafficlight.database.TrafficSnapshotManager
import com.leekleak.trafficlight.model.DataPlanManager
import com.leekleak.trafficlight.model.PermissionManager
import kotlinx.coroutines.launch

data class NotificationSettingsUiState(
    val notification: Boolean = false,
    val notificationPermission: Boolean = false,
    val activePlanNotificationsCount: Int = 0,
    val liveNotification: Boolean = false,
    val separateUpDown: Boolean = false,
    val speedThreshold: Boolean = false,
    val speedThresholdKb: Long = -1L,
    val speedBits: Boolean = false,
    val modeAOD: Boolean = false,
    val forceFallback: Boolean = false,
    val doesFallbackWork: Boolean = false,
)

class NotificationSettingsVM(
    private val appPreferenceRepo: AppPreferenceRepo,
    dataPlanManager: DataPlanManager,
    permissionManager: PermissionManager,
) : ViewModel() {
    val uiState: State<NotificationSettingsUiState> = transform(
        initialValue = NotificationSettingsUiState(),
        scope = viewModelScope,
    ) {
        val notification by appPreferenceRepo.notification.collectAsState(false)
        val liveNotification by appPreferenceRepo.liveNotification.collectAsState(false)
        val separateUpDown by appPreferenceRepo.separateUpDown.collectAsState(false)
        val speedThreshold by appPreferenceRepo.speedThreshold.collectAsState(false)
        val speedThresholdKb by appPreferenceRepo.speedThresholdKb.collectAsState(-1L)
        val speedBits by appPreferenceRepo.speedBits.collectAsState(false)
        val modeAOD by appPreferenceRepo.modeAOD.collectAsState(false)
        val forceFallback by appPreferenceRepo.forceFallback.collectAsState(false)
        val notificationPermission by permissionManager.notificationPermission.collectAsState()
        val activePlanNotificationsCount by dataPlanManager.activePlanNotificationsCount.collectAsState(0)
        val doesFallbackWork by produceState(initialValue = false) {
            value = TrafficSnapshotManager.doesFallbackWork()
        }

        NotificationSettingsUiState(
            notification = notification,
            notificationPermission = notificationPermission,
            activePlanNotificationsCount = activePlanNotificationsCount,
            liveNotification = liveNotification,
            separateUpDown = separateUpDown,
            speedThreshold = speedThreshold,
            speedThresholdKb = speedThresholdKb,
            speedBits = speedBits,
            modeAOD = modeAOD,
            forceFallback = forceFallback,
            doesFallbackWork = doesFallbackWork,
        )
    }

    fun setNotification(value: Boolean) {
        viewModelScope.launch { appPreferenceRepo.setNotification(value) }
    }

    fun setLiveNotification(value: Boolean) {
        viewModelScope.launch { appPreferenceRepo.setLiveNotification(value) }
    }

    fun setSeparateUpDown(value: Boolean) {
        viewModelScope.launch { appPreferenceRepo.setSeparateUpDown(value) }
    }

    fun setSpeedThreshold(value: Boolean) {
        viewModelScope.launch { appPreferenceRepo.setSpeedThreshold(value) }
    }

    fun setSpeedThresholdKb(value: Long) {
        viewModelScope.launch { appPreferenceRepo.setSpeedThresholdKb(value) }
    }

    fun setSpeedBits(value: Boolean) {
        viewModelScope.launch { appPreferenceRepo.setSpeedBits(value) }
    }

    fun setModeAOD(value: Boolean) {
        viewModelScope.launch { appPreferenceRepo.setModeAOD(value) }
    }

    fun setForceFallback(value: Boolean) {
        viewModelScope.launch { appPreferenceRepo.setForceFallback(value) }
    }
}
