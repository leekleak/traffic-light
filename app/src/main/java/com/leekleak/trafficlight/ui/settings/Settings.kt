package com.leekleak.trafficlight.ui.settings

import android.Manifest
import android.os.Build
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.leekleak.trafficlight.BuildConfig
import com.leekleak.trafficlight.R
import com.leekleak.trafficlight.database.AppPreferenceRepo
import com.leekleak.trafficlight.model.PermissionManager
import com.leekleak.trafficlight.ui.navigation.Navigator
import com.leekleak.trafficlight.ui.navigation.NotificationSettings
import com.leekleak.trafficlight.ui.theme.Theme
import com.leekleak.trafficlight.ui.theme.card
import com.leekleak.trafficlight.util.CategoryTitleSmallText
import com.leekleak.trafficlight.util.categoryTitle
import com.leekleak.trafficlight.util.categoryTitleSmall
import com.leekleak.trafficlight.util.openLink
import com.leekleak.trafficlight.util.px
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject
import rikka.shizuku.Shizuku

@Composable
fun Settings(paddingValues: PaddingValues) {
    val viewModel: SettingsVM = koinViewModel()
    val appPreferenceRepo: AppPreferenceRepo = koinInject()
    val permissionManager: PermissionManager = koinInject()
    val navigator: Navigator = koinInject()
    val activity = LocalActivity.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    LazyColumn(
        Modifier.background(MaterialTheme.colorScheme.surface),
        contentPadding = paddingValues
    ) {
        categoryTitle ({ navigator.goBack() }) { stringResource(R.string.settings) }
        item {
            val backgroundPermission by permissionManager.backgroundPermissionFlow.collectAsState(true)

            if (!backgroundPermission) {
                CategoryTitleSmallText(stringResource(R.string.missing_permissions))
                PermissionCard(
                    title = stringResource(R.string.battery_optimization),
                    description = stringResource(R.string.battery_optimization_description),
                    icon = painterResource(R.drawable.battery),
                    actionButton = {
                        PermissionButton(
                            icon = painterResource(R.drawable.grant),
                            contentDescription = stringResource(R.string.grant),
                            onClick = { permissionManager.askBackgroundPermission(activity) },
                        )
                    }
                )
            }
        }

        categoryTitleSmall { stringResource(R.string.notifications) }
        item {
            val notification by viewModel.notification.collectAsState()
            val notificationPermission by permissionManager.notificationPermissionFlow.collectAsState(true)
            val notificationPermissionCallback = rememberLauncherForActivityResult(
                ActivityResultContracts.RequestPermission()
            ) {
                scope.launch {
                    appPreferenceRepo.setNotification(it)
                    viewModel.runUsageService(it, context)
                }
            }

            SwitchPreference (
                title = stringResource(R.string.notifications),
                summary = stringResource(R.string.notification_description),
                icon = painterResource(R.drawable.notification),
                value = notification,
                onValueChanged = {
                    if (!notificationPermission && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        notificationPermissionCallback.launch(
                            Manifest.permission.POST_NOTIFICATIONS
                        )
                    } else {
                        scope.launch {
                            appPreferenceRepo.setNotification(it)
                            viewModel.runUsageService(it, context)
                        }
                    }
                },
            )
            AnimatedVisibility(
                visible = notification,
                enter = fadeIn() + slideInVertically() + expandVertically(),
                exit = fadeOut() + slideOutVertically() + shrinkVertically()
            ) {
                NavigatePreference(
                    title = stringResource(R.string.advanced_settings),
                    icon = painterResource(R.drawable.notification_settings),
                    onClick = { navigator.goTo(NotificationSettings) }
                )
            }
        }

        categoryTitleSmall { stringResource(R.string.data_plans) }
        item {
            val shizukuTracking by appPreferenceRepo.shizukuTracking.collectAsState(false)
            val shizukuPermission by permissionManager.shizukuPermissionFlow.collectAsState(false)
            val shizukuRunning by permissionManager.shizukuRunningFlow.collectAsState(false)

            SwitchPreference (
                title = stringResource(R.string.multi_sim_tracking),
                summary = if (shizukuRunning) stringResource(R.string.shizuku_required) else stringResource(R.string.shizuku_not_running),
                icon = painterResource(R.drawable.version),
                value = shizukuTracking && shizukuPermission,
                enabled = shizukuRunning,
                onValueChanged = {
                    if (shizukuPermission) {
                        scope.launch {
                            appPreferenceRepo.setShizukuTracking(it)
                        }
                    } else {
                        Shizuku.requestPermission(12199)
                    }
                },
            )
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
                    .card()
                    .horizontalScroll(scroll)
                    .fillMaxWidth()
                    .padding(start = 8.dp, end = 8.dp, bottom = 8.dp, top = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ThemePreferenceContainer(theme, true) { scope.launch { appPreferenceRepo.setTheme(it) } }
                ThemePreferenceContainer(theme, false) { scope.launch { appPreferenceRepo.setTheme(it) } }
            }
        }

        categoryTitleSmall { stringResource(R.string.about) }
        item {
            NavigatePreference(
                title = stringResource(R.string.github),
                summary = stringResource(R.string.github_description),
                icon = painterResource(R.drawable.github),
                onClick = { openLink(activity, "https://github.com/leekleak/traffic-light") },
            )
        }
        item {
            NavigatePreference(
                title = stringResource(R.string.support_development),
                icon = painterResource(R.drawable.donate),
                onClick = { openLink(activity, "https://github.com/sponsors/leekleak") },
            )
        }
        item {
            NavigatePreference(
                title = stringResource(R.string.version, BuildConfig.VERSION_NAME),
                icon = painterResource(R.drawable.version),
                onClick = { viewModel.openAppSettings(activity) },
            )
        }
    }
}