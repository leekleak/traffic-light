package com.leekleak.trafficlight.ui.settings

import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.compose.viewModel
import com.leekleak.trafficlight.BuildConfig
import com.leekleak.trafficlight.R
import com.leekleak.trafficlight.database.TrafficSnapshot
import com.leekleak.trafficlight.util.categoryTitle
import com.leekleak.trafficlight.util.categoryTitleSmall

@Composable
fun Settings(
    paddingValues: PaddingValues
) {
    val viewModel: SettingsVM = viewModel()
    val activity = LocalActivity.current
    LazyColumn(
        Modifier.background(MaterialTheme.colorScheme.surface),
        contentPadding = paddingValues
    ) {
        categoryTitle(R.string.settings)
        categoryTitleSmall(R.string.notifications)
        item {
            val modeAOD by viewModel.modeAOD.collectAsState()
            SwitchPreference(
                title = stringResource(R.string.screen_off_update),
                summary = stringResource(R.string.screen_off_update_description),
                icon = painterResource(R.drawable.aod),
                value = modeAOD,
                onValueChanged = { viewModel.setModeAOD(it) }
            )
        }
        item {
            val bigIcon by viewModel.bigIcon.collectAsState()
            SwitchPreference(
                title = stringResource(R.string.oversample_icon),
                summary = stringResource(R.string.oversample_icon_description),
                icon = painterResource(R.drawable.oversample),
                value = bigIcon,
                onValueChanged = { viewModel.setBigIcon(it) }
            )
        }
        item {
            val speedBits by viewModel.speedBits.collectAsState()
            SwitchPreference(
                title = stringResource(R.string.speed_in_bits),
                summary = null,
                icon = painterResource(R.drawable.speed),
                value = speedBits,
                onValueChanged = { viewModel.setSpeedBits(it) }
            )
        }
        item {
            val forceFallback by viewModel.forceFallback.collectAsState()
            val doesFallbackWork = remember { TrafficSnapshot.doesFallbackWork() }
            SwitchPreference(
                title = stringResource(R.string.force_fallback),
                summary = if (doesFallbackWork) stringResource(R.string.force_fallback_description) else stringResource(
                    R.string.fallback_unsupported
                ),
                icon = painterResource(R.drawable.fallback),
                value = forceFallback,
                enabled = doesFallbackWork,
                onValueChanged = { viewModel.setForceFallback(it) }
            )
        }

        categoryTitleSmall(R.string.history)
        item {
            val dbSize by viewModel.dbSize.collectAsState()
            Preference(
                title = stringResource(R.string.clear_history),
                summary = stringResource(R.string.clear_history_description),
                icon = painterResource(R.drawable.clear_history),
                onClick = { viewModel.clearDB() },
                controls = {
                    Text(pluralStringResource(R.plurals.days, dbSize / 24, dbSize / 24))
                }
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