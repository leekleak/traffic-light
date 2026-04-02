package com.leekleak.trafficlight.ui.settings

import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.leekleak.trafficlight.R
import com.leekleak.trafficlight.database.AppPreferenceRepo
import com.leekleak.trafficlight.database.TrafficSnapshot
import com.leekleak.trafficlight.services.UsageService.Companion.NOTIFICATION_CHANNEL_ID
import com.leekleak.trafficlight.services.UsageService.Companion.NOTIFICATION_CHANNEL_ID_SILENT
import com.leekleak.trafficlight.ui.navigation.Navigator
import com.leekleak.trafficlight.util.categoryTitle
import com.leekleak.trafficlight.util.categoryTitleSmall
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject

@Composable
fun NotificationSettings(paddingValues: PaddingValues) {
    val appPreferenceRepo: AppPreferenceRepo = koinInject()
    val navigator: Navigator = koinInject()
    val viewModel: SettingsVM = koinViewModel()
    val activity = LocalActivity.current
    val scope = rememberCoroutineScope()

    LazyColumn(
        Modifier
            .background(MaterialTheme.colorScheme.surface)
            .fillMaxSize(),
        contentPadding = paddingValues
    ) {
        categoryTitle ({ navigator.goBack() }) { stringResource(R.string.notifications) }
        categoryTitleSmall { stringResource(R.string.appearance) }
        item {
            val bigIcon by appPreferenceRepo.bigIcon.collectAsState(false)
            SwitchPreference(
                title = stringResource(R.string.oversample_icon),
                summary = stringResource(R.string.oversample_icon_description),
                icon = painterResource(R.drawable.oversample),
                value = bigIcon,
                onValueChanged = { scope.launch { appPreferenceRepo.setBigIcon(it) } }
            )
        }
        item {
            val speedBits by appPreferenceRepo.speedBits.collectAsState(false)
            SwitchPreference(
                title = stringResource(R.string.speed_in_bits),
                summary = null,
                icon = painterResource(R.drawable.speed),
                value = speedBits,
                onValueChanged = { scope.launch { appPreferenceRepo.setSpeedBits(it) } }
            )
        }

        categoryTitleSmall { stringResource(R.string.behavior) }
        item {
            val modeAOD by appPreferenceRepo.modeAOD.collectAsState(false)
            SwitchPreference(
                title = stringResource(R.string.screen_off_update),
                summary = stringResource(R.string.screen_off_update_description),
                icon = painterResource(R.drawable.aod),
                value = modeAOD,
                onValueChanged = { scope.launch { appPreferenceRepo.setModeAOD(it) } }
            )
        }
        item {
            val altVpn by appPreferenceRepo.altVpn.collectAsState(false)
            SwitchPreference(
                title = stringResource(R.string.alt_vpn_workaround),
                summary = stringResource(R.string.alt_vpn_workaround_description),
                icon = painterResource(R.drawable.vpn),
                value = altVpn,
                onValueChanged = { scope.launch { appPreferenceRepo.setAltVpn(it) } }
            )
        }
        item {
            val forceFallback by appPreferenceRepo.forceFallback.collectAsState(false)
            val doesFallbackWork = remember { TrafficSnapshot.doesFallbackWork() }
            SwitchPreference(
                title = stringResource(R.string.force_fallback),
                summary = if (doesFallbackWork) stringResource(R.string.force_fallback_description)
                          else stringResource(R.string.fallback_unsupported),
                icon = painterResource(R.drawable.fallback),
                value = forceFallback,
                enabled = doesFallbackWork,
                onValueChanged = { scope.launch { appPreferenceRepo.setForceFallback(it) } }
            )
        }

        categoryTitleSmall { stringResource(R.string.notification_channels) }
        item {
            NavigatePreference(
                title = stringResource(R.string.connected_to_network),
                icon = painterResource(R.drawable.bigtop_updates),
                onClick = { viewModel.openNotificationChannelSettings(activity, NOTIFICATION_CHANNEL_ID) },
            )
        }
        item {
            NavigatePreference(
                title = stringResource(R.string.disconnected_from_network),
                icon = painterResource(R.drawable.signal_disconnected),
                onClick = { viewModel.openNotificationChannelSettings(activity, NOTIFICATION_CHANNEL_ID_SILENT) },
            )
        }
    }
}