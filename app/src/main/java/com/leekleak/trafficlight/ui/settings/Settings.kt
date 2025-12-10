package com.leekleak.trafficlight.ui.settings

import android.Manifest
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
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
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import com.leekleak.trafficlight.database.UsageMode.Limited
import com.leekleak.trafficlight.database.UsageMode.NoPermission
import com.leekleak.trafficlight.database.UsageMode.Unlimited
import com.leekleak.trafficlight.ui.navigation.NotificationSettings
import com.leekleak.trafficlight.ui.theme.Theme
import com.leekleak.trafficlight.ui.theme.card
import com.leekleak.trafficlight.util.CategoryTitleSmallText
import com.leekleak.trafficlight.util.categoryTitle
import com.leekleak.trafficlight.util.categoryTitleSmall
import kotlinx.coroutines.launch

@Composable
fun Settings(
    paddingValues: PaddingValues,
    backstack: NavBackStack<NavKey>,
) {
    val viewModel: SettingsVM = viewModel()
    val activity = LocalActivity.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    LazyColumn(
        Modifier.background(MaterialTheme.colorScheme.surface),
        contentPadding = paddingValues
    ) {
        categoryTitle(R.string.settings)
        item {
            val usageMode by viewModel.hourlyUsageRepo.usageModeFlow().collectAsState(Unlimited)
            val backgroundPermission by viewModel.permissionManager.backgroundPermissionFlow.collectAsState(true)

            if (usageMode != Unlimited || !backgroundPermission) {
                CategoryTitleSmallText(stringResource(R.string.missing_permissions))
            }

            Column (
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AnimatedPreference(!backgroundPermission) {
                    PermissionCard(
                        title = stringResource(R.string.battery_optimization),
                        description = stringResource(R.string.battery_optimization_description),
                        icon = painterResource(R.drawable.battery),
                        onClick = { viewModel.permissionManager.askBackgroundPermission(activity) }
                    )
                }
                AnimatedPreference(usageMode == NoPermission) {
                    PermissionCard(
                        title = stringResource(R.string.usage_statistics),
                        description = stringResource(R.string.usage_statistics_description),
                        icon = painterResource(R.drawable.usage),
                        onClick = { viewModel.permissionManager.askUsagePermission(activity) }
                    ) {
                        FloatingActionButton(
                            containerColor = MaterialTheme.colorScheme.surface,
                            contentColor = MaterialTheme.colorScheme.onSurface,
                            onClick = { viewModel.permissionManager.openUsagePermissionHelp(activity) }
                        ) {
                            Icon(
                                painterResource(R.drawable.help),
                                contentDescription = stringResource(R.string.help)
                            )
                        }
                    }
                }
                AnimatedPreference(usageMode == Limited) {
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
            }
        }

        categoryTitleSmall(R.string.notifications)
        item {
            val notification by viewModel.preferenceRepo.notification.collectAsState(false)
            val notificationPermission by viewModel.permissionManager.notificationPermissionFlow.collectAsState(true)
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
                    if (!notificationPermission) {
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
            AnimatedPreference(notification) {
                Preference(
                    title = stringResource(R.string.advanced_settings),
                    icon = painterResource(R.drawable.notification_settings),
                    onClick = { backstack.add(NotificationSettings) }
                )
            }
        }

        categoryTitleSmall(R.string.ui)
        item {
            val theme by viewModel.preferenceRepo.theme.collectAsState(Theme.AutoMaterial)
            val scroll = rememberScrollState()
            Row(
                modifier = Modifier
                    .padding(vertical = 4.dp)
                    .card()
                    .horizontalScroll(scroll)
                    .fillMaxWidth()
                    .padding(start = 8.dp, end = 8.dp, bottom = 8.dp, top = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ThemePreferenceContainer(theme, true) { viewModel.preferenceRepo.setTheme(it) }
                ThemePreferenceContainer(theme, false) { viewModel.preferenceRepo.setTheme(it) }
            }
        }
        item {
            val expressiveFonts by viewModel.preferenceRepo.expressiveFonts.collectAsState(true)
            SwitchPreference(
                title = stringResource(R.string.expressive_fonts),
                icon = painterResource(R.drawable.expressive),
                value = expressiveFonts,
                onValueChanged = { viewModel.preferenceRepo.setExpressiveFonts(it) }
            )
        }

        categoryTitleSmall(R.string.about)
        item {
            Preference(
                title = stringResource(R.string.github),
                summary = stringResource(R.string.github_description),
                icon = painterResource(R.drawable.github),
                onClick = { viewModel.openGithub(activity) },
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

@Composable
fun AnimatedPreference(visible: Boolean, content: @Composable () -> Unit) {
    AnimatedVisibility(
        visible = visible,
        enter = slideInVertically() + expandVertically() + fadeIn(),
        exit = slideOutVertically() + shrinkVertically() + fadeOut()
    ) {
        content()
    }
}