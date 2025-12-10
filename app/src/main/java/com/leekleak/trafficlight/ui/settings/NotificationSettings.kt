package com.leekleak.trafficlight.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.compose.viewModel
import com.leekleak.trafficlight.R
import com.leekleak.trafficlight.database.TrafficSnapshot
import com.leekleak.trafficlight.util.categoryTitle

@Composable
fun NotificationSettings(paddingValues: PaddingValues) {
    val viewModel: SettingsVM = viewModel()

    LazyColumn(
        Modifier.background(MaterialTheme.colorScheme.surface),
        contentPadding = paddingValues
    ) {
        categoryTitle(R.string.notifications)
        item {
            val modeAOD by viewModel.preferenceRepo.modeAOD.collectAsState(false)
            SwitchPreference(
                title = stringResource(R.string.screen_off_update),
                summary = stringResource(R.string.screen_off_update_description),
                icon = painterResource(R.drawable.aod),
                value = modeAOD,
                onValueChanged = { viewModel.preferenceRepo.setModeAOD(it) }
            )
            val bigIcon by viewModel.preferenceRepo.bigIcon.collectAsState(false)
            SwitchPreference(
                title = stringResource(R.string.oversample_icon),
                summary = stringResource(R.string.oversample_icon_description),
                icon = painterResource(R.drawable.oversample),
                value = bigIcon,
                onValueChanged = { viewModel.preferenceRepo.setBigIcon(it) }
            )
            val speedBits by viewModel.preferenceRepo.speedBits.collectAsState(false)
            SwitchPreference(
                title = stringResource(R.string.speed_in_bits),
                summary = null,
                icon = painterResource(R.drawable.speed),
                value = speedBits,
                onValueChanged = { viewModel.preferenceRepo.setSpeedBits(it) }
            )

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
}