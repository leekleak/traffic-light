package com.leekleak.trafficlight.ui.settings

import android.Manifest
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.leekleak.core.R
import com.leekleak.core.ui.components.BackAction
import com.leekleak.core.ui.components.CategoryTitleSmallText
import com.leekleak.core.ui.components.HazeScaffold
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
    setNotification: (Boolean) -> Unit,
    setLiveNotification: (Boolean) -> Unit,
    setSeparateUpDown: (Boolean) -> Unit,
    setSpeedThreshold: (Boolean) -> Unit,
    setSpeedThresholdKb: (Long) -> Unit,
    setSpeedBits: (Boolean) -> Unit,
    setModeAOD: (Boolean) -> Unit,
    setForceFallback: (Boolean) -> Unit,
) {
    var showSettingsSheet by rememberSaveable { mutableStateOf(false) }

    if (showSettingsSheet) {
        NotificationSettingsSheet(
            onDismissRequest = { showSettingsSheet = false },
            uiState = uiState,
            setLiveNotification = setLiveNotification,
            setSeparateUpDown = setSeparateUpDown,
            setSpeedThreshold = setSpeedThreshold,
            setSpeedThresholdKb = setSpeedThresholdKb,
            setSpeedBits = setSpeedBits,
            setModeAOD = setModeAOD,
            setForceFallback = setForceFallback,
        )
    }

    HazeScaffold(
        title = stringResource(R.string.notification),
        backAction = BackAction.Normal(navigator),
        scrollState = null,
        verticalArrangement = Arrangement.Top
    ) { contentPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                NotificationSettings(uiState, setNotification)
            }
            Button(
                modifier = Modifier.align(Alignment.BottomCenter),
                onClick = { showSettingsSheet = true }
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(painterResource(R.drawable.settings), null)
                    Text(
                        modifier = Modifier.padding(start = 8.dp),
                        text = stringResource(R.string.settings)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NotificationSettingsSheet(
    onDismissRequest: () -> Unit,
    uiState: NotificationSettingsUiState,
    setLiveNotification: (Boolean) -> Unit,
    setSeparateUpDown: (Boolean) -> Unit,
    setSpeedThreshold: (Boolean) -> Unit,
    setSpeedThresholdKb: (Long) -> Unit,
    setSpeedBits: (Boolean) -> Unit,
    setModeAOD: (Boolean) -> Unit,
    setForceFallback: (Boolean) -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismissRequest
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            NotificationAppearanceSettings(
                uiState = uiState,
                setLiveNotification = setLiveNotification,
                setSeparateUpDown = setSeparateUpDown,
                setSpeedBits = setSpeedBits,
            )
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
}

@Composable
private fun NotificationSettings(
    uiState: NotificationSettingsUiState,
    setNotification: (Boolean) -> Unit,
) {
    var showWarning by remember { mutableStateOf(false) }
    if (showWarning) {
        NotificationWarningDialog(onDismiss = { showWarning = false })
    }

    val notificationPermission = uiState.notificationPermission

    var expectingNotificationPermission by remember { mutableStateOf(false) }

    fun setNotificationsEnabled(enabled: Boolean) {
        if (enabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.BAKLAVA && uiState.activePlanNotificationsCount > 0) {
            showWarning = true
        }
        setNotification(enabled)
    }

    val notificationPermissionCallback =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (!granted) expectingNotificationPermission = false
        }

    LaunchedEffect(notificationPermission) {
        if (notificationPermission && expectingNotificationPermission) {
            expectingNotificationPermission = false
            setNotificationsEnabled(true)
        }
    }

    SwitchPreference(
        title = stringResource(R.string.notification),
        summary = stringResource(R.string.notification_description),
        iconContent = { SpeedIconPreview() },
        value = uiState.notification && notificationPermission,
        onValueChanged = { enabled ->
            if (enabled && !notificationPermission) {
                expectingNotificationPermission = true
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    notificationPermissionCallback.launch(
                        Manifest.permission.POST_NOTIFICATIONS
                    )
                }
            } else {
                expectingNotificationPermission = false
                setNotificationsEnabled(enabled)
            }
        },
    )
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

    AnimatedVisibility(
        visible = uiState.speedThreshold,
        enter = fadeIn(tween()) + slideInVertically() + expandVertically(),
        exit = fadeOut(tween()) + slideOutVertically() + shrinkVertically()
    ) {
        Column {
            SliderPreference(
                modifierLabelText = Modifier.widthIn(min = 128.dp),
                title = stringResource(R.string.threshold),
                icon = painterResource(R.drawable.horizontal_align_right),
                value = uiState.speedThresholdKb,
                values = values,
                onValueChanged = setSpeedThresholdKb
            )
            Row(
                modifier = Modifier.height(IntrinsicSize.Min),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                IconPreference(
                    title = stringResource(R.string.notification_channels),
                    painter = painterResource(R.drawable.notification_settings),
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
                    onClick = {
                        openLink(
                            activity,
                            "https://github.com/leekleak/traffic-light/wiki/Troubleshooting#notification-doesnt-get-silenced"
                        )
                    },
                )
            }
        }
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
private fun NotificationAppearanceSettings(
    uiState: NotificationSettingsUiState,
    setLiveNotification: (Boolean) -> Unit,
    setSeparateUpDown: (Boolean) -> Unit,
    setSpeedBits: (Boolean) -> Unit,
) {
    CategoryTitleSmallText(stringResource(R.string.appearance))
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.BAKLAVA) {
        LiveNotificationSettings(uiState, setLiveNotification)
    }

    SwitchPreference(
        title = stringResource(R.string.separate_upload_and_download),
        summary = null,
        iconContent = { SeparateSpeedIconPreview() },
        value = uiState.separateUpDown,
        enabled = !uiState.liveNotification,
        onValueChanged = setSeparateUpDown
    )


    SwitchPreference(
        title = stringResource(R.string.speed_in_bits),
        summary = null,
        icon = R.drawable.speed,
        value = uiState.speedBits,
        onValueChanged = setSpeedBits
    )
}

@Composable
private fun LiveNotificationSettings(
    uiState: NotificationSettingsUiState,
    setLiveNotification: (Boolean) -> Unit,
) {
    val activity = LocalActivity.current

    Row(
        modifier = Modifier.height(IntrinsicSize.Min),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        SwitchPreference(
            modifier = Modifier.weight(1f),
            title = stringResource(R.string.live_notification),
            icon = R.drawable.app_badging,
            value = uiState.liveNotification,
            enabled = !uiState.separateUpDown,
            onValueChanged = setLiveNotification
        )
        IconPreference(
            title = stringResource(R.string.help),
            painter = painterResource(R.drawable.help),
            onClick = {
                openLink(
                    activity,
                    "https://github.com/leekleak/traffic-light/wiki/Troubleshooting#notifications"
                )
            },
        )
    }
}
