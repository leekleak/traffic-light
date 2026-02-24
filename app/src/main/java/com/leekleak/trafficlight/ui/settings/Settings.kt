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
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import com.leekleak.trafficlight.BuildConfig
import com.leekleak.trafficlight.R
import com.leekleak.trafficlight.database.HourlyUsageRepo
import com.leekleak.trafficlight.database.UsageMode.Limited
import com.leekleak.trafficlight.database.UsageMode.NoPermission
import com.leekleak.trafficlight.database.UsageMode.Unlimited
import com.leekleak.trafficlight.model.PreferenceRepo
import com.leekleak.trafficlight.services.PermissionManager
import com.leekleak.trafficlight.ui.navigation.NotificationSettings
import com.leekleak.trafficlight.ui.theme.Theme
import com.leekleak.trafficlight.ui.theme.card
import com.leekleak.trafficlight.util.CategoryTitleSmallText
import com.leekleak.trafficlight.util.categoryTitle
import com.leekleak.trafficlight.util.categoryTitleSmall
import com.leekleak.trafficlight.util.px
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import rikka.shizuku.Shizuku

@Composable
fun Settings(
    paddingValues: PaddingValues,
    backstack: NavBackStack<NavKey>,
) {
    val viewModel: SettingsVM = viewModel()
    val preferenceRepo: PreferenceRepo = koinInject()
    val permissionManager: PermissionManager = koinInject()
    val hourlyUsageRepo: HourlyUsageRepo = koinInject()
    val activity = LocalActivity.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    LazyColumn(
        Modifier.background(MaterialTheme.colorScheme.surface),
        contentPadding = paddingValues
    ) {
        categoryTitle { stringResource(R.string.settings) }
        item {
            val usageMode by hourlyUsageRepo.usageModeFlow().collectAsState(Unlimited)
            val backgroundPermission by permissionManager.backgroundPermissionFlow.collectAsState(true)
            val shizukuPermission by permissionManager.shizukuPermissionFlow.collectAsState(false)
            val shizukuRunning by permissionManager.shizukuRunningFlow.collectAsState(false)

            if (usageMode != Unlimited || !backgroundPermission) {
                CategoryTitleSmallText(stringResource(R.string.missing_permissions))
            }

            Column (
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (!backgroundPermission) {
                    PermissionCard(
                        title = stringResource(R.string.battery_optimization),
                        description = stringResource(R.string.battery_optimization_description),
                        icon = painterResource(R.drawable.battery),
                        onClick = { permissionManager.askBackgroundPermission(activity) }
                    )
                }
                if (usageMode == NoPermission) {
                    PermissionCard(
                        title = stringResource(R.string.usage_statistics),
                        description = stringResource(R.string.usage_statistics_description),
                        icon = painterResource(R.drawable.usage),
                        onClick = { permissionManager.askUsagePermission(activity) }
                    ) {
                        PermissionButton(
                            icon = painterResource(R.drawable.help),
                            contentDescription = stringResource(R.string.help),
                            colors = IconButtonDefaults.filledIconButtonColors(
                                containerColor = MaterialTheme.colorScheme.surface,
                                contentColor = MaterialTheme.colorScheme.onSurface,
                            ),
                            onClick = { permissionManager.openUsagePermissionHelp(activity) }
                        )
                    }
                }
                if (usageMode == Limited) {
                    Column(
                        modifier = Modifier
                            .padding(vertical = 8.dp)
                            .card()
                            .padding(16.dp)
                    ) {
                        Text(
                            modifier = Modifier.fillMaxWidth(),
                            text = stringResource(R.string.limited_mode),
                            fontWeight = FontWeight(800),
                            color = MaterialTheme.colorScheme.error,
                        )
                        Text(
                            modifier = Modifier.fillMaxWidth(),
                            text = stringResource(R.string.limited_mode_description),
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                }

                if (!shizukuPermission || !shizukuRunning) {
                    PermissionCard (
                        title = stringResource(R.string.shizuku),
                        enabled = shizukuRunning,
                        description = stringResource(R.string.allows_in_depth_data_plan_tracking) +
                                if (!shizukuRunning) " " + stringResource(R.string.shizuku_not_running) else "",
                        icon = painterResource(R.drawable.version),
                        onClick = { Shizuku.requestPermission(12199) },
                    )
                }
            }
        }

        categoryTitleSmall { stringResource(R.string.notifications) }
        item {
            val notification by preferenceRepo.notification.collectAsState(false)
            val notificationPermission by permissionManager.notificationPermissionFlow.collectAsState(true)
            val notificationPermissionCallback = rememberLauncherForActivityResult(
                ActivityResultContracts.RequestPermission()
            ) {
                scope.launch {
                    viewModel.setNotifications(it, context)
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
                            viewModel.setNotifications(it, context)
                        }
                    }
                },
            )
            if (notification) {
                Preference(
                    title = stringResource(R.string.advanced_settings),
                    icon = painterResource(R.drawable.notification_settings),
                    onClick = { backstack.add(NotificationSettings) }
                )
            }
        }

        categoryTitleSmall { stringResource(R.string.ui) }
        item {
            val theme by preferenceRepo.theme.collectAsState(Theme.AutoMaterial)
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
                ThemePreferenceContainer(theme, true) { scope.launch { preferenceRepo.setTheme(it) } }
                ThemePreferenceContainer(theme, false) { scope.launch { preferenceRepo.setTheme(it) } }
            }
        }
        item {
            val expressiveFonts by preferenceRepo.expressiveFonts.collectAsState(true)
            SwitchPreference(
                title = stringResource(R.string.expressive_fonts),
                summary = stringResource(R.string.expressive_fonts_description),
                icon = painterResource(R.drawable.expressive),
                value = expressiveFonts,
                onValueChanged = { scope.launch { preferenceRepo.setExpressiveFonts(it) } }
            )
        }

        categoryTitleSmall { stringResource(R.string.about) }
        item {
            Preference(
                title = stringResource(R.string.github),
                summary = stringResource(R.string.github_description),
                icon = painterResource(R.drawable.github),
                onClick = { viewModel.openLink(activity, "https://github.com/leekleak/traffic-light") },
            )
        }
        item {
            Preference(
                title = stringResource(R.string.support_development),
                icon = painterResource(R.drawable.donate),
                onClick = { viewModel.openLink(activity, "https://github.com/sponsors/leekleak") },
            )
        }
        item {
            Preference(
                title = stringResource(R.string.version, BuildConfig.VERSION_NAME),
                icon = painterResource(R.drawable.version),
                onClick = { viewModel.openAppSettings(activity) },
            )
        }
    }
}