package com.leekleak.trafficlight.ui.settings

import android.Manifest
import android.os.Build
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.leekleak.core.R
import com.leekleak.core.ui.components.BackAction
import com.leekleak.core.ui.components.HazeScaffold
import com.leekleak.core.ui.navigation.Navigator
import com.leekleak.core.ui.theme.card
import com.leekleak.core.ui.theme.googleSans
import com.leekleak.core.util.CategoryTitleSmallText
import com.leekleak.core.util.SlideAnimatedVisibility
import com.leekleak.core.util.openLink
import com.leekleak.trafficlight.BuildConfig
import com.leekleak.trafficlight.database.AppPreferenceRepo
import com.leekleak.trafficlight.integrations.ShizukuServicesProvider
import com.leekleak.trafficlight.model.PermissionManager
import com.leekleak.trafficlight.ui.navigation.LibraryLicenseScreen
import com.leekleak.trafficlight.ui.navigation.NotificationSettingsKey
import kotlinx.coroutines.launch

@Composable
fun Settings(
    navigator: Navigator,
    viewModel: SettingsVM,
    appPreferenceRepo: AppPreferenceRepo,
    permissionManager: PermissionManager,
    shizukuServicesProvider: ShizukuServicesProvider,
) {
    var showWarning by remember { mutableStateOf(false) }
    if (showWarning) {
        NotificationWarningDialog(onDismiss = { showWarning = false })
    }

    HazeScaffold(
        title = stringResource(R.string.settings),
        verticalArrangement = Arrangement.Top,
        backAction = BackAction.Normal(navigator),
    ) {
        BackgroundPermissionPrompt(permissionManager)
        NotificationSettings(
            navigator = navigator,
            viewModel = viewModel,
            permissionManager = permissionManager,
            appPreferenceRepo = appPreferenceRepo,
        ) { showWarning = true }
        if (BuildConfig.SHIZUKU) {
            ShizukuSettings(
                appPreferenceRepo = appPreferenceRepo,
                permissionManager = permissionManager,
                shizukuServicesProvider = shizukuServicesProvider
            )
        }
        BasesSettings(appPreferenceRepo)
        InterfaceSettings(appPreferenceRepo)
        AboutSettings(navigator, appPreferenceRepo)
    }
}

@Composable
private fun AboutSettings(navigator: Navigator, appPreferenceRepo: AppPreferenceRepo) {
    val activity = LocalActivity.current
    val scope = rememberCoroutineScope()

    CategoryTitleSmallText(stringResource(R.string.about))
    val ads by appPreferenceRepo.ads.collectAsState(false)
    SwitchPreference(
        title = stringResource(R.string.supporter_ads),
        summary = stringResource(
            if (BuildConfig.ADS) R.string.supporter_ads_description
            else R.string.supporter_ads_disabled
        ),
        icon = R.drawable.celebration,
        value = ads,
        enabled = BuildConfig.ADS,
        onValueChanged = { scope.launch { appPreferenceRepo.setAds(it) } },
    )

    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        NavigatePreference(
            modifier = Modifier.weight(1f),
            title = stringResource(R.string.donate),
            icon = R.drawable.donate,
            onClick = { openLink(activity, "https://github.com/sponsors/leekleak") },
            showControl = false
        )
        NavigatePreferenceIcon(
            icon = painterResource(R.drawable.github),
            contentDescription = stringResource(R.string.github),
            onClick = { openLink(activity, "https://github.com/leekleak/traffic-light") },
        )
        NavigatePreferenceIcon(
            icon = painterResource(R.drawable.gavel),
            contentDescription = stringResource(R.string.licenses),
            onClick = { navigator.goTo(LibraryLicenseScreen) },
        )
    }

    val fontFamily = remember { googleSans(weight = 600f, roundness = 100f) }
    Text(
        modifier = Modifier
            .fillMaxWidth()
            .alpha(0.6f)
            .padding(vertical = 4.dp),
        fontFamily = fontFamily,
        text = stringResource(R.string.version_short, BuildConfig.VERSION_NAME),
        textAlign = TextAlign.Center
    )
}

@Composable
private fun InterfaceSettings(appPreferenceRepo: AppPreferenceRepo) {
    val scope = rememberCoroutineScope()

    CategoryTitleSmallText(stringResource(R.string.ui))
    val theme by appPreferenceRepo.theme.collectAsState()
    val blur by appPreferenceRepo.blur.collectAsState(true)
    val scroll = rememberScrollState(0)

    val spacing = 8.dp
    val spacingPx = with(LocalDensity.current) { spacing.roundToPx() }
    var panelWidth by remember { mutableIntStateOf(0) }
    var aligned by remember { mutableStateOf(false) }

    LaunchedEffect(panelWidth) {
        if (panelWidth == 0) return@LaunchedEffect
        scroll.scrollTo(if (theme.isMaterial) 0 else panelWidth + spacingPx)
        aligned = true
    }
    Row(
        modifier = Modifier
            .padding(vertical = 4.dp)
            .fillMaxWidth()
            .card()
            .horizontalScroll(scroll)
            .padding(start = 8.dp, end = 8.dp, bottom = 8.dp, top = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(spacing)
    ) {
        ThemePreferenceContainer(
            currentTheme = theme,
            material = true,
            modifier = Modifier.onSizeChanged { panelWidth = it.width },
        ) { scope.launch { appPreferenceRepo.setTheme(it) } }
        ThemePreferenceContainer(theme, false) { scope.launch { appPreferenceRepo.setTheme(it) } }
    }
    SwitchPreference(
        title = stringResource(R.string.blur_effects),
        icon = R.drawable.blur_on,
        value = blur,
        onValueChanged = { scope.launch { appPreferenceRepo.setBlur(it) } },
    )
}

@Composable
private fun BasesSettings(appPreferenceRepo: AppPreferenceRepo) {
    val scope = rememberCoroutineScope()

    CategoryTitleSmallText(stringResource(R.string.sizes))
    val speedMetric by appPreferenceRepo.speedMetric.collectAsState(false)
    val sizeMetric by appPreferenceRepo.sizeMetric.collectAsState(false)
    val values = listOf(
        true to stringResource(R.string.base_1000),
        false to stringResource(R.string.base_1024)
    )
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        DialogItemSelectPreference(
            modifier = Modifier.weight(1f),
            title = stringResource(R.string.speed_unit),
            icon = R.drawable.network_check,
            value = speedMetric,
            values = values,
            onValueChanged = { scope.launch { appPreferenceRepo.setSpeedMetric(it) } },
        )
        DialogItemSelectPreference(
            modifier = Modifier.weight(1f),
            title = stringResource(R.string.size_unit),
            icon = R.drawable.database,
            value = sizeMetric,
            values = values,
            onValueChanged = { scope.launch { appPreferenceRepo.setSizeMetric(it) } },
        )
    }
}

@Composable
private fun ShizukuSettings(
    appPreferenceRepo: AppPreferenceRepo,
    permissionManager: PermissionManager,
    shizukuServicesProvider: ShizukuServicesProvider
) {
    val scope = rememberCoroutineScope()

    CategoryTitleSmallText(stringResource(R.string.data_plans))
    val shizukuTracking by appPreferenceRepo.shizukuTracking.collectAsState(false)
    val shizukuPermission by permissionManager.shizukuPermission.collectAsState()
    val shizukuRunning by permissionManager.shizukuRunning.collectAsState()
    var expectingPermissionChange by remember { mutableStateOf(false) }
    LaunchedEffect(shizukuPermission) {
        if (shizukuPermission && expectingPermissionChange) {
            scope.launch { appPreferenceRepo.setShizukuTracking(true) }
        }
    }
    SwitchPreference(
        title = stringResource(R.string.multi_sim_tracking),
        summary = if (shizukuRunning) stringResource(R.string.shizuku_required) else stringResource(
            R.string.shizuku_not_running
        ),
        icon = R.drawable.version,
        value = shizukuTracking && shizukuPermission,
        enabled = shizukuRunning,
        onValueChanged = {
            if (shizukuPermission) {
                scope.launch { appPreferenceRepo.setShizukuTracking(it) }
            } else {
                shizukuServicesProvider.shizukuRequestPermission()
                expectingPermissionChange = true
            }
        },
    )
}

@Composable
private fun BackgroundPermissionPrompt(permissionManager: PermissionManager) {
    val activity = LocalActivity.current
    val backgroundPermission by permissionManager.backgroundPermission.collectAsStateWithLifecycle()
    SlideAnimatedVisibility(!backgroundPermission) {
        CategoryTitleSmallText(stringResource(R.string.missing_permissions))
        PermissionCard(
            title = stringResource(R.string.battery_optimization),
            description = stringResource(R.string.battery_optimization_description),
            icon = painterResource(R.drawable.battery),
            onClick = { permissionManager.askBackgroundPermission(activity) }
        )
    }
}

@Composable
private fun NotificationSettings(
    navigator: Navigator,
    viewModel: SettingsVM,
    permissionManager: PermissionManager,
    appPreferenceRepo: AppPreferenceRepo,
    showWarning: () -> Unit
) {
    val scope = rememberCoroutineScope()

    CategoryTitleSmallText(stringResource(R.string.notifications))
    val notification by viewModel.notification.collectAsStateWithLifecycle()
    val activePlanNotificationsCount by viewModel.activePlanNotificationsCount.collectAsStateWithLifecycle()
    val notificationPermission by permissionManager.notificationPermission.collectAsStateWithLifecycle()

    var expectingNotificationPermission by remember { mutableStateOf(false) }

    fun setNotificationsEnabled(enabled: Boolean) {
        if (enabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.BAKLAVA && activePlanNotificationsCount > 0) {
            showWarning()
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
        title = stringResource(R.string.notifications),
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

    SlideAnimatedVisibility(notification) {
        NavigatePreference(
            title = stringResource(R.string.advanced_settings),
            icon = R.drawable.notification_settings,
            onClick = { navigator.goTo(NotificationSettingsKey) }
        )
    }
}