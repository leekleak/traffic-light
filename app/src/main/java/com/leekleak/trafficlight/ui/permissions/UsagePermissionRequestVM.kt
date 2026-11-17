package com.leekleak.trafficlight.ui.permissions

import android.app.Activity
import androidx.appstate.transform.transform
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.leekleak.trafficlight.model.PermissionManager
import kotlinx.coroutines.Dispatchers

data class UsagePermissionRequestUiState(
    val usagePermission: Boolean = false,
)

class UsagePermissionRequestVM(
    private val permissionManager: PermissionManager,
) : ViewModel() {
    val uiState: State<UsagePermissionRequestUiState> = transform(
        initialValue = UsagePermissionRequestUiState(),
        scope = viewModelScope,
        dispatcher = Dispatchers.Main,
    ) {
        val usagePermission by permissionManager.usagePermission.collectAsState()

        UsagePermissionRequestUiState(
            usagePermission = usagePermission,
        )
    }

    fun askUsagePermission(activity: Activity?) {
        permissionManager.askUsagePermission(activity)
    }

    fun openUsagePermissionHelp(activity: Activity?) {
        permissionManager.openUsagePermissionHelp(activity)
    }
}
