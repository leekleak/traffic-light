package com.leekleak.trafficlight.ui.tools.overlay

import android.app.Activity
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.res.stringResource
import com.leekleak.core.R
import com.leekleak.core.ui.components.BackAction
import com.leekleak.core.ui.components.HazeScaffold
import com.leekleak.core.ui.components.preference.SwitchPreference
import com.leekleak.core.ui.navigation.Navigator

@Composable
fun OverlayScreen(
    navigator: Navigator,
    uiState: OverlayUiState,
    setOverlay: (Boolean, Activity?) -> Unit,
    onOverlayPermissionChanged: (Boolean) -> Unit,
) {
    val activity = LocalActivity.current

    LaunchedEffect(uiState.overlayPermission) {
        onOverlayPermissionChanged(uiState.overlayPermission)
    }

    HazeScaffold(
        title = stringResource(R.string.overlay),
        backAction = BackAction.Normal(navigator),
        verticalArrangement = Arrangement.Top
    ) {
        SwitchPreference(
            title = stringResource(R.string.overlay),
            icon = R.drawable.picture_in_picture,
            value = uiState.overlay && uiState.overlayPermission,
            onValueChanged = { enabled -> setOverlay(enabled, activity) },
        )
    }
}
