package com.leekleak.trafficlight.ui.tools.notification

import android.content.Intent
import android.provider.Settings
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.rememberNavBackStack
import com.leekleak.core.R
import com.leekleak.core.ui.components.BackAction
import com.leekleak.core.ui.components.CategoryTitleSmallText
import com.leekleak.core.ui.components.HazeScaffold
import com.leekleak.core.ui.components.ScreenPreview
import com.leekleak.core.ui.components.preference.IconPreference
import com.leekleak.core.ui.components.preference.NavigatePreference
import com.leekleak.core.ui.components.preference.SliderPreference
import com.leekleak.core.ui.components.preference.SwitchPreference
import com.leekleak.core.ui.navigation.Navigator
import com.leekleak.core.ui.theme.LocalSpeedMetric
import com.leekleak.core.util.DataSize
import com.leekleak.core.util.openLink
import com.leekleak.trafficlight.services.notifications.SpeedNotification
import kotlin.math.pow

@Composable
fun NotificationSettingsScreen(
    navigator: Navigator,
    uiState: NotificationSettingsUiState,
    setSpeedThreshold: (Boolean) -> Unit,
    setSpeedThresholdKb: (Long) -> Unit,
    setModeAOD: (Boolean) -> Unit,
    setForceFallback: (Boolean) -> Unit,
) {
    HazeScaffold(
        title = stringResource(R.string.advanced_settings),
        backAction = BackAction.Normal(navigator),
        verticalArrangement = Arrangement.Top
    ) {
        BehaviorSettings(
            uiState = uiState,
            setModeAOD = setModeAOD,
            setForceFallback = setForceFallback,
        )
        NotificationChannelSettings(
            uiState = uiState,
            setSpeedThreshold = setSpeedThreshold,
            setSpeedThresholdKb = setSpeedThresholdKb,
        )
    }
}

@Composable
private fun NotificationChannelSettings(
    uiState: NotificationSettingsUiState,
    setSpeedThreshold: (Boolean) -> Unit,
    setSpeedThresholdKb: (Long) -> Unit,
) {
    val activity = LocalActivity.current

    CategoryTitleSmallText(stringResource(R.string.notification_channels))

    SwitchPreference(
        title = stringResource(R.string.speed_threshold),
        summary = stringResource(R.string.speed_threshold_description),
        icon = R.drawable.visibility_off,
        value = uiState.speedThreshold,
        onValueChanged = setSpeedThreshold
    )

    val speedBits = uiState.speedBits
    val speedMetric = LocalSpeedMetric.current
    val disconnectedString = stringResource(R.string.disconnected)
    val values = remember(speedBits, disconnectedString, speedMetric) {
        (listOf(-1L) + (0..10).map { 2.0.pow(it).toLong() }).map {
            it to if (it < 0) disconnectedString else DataSize.kb(it)
                .toString(speed = true, inBits = speedBits, metric = speedMetric)
        }
    }

    SliderPreference(
        modifierLabelText = Modifier.widthIn(min = 128.dp),
        title = stringResource(R.string.threshold),
        icon = R.drawable.horizontal_align_right,
        value = uiState.speedThresholdKb,
        values = values,
        enabled = uiState.speedThreshold,
        onValueChanged = setSpeedThresholdKb
    )
    Row(
        modifier = Modifier.height(IntrinsicSize.Min),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        IconPreference(
            title = stringResource(R.string.notification_channels),
            icon = R.drawable.notification_settings,
            enabled = uiState.speedThreshold,
            onClick = {
                val intent: Intent = Intent(Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS)
                    .putExtra(Settings.EXTRA_APP_PACKAGE, activity?.packageName)
                    .putExtra(Settings.EXTRA_CHANNEL_ID, SpeedNotification.NOTIFICATION_CHANNEL_ID_SILENT)
                activity?.startActivity(intent)
            },
        )
        NavigatePreference(
            title = stringResource(R.string.doesn_t_work),
            icon = R.drawable.help,
            enabled = uiState.speedThreshold,
            onClick = {
                openLink(
                    activity,
                    "https://github.com/leekleak/traffic-light/wiki/Troubleshooting#notification-doesnt-get-silenced"
                )
            },
        )
    }
}

@Composable
private fun BehaviorSettings(
    uiState: NotificationSettingsUiState,
    setModeAOD: (Boolean) -> Unit,
    setForceFallback: (Boolean) -> Unit,
) {
    CategoryTitleSmallText(stringResource(R.string.behavior))

    SwitchPreference(
        title = stringResource(R.string.screen_off_update),
        summary = stringResource(R.string.screen_off_update_description),
        icon = R.drawable.aod,
        value = uiState.modeAOD,
        onValueChanged = setModeAOD
    )

    SwitchPreference(
        title = stringResource(R.string.force_fallback),
        summary = if (uiState.doesFallbackWork) stringResource(R.string.force_fallback_description)
        else stringResource(R.string.fallback_unsupported),
        icon = R.drawable.fallback,
        value = uiState.forceFallback,
        enabled = uiState.doesFallbackWork,
        onValueChanged = setForceFallback
    )
}

@Composable
@ScreenPreview
private fun NotificationSettingsScreenPreview() {
    val backStack = rememberNavBackStack()
    NotificationSettingsScreen(
        navigator = Navigator(backStack),
        uiState = NotificationSettingsUiState(),
        setSpeedThreshold = {},
        setSpeedThresholdKb = {},
        setModeAOD = {},
        setForceFallback = {},
    )
}
