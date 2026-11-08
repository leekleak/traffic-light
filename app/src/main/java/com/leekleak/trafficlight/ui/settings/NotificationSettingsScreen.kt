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
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.leekleak.core.R
import com.leekleak.core.ui.components.BackAction
import com.leekleak.core.ui.components.HazeScaffold
import com.leekleak.core.ui.navigation.Navigator
import com.leekleak.core.ui.theme.LocalSpeedMetric
import com.leekleak.core.util.CategoryTitleSmallText
import com.leekleak.core.util.DataSize
import com.leekleak.core.util.openLink
import com.leekleak.trafficlight.database.AppPreferenceRepo
import com.leekleak.trafficlight.database.TrafficSnapshotManager
import com.leekleak.trafficlight.model.PermissionManager
import com.leekleak.trafficlight.services.notifications.SpeedNotification
import kotlinx.coroutines.launch
import kotlin.math.pow

@Composable
fun NotificationSettingsScreen(
    navigator: Navigator,
    viewModel: SettingsVM,
    appPreferenceRepo: AppPreferenceRepo,
    permissionManager: PermissionManager,
) {
    HazeScaffold(
        title = stringResource(R.string.notification),
        backAction = BackAction.Normal(navigator),
        verticalArrangement = Arrangement.Top
    ) {
        NotificationSettings(viewModel, appPreferenceRepo, permissionManager)
        NotificationAppearanceSettings(viewModel, appPreferenceRepo)
        BehaviorSettings(appPreferenceRepo)
        NotificationChannelSettings(appPreferenceRepo)
    }
}

@Composable
private fun NotificationSettings(
    viewModel: SettingsVM,
    appPreferenceRepo: AppPreferenceRepo,
    permissionManager: PermissionManager,
) {
    val scope = rememberCoroutineScope()

    CategoryTitleSmallText(stringResource(R.string.notification))

    var showWarning by remember { mutableStateOf(false) }
    if (showWarning) {
        NotificationWarningDialog(onDismiss = { showWarning = false })
    }

    val notification by viewModel.notification.collectAsStateWithLifecycle()
    val activePlanNotificationsCount by viewModel.activePlanNotificationsCount.collectAsStateWithLifecycle()
    val notificationPermission by permissionManager.notificationPermission.collectAsStateWithLifecycle()

    var expectingNotificationPermission by remember { mutableStateOf(false) }

    fun setNotificationsEnabled(enabled: Boolean) {
        if (enabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.BAKLAVA && activePlanNotificationsCount > 0) {
            showWarning = true
        }
        scope.launch {
            appPreferenceRepo.setNotification(enabled)
        }
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
        icon = R.drawable.speed_notification,
        value = notification && notificationPermission,
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
private fun NotificationChannelSettings(appPreferenceRepo: AppPreferenceRepo) {
    val activity = LocalActivity.current
    val scope = rememberCoroutineScope()

    CategoryTitleSmallText(stringResource(R.string.notification_channels))

    val speedThreshold by appPreferenceRepo.speedThreshold.collectAsState(false)
    SwitchPreference(
        title = stringResource(R.string.speed_threshold),
        summary = stringResource(R.string.speed_threshold_description),
        icon = R.drawable.visibility_off,
        value = speedThreshold,
        onValueChanged = {
            scope.launch { appPreferenceRepo.setSpeedThreshold(it) }
        }
    )

    val speedThresholdKb by appPreferenceRepo.speedThresholdKb.collectAsState(-1)
    val speedBits by appPreferenceRepo.speedBits.collectAsState(false)
    val speedMetric = LocalSpeedMetric.current
    val disconnectedString = stringResource(R.string.disconnected)
    val values = remember(speedBits, disconnectedString, speedMetric) {
        (listOf(-1L) + (0..10).map { 2.0.pow(it).toLong() }).map {
            it to if (it < 0) disconnectedString else DataSize.kb(it)
                .toString(speed = true, inBits = speedBits, metric = speedMetric)
        }
    }

    AnimatedVisibility(
        visible = speedThreshold,
        enter = fadeIn(tween()) + slideInVertically() + expandVertically(),
        exit = fadeOut(tween()) + slideOutVertically() + shrinkVertically()
    ) {
        Column {
            SliderPreference(
                modifierLabelText = Modifier.widthIn(min = 128.dp),
                title = stringResource(R.string.threshold),
                icon = painterResource(R.drawable.horizontal_align_right),
                value = speedThresholdKb,
                values = values,
                onValueChanged = {
                    scope.launch { appPreferenceRepo.setSpeedThresholdKb(it) }
                }
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
private fun BehaviorSettings(appPreferenceRepo: AppPreferenceRepo) {
    val scope = rememberCoroutineScope()

    CategoryTitleSmallText(stringResource(R.string.behavior))

    val modeAOD by appPreferenceRepo.modeAOD.collectAsState(false)
    SwitchPreference(
        title = stringResource(R.string.screen_off_update),
        summary = stringResource(R.string.screen_off_update_description),
        icon = R.drawable.aod,
        value = modeAOD,
        onValueChanged = { scope.launch { appPreferenceRepo.setModeAOD(it) } }
    )

    val forceFallback by appPreferenceRepo.forceFallback.collectAsState(false)
    val doesFallbackWork by produceState(initialValue = false) {
        value = TrafficSnapshotManager.doesFallbackWork()
    }
    SwitchPreference(
        title = stringResource(R.string.force_fallback),
        summary = if (doesFallbackWork) stringResource(R.string.force_fallback_description)
        else stringResource(R.string.fallback_unsupported),
        icon = R.drawable.fallback,
        value = forceFallback,
        enabled = doesFallbackWork,
        onValueChanged = { scope.launch { appPreferenceRepo.setForceFallback(it) } }
    )
}

@Composable
private fun NotificationAppearanceSettings(viewModel: SettingsVM, appPreferenceRepo: AppPreferenceRepo) {
    val scope = rememberCoroutineScope()

    val separateUpDown by appPreferenceRepo.separateUpDown.collectAsState(false)
    val liveNotification by viewModel.liveNotification.collectAsStateWithLifecycle()
    CategoryTitleSmallText(stringResource(R.string.appearance))
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.BAKLAVA) {
        LiveNotificationSettings(viewModel, appPreferenceRepo)
    }

    SwitchPreference(
        title = stringResource(R.string.separate_upload_and_download),
        summary = null,
        icon = R.drawable.speed_separate,
        value = separateUpDown,
        enabled = !liveNotification,
        onValueChanged = { scope.launch { appPreferenceRepo.setSeparateUpDown(it) } }
    )


    val speedBits by appPreferenceRepo.speedBits.collectAsState(false)
    SwitchPreference(
        title = stringResource(R.string.speed_in_bits),
        summary = null,
        icon = R.drawable.speed,
        value = speedBits,
        onValueChanged = { scope.launch { appPreferenceRepo.setSpeedBits(it) } }
    )
}

@Composable
private fun LiveNotificationSettings(viewModel: SettingsVM, appPreferenceRepo: AppPreferenceRepo) {
    val activity = LocalActivity.current
    val scope = rememberCoroutineScope()

    val liveNotification by viewModel.liveNotification.collectAsStateWithLifecycle()
    val separateUpDown by appPreferenceRepo.separateUpDown.collectAsState(false)
    Row(
        modifier = Modifier.height(IntrinsicSize.Min),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        SwitchPreference(
            modifier = Modifier.weight(1f),
            title = stringResource(R.string.live_notification),
            icon = R.drawable.app_badging,
            value = liveNotification,
            enabled = !separateUpDown,
            onValueChanged = { scope.launch { appPreferenceRepo.setLiveNotification(it) } }
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