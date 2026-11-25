package com.leekleak.trafficlight.ui.settings

import androidx.appstate.transform.transform
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.leekleak.core.ui.theme.Theme
import com.leekleak.trafficlight.database.AppPreferenceRepo
import com.leekleak.trafficlight.model.PermissionManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

data class SettingsUiState(
    val theme: Theme = Theme.AutoMaterial,
    val blur: Boolean = true,
    val speedMetric: Boolean = true,
    val sizeMetric: Boolean = false,
    val backgroundPermission: Boolean = false,
)

class SettingsVM(
    private val appPreferenceRepo: AppPreferenceRepo,
    private val permissionManager: PermissionManager,
) : ViewModel() {
    /**
    * TODO: dispatcher is forced to work around what looks like a compose animation lib issue
    *       which has been exascerbated by the AppState lib.
    *       https://issuetracker.google.com/issues/579488073
    */
    val uiState: State<SettingsUiState> = transform(
        initialValue = SettingsUiState(),
        scope = viewModelScope,
        dispatcher = Dispatchers.Main,
    ) {
        val theme by appPreferenceRepo.theme.collectAsState()
        val blur by appPreferenceRepo.blur.collectAsState(true)
        val speedMetric by appPreferenceRepo.speedMetric.collectAsState(true)
        val sizeMetric by appPreferenceRepo.sizeMetric.collectAsState(false)
        val backgroundPermission by permissionManager.backgroundPermission.collectAsState()

        SettingsUiState(
            theme = theme,
            blur = blur,
            speedMetric = speedMetric,
            sizeMetric = sizeMetric,
            backgroundPermission = backgroundPermission,
        )
    }

    fun setTheme(value: Theme) {
        viewModelScope.launch { appPreferenceRepo.setTheme(value) }
    }

    fun setBlur(value: Boolean) {
        viewModelScope.launch { appPreferenceRepo.setBlur(value) }
    }

    fun setSpeedMetric(value: Boolean) {
        viewModelScope.launch { appPreferenceRepo.setSpeedMetric(value) }
    }

    fun setSizeMetric(value: Boolean) {
        viewModelScope.launch { appPreferenceRepo.setSizeMetric(value) }
    }
}
