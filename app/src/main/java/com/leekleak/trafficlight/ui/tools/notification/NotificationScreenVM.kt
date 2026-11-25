package com.leekleak.trafficlight.ui.tools.notification

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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

data class NotificationScreenUiState(
    val notification: Boolean = false,
    val notificationPermission: Boolean = false,
    val activePlanNotificationsCount: Int = 0,
    val speedThreshold: Boolean = false,
    val speedThresholdKb: Long = -1L,
    val speedBits: Boolean = false,
    val modeAOD: Boolean = false,
    val forceFallback: Boolean = false,
    val doesFallbackWork: Boolean = false,
    val notificationType: SpeedNotificationType = SpeedNotificationType.Classic,
    val notificationAccent: NotificationAccent = NotificationAccent.Default,
)

enum class SpeedNotificationType {
    Classic,
    Split,
    Live
}

enum class NotificationAccent {
    Default,
    Primary,
    Secondary,
    Tertiary
}

class NotificationScreenVM(
    private val appPreferenceRepo: AppPreferenceRepo,
    dataPlanManager: DataPlanManager,
    permissionManager: PermissionManager,
) : ViewModel() {
    val uiState: State<NotificationScreenUiState> = transform(
        initialValue = NotificationScreenUiState(),
        scope = viewModelScope,
        dispatcher = Dispatchers.Main,
    ) {
        val notification by appPreferenceRepo.notification.collectAsState(false)
        val notificationType by appPreferenceRepo.notificationType.collectAsState(SpeedNotificationType.Classic)
        val notificationAccent by appPreferenceRepo.notificationAccent.collectAsState(NotificationAccent.Default)
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

        NotificationScreenUiState(
            notification = notification,
            notificationPermission = notificationPermission,
            activePlanNotificationsCount = activePlanNotificationsCount,
            speedThreshold = speedThreshold,
            speedThresholdKb = speedThresholdKb,
            speedBits = speedBits,
            modeAOD = modeAOD,
            forceFallback = forceFallback,
            doesFallbackWork = doesFallbackWork,
            notificationType = notificationType,
            notificationAccent = notificationAccent,
        )
    }

    fun setNotification(value: Boolean) {
        viewModelScope.launch { appPreferenceRepo.setNotification(value) }
    }

    fun setNotificationType(value: SpeedNotificationType) {
        viewModelScope.launch { appPreferenceRepo.setNotificationType(value) }
    }

    fun setNotificationAccent(value: NotificationAccent) {
        viewModelScope.launch { appPreferenceRepo.setNotificationAccent(value) }
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
