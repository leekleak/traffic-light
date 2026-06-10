package com.leekleak.trafficlight.ui.settings

import android.Manifest
import android.os.Build
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.leekleak.trafficlight.BuildConfig
import com.leekleak.trafficlight.R
import com.leekleak.trafficlight.database.AppPreferenceRepo
import com.leekleak.trafficlight.integrations.ShizukuServicesProvider
import com.leekleak.trafficlight.model.PermissionManager
import com.leekleak.trafficlight.ui.navigation.LibraryLicenseScreen
import com.leekleak.trafficlight.ui.navigation.Navigator
import com.leekleak.trafficlight.ui.navigation.NotificationSettingsKey
import com.leekleak.trafficlight.ui.theme.Theme
import com.leekleak.trafficlight.ui.theme.card
import com.leekleak.trafficlight.ui.theme.googleSans
import com.leekleak.trafficlight.util.CategoryTitleSmallText
import com.leekleak.trafficlight.util.PageTitle
import com.leekleak.trafficlight.util.SlideAnimatedVisibility
import com.leekleak.trafficlight.util.categoryTitleSmall
import com.leekleak.trafficlight.util.openLink
import com.leekleak.trafficlight.util.px
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.rememberHazeState
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject

@Composable
fun Settings(paddingValues: PaddingValues) {
    val viewModel: SettingsVM = koinViewModel()
    val appPreferenceRepo: AppPreferenceRepo = koinInject()
    val permissionManager: PermissionManager = koinInject()
    val shizukuServicesProvider: ShizukuServicesProvider = koinInject()
    val navigator: Navigator = koinInject()
    val activity = LocalActivity.current
    val scope = rememberCoroutineScope()
    val hazeState = rememberHazeState()

    var showWarning by remember { mutableStateOf(false) }
    if (showWarning) {
        NotificationWarningDialog(onDismiss = { showWarning = false })
    }

    LazyColumn(
        Modifier
            .background(MaterialTheme.colorScheme.surface)
            .hazeSource(hazeState),
        contentPadding = paddingValues
    ) {
        item {
            val backgroundPermission by permissionManager.backgroundPermissionFlow.collectAsState()

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

        categoryTitleSmall { stringResource(R.string.notifications) }
        item {
            val notification by viewModel.notification.collectAsState()
            val activePlanNotificationsCount by viewModel.activePlanNotificationsCount.collectAsState()
            val notificationPermission by permissionManager.notificationPermissionFlow.collectAsState()
            val notificationPermissionCallback = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {}

            SlideAnimatedVisibility(!notificationPermission) {
                PermissionCard(
                    title = stringResource(R.string.notification_permission),
                    description = stringResource(R.string.allow_app_to_send_notifications),
                    icon = painterResource(R.drawable.notification),
                    onClick = {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            notificationPermissionCallback.launch(
                                Manifest.permission.POST_NOTIFICATIONS
                            )
                        }
                    }
                )
            }

            SwitchPreference (
                title = stringResource(R.string.notifications),
                summary = stringResource(R.string.notification_description),
                icon = painterResource(R.drawable.speed_notification),
                value = notification,
                enabled = notificationPermission,
                onValueChanged = {
                    if (it && Build.VERSION.SDK_INT >= Build.VERSION_CODES.BAKLAVA && activePlanNotificationsCount > 0) {
                        showWarning = true
                    }
                    scope.launch {
                        appPreferenceRepo.setNotification(it)
                    }
                },
            )

            SlideAnimatedVisibility(notification) {
                NavigatePreference(
                    title = stringResource(R.string.advanced_settings),
                    icon = painterResource(R.drawable.notification_settings),
                    enabled = notificationPermission,
                    onClick = { navigator.goTo(NotificationSettingsKey) }
                )
            }
        }

        if (BuildConfig.SHIZUKU) {
            categoryTitleSmall { stringResource(R.string.data_plans) }
            item {
                val shizukuTracking by appPreferenceRepo.shizukuTracking.collectAsState(false)
                val shizukuPermission by permissionManager.shizukuPermissionFlow.collectAsState(false)
                val shizukuRunning by permissionManager.shizukuRunningFlow.collectAsState(false)
                var expectingPermissionChange by remember { mutableStateOf(false) }
                LaunchedEffect(shizukuPermission) {
                    if (shizukuPermission && expectingPermissionChange) {
                        scope.launch { appPreferenceRepo.setShizukuTracking(true) }
                    }
                }
                SwitchPreference(
                    title = stringResource(R.string.multi_sim_tracking),
                    summary = if (shizukuRunning) stringResource(R.string.shizuku_required) else stringResource(R.string.shizuku_not_running),
                    icon = painterResource(R.drawable.version),
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
        }

        categoryTitleSmall { stringResource(R.string.ui) }
        item {
            val theme by appPreferenceRepo.theme.collectAsState(Theme.AutoMaterial)
            val scroll = rememberScrollState(0)

            val panelWidth = 272.dp.px.toInt() // Just a guess lol. Calculate the actual size if I ever add more themes
            LaunchedEffect(theme) {
                scroll.animateScrollTo(panelWidth * (theme.ordinal / 3), tween())
            }
            Row (
                modifier = Modifier
                    .padding(vertical = 4.dp)
                    .fillMaxWidth()
                    .card()
                    .horizontalScroll(scroll)
                    .padding(start = 8.dp, end = 8.dp, bottom = 8.dp, top = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ThemePreferenceContainer(theme, true) { scope.launch { appPreferenceRepo.setTheme(it) } }
                ThemePreferenceContainer(theme, false) { scope.launch { appPreferenceRepo.setTheme(it) } }
            }
        }

        categoryTitleSmall { stringResource(R.string.about) }
        item {
            val ads by appPreferenceRepo.ads.collectAsState(false)
            SwitchPreference(
                title = stringResource(R.string.supporter_ads),
                summary = stringResource(
                    if (BuildConfig.ADS) R.string.supporter_ads_description
                    else R.string.supporter_ads_disabled
                ),
                icon = painterResource(R.drawable.celebration),
                value = ads,
                enabled = BuildConfig.ADS,
                onValueChanged = { scope.launch { appPreferenceRepo.setAds(it) } },
            )
        }
        item {
            Row (horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                NavigatePreference(
                    modifier = Modifier.weight(1f),
                    title = stringResource(R.string.donate),
                    icon = painterResource(R.drawable.donate),
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
        }
        item {
            val fontFamily = remember { googleSans(weight = 600f, roundness = 100f) }
            Text(
                modifier = Modifier.fillMaxWidth().alpha(0.6f).padding(vertical = 4.dp),
                fontFamily = fontFamily,
                text = stringResource(R.string.version_short, BuildConfig.VERSION_NAME),
                textAlign = TextAlign.Center
            )
        }
    }

    PageTitle (true, hazeState, stringResource(R.string.settings))
}