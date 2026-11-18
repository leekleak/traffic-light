package com.leekleak.trafficlight.ui.tools.notification

import android.Manifest
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.snapping.SnapPosition
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PageSize
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.rememberNavBackStack
import com.leekleak.core.R
import com.leekleak.core.ui.components.BackAction
import com.leekleak.core.ui.components.CategoryTitleSmallText
import com.leekleak.core.ui.components.HazeScaffold
import com.leekleak.core.ui.components.LiveNotificationPreview
import com.leekleak.core.ui.components.NotificationWarningDialog
import com.leekleak.core.ui.components.ScreenPreview
import com.leekleak.core.ui.components.SeparateSpeedIconPreview
import com.leekleak.core.ui.components.SpeedIconPreview
import com.leekleak.core.ui.components.preference.IconPreference
import com.leekleak.core.ui.components.preference.NavigatePreference
import com.leekleak.core.ui.components.preference.SliderPreference
import com.leekleak.core.ui.components.preference.SwitchPreference
import com.leekleak.core.ui.navigation.Navigator
import com.leekleak.core.ui.theme.LocalSpeedMetric
import com.leekleak.core.ui.theme.card
import com.leekleak.core.ui.theme.googleSans
import com.leekleak.core.util.DataSize
import com.leekleak.core.util.openLink
import com.leekleak.trafficlight.services.notifications.SpeedNotification
import kotlinx.coroutines.launch
import kotlin.math.pow

@Composable
fun SpeedNotificationScreen(
    navigator: Navigator,
    uiState: NotificationSettingsUiState,
    setNotification: (Boolean) -> Unit,
    setNotificationType: (SpeedNotificationType) -> Unit,
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
        verticalArrangement = Arrangement.Top
    ) {
        NotificationSettings(uiState, setNotification)
        NotificationTypeSelector(
            selected = uiState.notificationType,
            onSelect = setNotificationType,
        )
        Button(
            modifier = Modifier,
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NotificationSettingsSheet(
    onDismissRequest: () -> Unit,
    uiState: NotificationSettingsUiState,
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
    setSpeedBits: (Boolean) -> Unit,
) {
    CategoryTitleSmallText(stringResource(R.string.appearance))

    SwitchPreference(
        title = stringResource(R.string.speed_in_bits),
        summary = null,
        icon = R.drawable.speed,
        value = uiState.speedBits,
        onValueChanged = setSpeedBits
    )
}

@Composable
private fun NotificationTypeSelector(
    selected: SpeedNotificationType,
    onSelect: (SpeedNotificationType) -> Unit,
) {
    val font = remember { googleSans(600f) }
    val types = remember {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.BAKLAVA) SpeedNotificationType.entries
        else SpeedNotificationType.entries.filter { it != SpeedNotificationType.Live }
    }
    val pagerState = rememberPagerState(
        initialPage = types.indexOf(selected).coerceAtLeast(0),
        pageCount = { types.size },
    )

    LaunchedEffect(pagerState.settledPage, selected) {
        val type = types.getOrNull(pagerState.settledPage) ?: return@LaunchedEffect
        if (type != selected) onSelect(type)
    }

    LaunchedEffect(selected) {
        val index = types.indexOf(selected)
        if (index >= 0 && index != pagerState.currentPage) {
            pagerState.animateScrollToPage(index)
        }
    }

    Column(
        modifier = Modifier.card().padding(8.dp)
    ) {
        HorizontalPager(
            modifier = Modifier.fillMaxWidth(),
            state = pagerState,
            pageSize = PageSize.Fill,
        ) { page ->
            NotificationTypeCard(type = types[page])
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(32.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                modifier = Modifier.background(MaterialTheme.colorScheme.surfaceContainerHighest, MaterialTheme.shapes.extraLargeIncreased),
                onClick = {},
                shape = MaterialTheme.shapes.extraLargeIncreased
            ) {
                Icon(painterResource(R.drawable.arrow_back), stringResource(R.string.go_back))
            }
            val type = types[pagerState.currentPage]
            Text(
                text = type.label(),
                fontFamily = font,
                style = MaterialTheme.typography.titleMedium,
            )
            IconButton(
                modifier = Modifier.background(MaterialTheme.colorScheme.surfaceContainerHighest, MaterialTheme.shapes.extraLargeIncreased),
                onClick = {},
                shape = MaterialTheme.shapes.extraLargeIncreased
            ) {
                Icon(painterResource(R.drawable.arrow_forward), stringResource(R.string.go_back))
            }
        }
    }
}

@Composable
private fun NotificationTypeCard(
    type: SpeedNotificationType,
) {
    Box(
        modifier = Modifier.fillMaxWidth().height(92.dp + 32.dp * 2),
        contentAlignment = Alignment.Center,
    ) {
        when (type) {
            SpeedNotificationType.Classic -> SpeedIconPreview(Modifier.size(92.dp))
            SpeedNotificationType.Split -> SeparateSpeedIconPreview(Modifier.size(92.dp))
            SpeedNotificationType.Live -> LiveNotificationPreview()
        }
    }
}

@Composable
private fun SpeedNotificationType.label(): String = stringResource(
    when (this) {
        SpeedNotificationType.Classic -> R.string.notification_type_classic
        SpeedNotificationType.Split -> R.string.notification_type_split
        SpeedNotificationType.Live -> R.string.notification_type_live
    }
)

@Composable
@ScreenPreview
private fun SpeedNotificationScreenPreview() {
    val backStack = rememberNavBackStack()
    SpeedNotificationScreen(
        navigator = Navigator(backStack),
        uiState = NotificationSettingsUiState(),
        setNotification = {},
        setNotificationType = {},
        setSpeedThreshold = {},
        setSpeedThresholdKb = {},
        setSpeedBits = {},
        setModeAOD = {},
        setForceFallback = {}
    )
}
