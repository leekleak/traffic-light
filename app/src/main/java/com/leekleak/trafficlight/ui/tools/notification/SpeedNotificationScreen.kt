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
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PageSize
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonColors
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.rememberNavBackStack
import com.leekleak.core.R
import com.leekleak.core.ui.components.BackAction
import com.leekleak.core.ui.components.CategoryTitleSmallText
import com.leekleak.core.ui.components.HazeScaffold
import com.leekleak.core.ui.components.InfoCard
import com.leekleak.core.ui.components.LiveNotificationPreview
import com.leekleak.core.ui.components.NotificationWarningDialog
import com.leekleak.core.ui.components.ScreenPreview
import com.leekleak.core.ui.components.SeparateSpeedIconPreview
import com.leekleak.core.ui.components.SlideAnimatedVisibility
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
    setNotificationAccent: (NotificationAccent) -> Unit,
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
            setModeAOD = setModeAOD,
            setForceFallback = setForceFallback,
        )
    }

    HazeScaffold(
        title = stringResource(R.string.notification),
        backAction = BackAction.Normal(navigator),
        verticalArrangement = Arrangement.Top
    ) {
        OnOffSetting(uiState, setNotification)
        NotificationTypeSelector(
            selected = uiState.notificationType,
            onSelect = setNotificationType,
        )
        NotificationAppearanceSettings(
            uiState = uiState,
            setSpeedBits = setSpeedBits,
            setNotificationAccent = setNotificationAccent,
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
private fun OnOffSetting(
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
                icon = R.drawable.horizontal_align_right,
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
                    icon = R.drawable.notification_settings,
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
    setNotificationAccent: (NotificationAccent) -> Unit,
) {
    CategoryTitleSmallText(stringResource(R.string.appearance))

    SwitchPreference(
        title = stringResource(R.string.speed_in_bits),
        summary = null,
        icon = R.drawable.speed,
        value = uiState.speedBits,
        onValueChanged = setSpeedBits
    )

    NotificationAccentPreference(
        value = uiState.notificationAccent,
        onValueChanged = setNotificationAccent,
    )
}

@Composable
private fun NotificationAccentPreference(
    value: NotificationAccent,
    onValueChanged: (NotificationAccent) -> Unit,
) {
    Column(
        modifier = Modifier.padding(vertical = 2.dp).card().padding(8.dp).selectableGroup(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(modifier = Modifier.width(56.dp), painter = painterResource(R.drawable.palette), contentDescription = null)
            Text(
                modifier = Modifier.padding(),
                text = stringResource(R.string.background_color),
                style = MaterialTheme.typography.titleMedium,
            )
        }
        Row {
            NotificationAccentButton(NotificationAccent.Default, value, onValueChanged)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            NotificationAccentButton(NotificationAccent.Primary, value, onValueChanged)
            NotificationAccentButton(NotificationAccent.Secondary, value, onValueChanged)
            NotificationAccentButton(NotificationAccent.Tertiary, value, onValueChanged)
        }
    }
}


@Composable
private fun RowScope.NotificationAccentButton(
    color: NotificationAccent,
    value: NotificationAccent,
    onValueChanged: (NotificationAccent) -> Unit
){
    val haptic = LocalHapticFeedback.current
    val selected = value == color
    val label = color.label()
    val borderColor by animateColorAsState(if (selected) color.onColor() else color.onColor().copy(alpha = 0.2f))
    Box(
        modifier = Modifier.weight(1f)
            .height(52.dp)
            .border(
                width = 2.dp,
                color = borderColor,
                shape = MaterialTheme.shapes.large
            )
            .clip(MaterialTheme.shapes.large)
            .background(color.color(), shape = MaterialTheme.shapes.large)
            .semantics {
                if (color != NotificationAccent.Default) contentDescription = label
            }
            .selectable(
                selected = selected,
                role = Role.RadioButton,
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                    onValueChanged(color)
                },
            ),
    ) {
        Row(
            modifier = Modifier.align(Alignment.Center),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AnimatedVisibility (
                visible = selected,
                enter = fadeIn(tween()) + scaleIn() + expandHorizontally(),
                exit = fadeOut(tween()) + scaleOut() + shrinkHorizontally()
            ) {
                Icon(
                    modifier = Modifier.padding(8.dp),
                    painter = painterResource(R.drawable.check_circle),
                    contentDescription = null,
                    tint = color.onColor()
                )
            }
            if (color == NotificationAccent.Default) {
                val font = remember { googleSans(600f) }
                Text(text = label, fontFamily = font)
            }
        }
    }
}

@Composable
private fun NotificationAccent.label(): String = stringResource(
    when (this) {
        NotificationAccent.Primary -> R.string.primary
        NotificationAccent.Secondary -> R.string.secondary
        NotificationAccent.Tertiary -> R.string.tertiary
        NotificationAccent.Default -> R.string.default_accent
    }
)

@Composable
private fun NotificationAccent.color(): Color = when (this) {
    NotificationAccent.Primary -> MaterialTheme.colorScheme.primary
    NotificationAccent.Secondary -> MaterialTheme.colorScheme.secondary
    NotificationAccent.Tertiary -> MaterialTheme.colorScheme.tertiary
    NotificationAccent.Default -> MaterialTheme.colorScheme.surface
}

@Composable
private fun NotificationAccent.onColor(): Color = when (this) {
    NotificationAccent.Primary -> MaterialTheme.colorScheme.onPrimary
    NotificationAccent.Secondary -> MaterialTheme.colorScheme.onSecondary
    NotificationAccent.Tertiary -> MaterialTheme.colorScheme.onTertiary
    NotificationAccent.Default -> MaterialTheme.colorScheme.onSurface
}

@Composable
fun highIconButtonColors(): IconButtonColors {
    return IconButtonDefaults.iconButtonColors(
        containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
        contentColor = MaterialTheme.colorScheme.onSurface,
        disabledContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun NotificationTypeSelector(
    selected: SpeedNotificationType,
    onSelect: (SpeedNotificationType) -> Unit,
) {
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current

    val font = remember { googleSans(600f) }
    val types = remember {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.BAKLAVA) SpeedNotificationType.entries
        else SpeedNotificationType.entries.filter { it != SpeedNotificationType.Live }
    }
    val pagerState = rememberPagerState(
        initialPage = types.indexOf(selected).coerceAtLeast(0),
        pageCount = { types.size },
    )
    var ignoreSelection by remember { mutableStateOf(false) }

    LaunchedEffect(selected) {
        val index = types.indexOf(selected)
        if (index >= 0 && index != pagerState.currentPage && !ignoreSelection) {
            pagerState.scrollToPage(index)
        }
    }

    LaunchedEffect(pagerState.settledPage) {
        val type = types.getOrNull(pagerState.settledPage) ?: return@LaunchedEffect
        if (type != selected) {
            onSelect(type)
            ignoreSelection = true
        }
    }

    Column(
        modifier = Modifier.padding(vertical = 2.dp).card()
    ) {
        HorizontalPager(
            modifier = Modifier.fillMaxWidth(),
            state = pagerState,
            pageSize = PageSize.Fill,
        ) { page ->
            NotificationTypeCard(type = types[page])
        }
        val type = types[pagerState.currentPage]
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(32.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                    scope.launch { pagerState.animateScrollToPage(pagerState.currentPage - 1) }
                },
                enabled = pagerState.currentPage != 0,
                colors = highIconButtonColors(),
            ) {
                Icon(
                    modifier = Modifier.padding(8.dp),
                    painter = painterResource(R.drawable.arrow_back),
                    contentDescription = stringResource(R.string.go_back)
                )
            }
            Text(
                modifier = Modifier.width(92.dp),
                text = type.label(),
                fontFamily = font,
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.titleMedium,
            )
            IconButton(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                    scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
                },
                enabled = pagerState.currentPage != pagerState.pageCount - 1,
                colors = highIconButtonColors(),
            ) {
                Icon(painterResource(R.drawable.arrow_forward), stringResource(R.string.go_back))
            }
        }
    }

    SlideAnimatedVisibility(types[pagerState.currentPage] == SpeedNotificationType.Live) {
        InfoCard(
            title = stringResource(R.string.info),
            description = stringResource(R.string.live_speed_notification_warning),
            icon = R.drawable.info,
        )
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
            SpeedNotificationType.Live -> {
                LiveNotificationHelpButton(Modifier.align(Alignment.TopEnd))
                LiveNotificationPreview()
            }
        }
    }
}

@Composable
private fun LiveNotificationHelpButton(
    modifier: Modifier = Modifier,
) {
    val activity = LocalActivity.current
    IconButton(
        modifier = modifier.padding(4.dp),
        colors = highIconButtonColors(),
        onClick = {
            openLink(
                activity,
                "https://github.com/leekleak/traffic-light/wiki/Troubleshooting#notifications"
            )
        },
    ) {
        Icon(painterResource(R.drawable.help), stringResource(R.string.help))
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
        setNotificationAccent = {},
        setModeAOD = {},
        setForceFallback = {}
    )
}
