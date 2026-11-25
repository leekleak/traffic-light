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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

data class NotificationSettingsUiState(
    val speedThreshold: Boolean = false,
    val speedThresholdKb: Long = -1L,
    val speedBits: Boolean = false,
    val modeAOD: Boolean = false,
    val forceFallback: Boolean = false,
    val doesFallbackWork: Boolean = false,
)

class NotificationSettingsScreenVM(
    private val appPreferenceRepo: AppPreferenceRepo,
) : ViewModel() {
    val uiState: State<NotificationSettingsUiState> = transform(
        initialValue = NotificationSettingsUiState(),
        scope = viewModelScope,
        dispatcher = Dispatchers.Main,
    ) {
        val speedThreshold by appPreferenceRepo.speedThreshold.collectAsState(false)
        val speedThresholdKb by appPreferenceRepo.speedThresholdKb.collectAsState(-1L)
        val speedBits by appPreferenceRepo.speedBits.collectAsState(false)
        val modeAOD by appPreferenceRepo.modeAOD.collectAsState(false)
        val forceFallback by appPreferenceRepo.forceFallback.collectAsState(false)
        val doesFallbackWork by produceState(initialValue = false) {
            value = TrafficSnapshotManager.doesFallbackWork()
        }

        NotificationSettingsUiState(
            speedThreshold = speedThreshold,
            speedThresholdKb = speedThresholdKb,
            speedBits = speedBits,
            modeAOD = modeAOD,
            forceFallback = forceFallback,
            doesFallbackWork = doesFallbackWork,
        )
    }

    fun setSpeedThreshold(value: Boolean) {
        viewModelScope.launch { appPreferenceRepo.setSpeedThreshold(value) }
    }

    fun setSpeedThresholdKb(value: Long) {
        viewModelScope.launch { appPreferenceRepo.setSpeedThresholdKb(value) }
    }

    fun setModeAOD(value: Boolean) {
        viewModelScope.launch { appPreferenceRepo.setModeAOD(value) }
    }

    fun setForceFallback(value: Boolean) {
        viewModelScope.launch { appPreferenceRepo.setForceFallback(value) }
    }
}
