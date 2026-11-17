package com.leekleak.trafficlight.ui.tools.overlay

import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.leekleak.core.R
import com.leekleak.core.ui.components.BackAction
import com.leekleak.core.ui.components.HazeScaffold
import com.leekleak.core.ui.components.preference.SwitchPreference
import com.leekleak.core.ui.navigation.Navigator
import com.leekleak.trafficlight.database.AppPreferenceRepo
import com.leekleak.trafficlight.model.PermissionManager
import kotlinx.coroutines.launch

@Composable
fun OverlayScreen(
    navigator: Navigator,
    appPreferenceRepo: AppPreferenceRepo,
    permissionManager: PermissionManager,
) {
    val activity = LocalActivity.current
    val scope = rememberCoroutineScope()

    val overlay by appPreferenceRepo.overlay.collectAsState(false)
    val overlayPermission by permissionManager.overlayPermission.collectAsStateWithLifecycle()

    var expectingOverlayPermission by remember { mutableStateOf(false) }

    LaunchedEffect(overlayPermission) {
        if (overlayPermission && expectingOverlayPermission) {
            expectingOverlayPermission = false
            scope.launch { appPreferenceRepo.setOverlay(true) }
        }
    }

    HazeScaffold(
        title = stringResource(R.string.overlay),
        backAction = BackAction.Normal(navigator),
        verticalArrangement = Arrangement.Top
    ) {
        SwitchPreference(
            title = stringResource(R.string.overlay),
            icon = R.drawable.picture_in_picture,
            value = overlay && overlayPermission,
            onValueChanged = { enabled ->
                if (enabled && !overlayPermission) {
                    expectingOverlayPermission = true
                    permissionManager.askOverlayPermission(activity)
                } else {
                    expectingOverlayPermission = false
                    scope.launch { appPreferenceRepo.setOverlay(enabled) }
                }
            },
        )
    }
}
