package com.leekleak.trafficlight.ui.settings

import android.os.Build
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
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
            val modeAOD by viewModel.preferenceRepo.modeAOD.collectAsState(false)
            SwitchPreference(
                title = stringResource(R.string.screen_off_update),
                summary = stringResource(R.string.screen_off_update_description),
                icon = painterResource(R.drawable.aod),
                value = modeAOD,
                onValueChanged = { viewModel.preferenceRepo.setModeAOD(it) }
            )
        }
        item {
            val bigIcon by viewModel.preferenceRepo.bigIcon.collectAsState(false)
            SwitchPreference(
                title = stringResource(R.string.oversample_icon),
                summary = stringResource(R.string.oversample_icon_description),
                icon = painterResource(R.drawable.oversample),
                value = bigIcon,
                onValueChanged = { viewModel.preferenceRepo.setBigIcon(it) }
            )
        }
        item {
            val speedBits by viewModel.preferenceRepo.speedBits.collectAsState(false)
            SwitchPreference(
                title = stringResource(R.string.speed_in_bits),
                summary = null,
                icon = painterResource(R.drawable.speed),
                value = speedBits,
                onValueChanged = { viewModel.preferenceRepo.setSpeedBits(it) }
            )
        }
        item {
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

        categoryTitleSmall(R.string.ui)
        item {
            val dynamicColor by viewModel.preferenceRepo.dynamicColor.collectAsState(false)
            SwitchPreference (
                title = stringResource(R.string.dynamic_color),
                icon = painterResource(R.drawable.theme),
                value = dynamicColor && Build.VERSION.SDK_INT >= 31,
                enabled = Build.VERSION.SDK_INT >= 31,
                onValueChanged = { viewModel.preferenceRepo.setDynamicColor(it) },
            )
        }
        item {
            val improveContrast by viewModel.preferenceRepo.improveContrast.collectAsState(false)
            SwitchPreference (
                title = stringResource(R.string.improve_contrast),
                icon = painterResource(R.drawable.contrast),
                value = improveContrast,
                onValueChanged = { viewModel.preferenceRepo.setImproveContrast(it) },
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