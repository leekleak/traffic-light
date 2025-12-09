package com.leekleak.trafficlight.ui.settings

import androidx.activity.compose.LocalActivity
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.leekleak.trafficlight.BuildConfig
import com.leekleak.trafficlight.R
import com.leekleak.trafficlight.database.TrafficSnapshot
import com.leekleak.trafficlight.ui.theme.Theme
import com.leekleak.trafficlight.ui.theme.card
import com.leekleak.trafficlight.util.categoryTitle
import com.leekleak.trafficlight.util.categoryTitleSmall

@Composable
fun Settings(
    paddingValues: PaddingValues
) {
    val viewModel: SettingsVM = viewModel()
    val activity = LocalActivity.current

    val limitedMode by viewModel.hourlyUsageRepo.limitedMode().collectAsState(false)
    LazyColumn(
        Modifier.background(MaterialTheme.colorScheme.surface),
        contentPadding = paddingValues
    ) {
        item {
            if (limitedMode) {
                Column (
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

        categoryTitle(R.string.settings)
        categoryTitleSmall(R.string.notifications)
        item {
            val notification by viewModel.preferenceRepo.notification.collectAsState(false)
            SwitchPreference(
                title = stringResource(R.string.screen_off_update),
                summary = stringResource(R.string.screen_off_update_description),
                icon = painterResource(R.drawable.aod),
                value = notification,
                onValueChanged = { viewModel.setNotifications(it, activity) }
            )


            AnimatedVisibility(notification) {
                val modeAOD by viewModel.preferenceRepo.modeAOD.collectAsState(false)
                SwitchPreference(
                    title = stringResource(R.string.screen_off_update),
                    summary = stringResource(R.string.screen_off_update_description),
                    icon = painterResource(R.drawable.aod),
                    value = modeAOD,
                    onValueChanged = { viewModel.preferenceRepo.setModeAOD(it) }
                )
            }
            AnimatedVisibility(notification) {
                val bigIcon by viewModel.preferenceRepo.bigIcon.collectAsState(false)
                SwitchPreference(
                    title = stringResource(R.string.oversample_icon),
                    summary = stringResource(R.string.oversample_icon_description),
                    icon = painterResource(R.drawable.oversample),
                    value = bigIcon,
                    onValueChanged = { viewModel.preferenceRepo.setBigIcon(it) }
                )
            }
            AnimatedVisibility(notification) {
                val speedBits by viewModel.preferenceRepo.speedBits.collectAsState(false)
                SwitchPreference(
                    title = stringResource(R.string.speed_in_bits),
                    summary = null,
                    icon = painterResource(R.drawable.speed),
                    value = speedBits,
                    onValueChanged = { viewModel.preferenceRepo.setSpeedBits(it) }
                )
            }
            AnimatedVisibility(notification) {
                val forceFallback by viewModel.preferenceRepo.forceFallback.collectAsState(false)
                val doesFallbackWork = remember { TrafficSnapshot.doesFallbackWork() }
                SwitchPreference(
                    title = stringResource(R.string.force_fallback),
                    summary = if (doesFallbackWork) stringResource(R.string.force_fallback_description) else stringResource(
                        R.string.fallback_unsupported
                    ),
                    icon = painterResource(R.drawable.fallback),
                    value = forceFallback,
                    enabled = doesFallbackWork,
                    onValueChanged = { viewModel.preferenceRepo.setForceFallback(it) }
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