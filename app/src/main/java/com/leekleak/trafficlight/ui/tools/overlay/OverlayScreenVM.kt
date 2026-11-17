package com.leekleak.trafficlight.ui.tools.overlay

import android.app.Activity
import androidx.appstate.transform.transform
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.leekleak.trafficlight.database.AppPreferenceRepo
import com.leekleak.trafficlight.model.PermissionManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

data class OverlayUiState(
    val overlay: Boolean = false,
    val overlayPermission: Boolean = false,
)

class OverlayScreenVM(
    private val appPreferenceRepo: AppPreferenceRepo,
    private val permissionManager: PermissionManager,
) : ViewModel() {
    private val expectingPermission = MutableStateFlow(false)

    val uiState: State<OverlayUiState> = transform(
        initialValue = OverlayUiState(),
        scope = viewModelScope,
        dispatcher = Dispatchers.Main,
    ) {
        val overlay by appPreferenceRepo.overlay.collectAsState(false)
        val overlayPermission by permissionManager.overlayPermission.collectAsState()

        OverlayUiState(
            overlay = overlay,
            overlayPermission = overlayPermission,
        )
    }

    fun setOverlay(enabled: Boolean, activity: Activity?) {
        if (enabled && !permissionManager.overlayPermission.value) {
            expectingPermission.value = true
            permissionManager.askOverlayPermission(activity)
        } else {
            expectingPermission.value = false
            viewModelScope.launch { appPreferenceRepo.setOverlay(enabled) }
        }
    }

    fun onOverlayPermissionChanged(granted: Boolean) {
        if (!granted) {
            expectingPermission.value = false
            return
        }
        if (expectingPermission.value) {
            expectingPermission.value = false
            viewModelScope.launch { appPreferenceRepo.setOverlay(true) }
        }
    }
}
