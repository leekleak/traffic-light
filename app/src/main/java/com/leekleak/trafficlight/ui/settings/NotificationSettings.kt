package com.leekleak.trafficlight.ui.settings

import android.os.Build
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
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
import androidx.compose.ui.unit.dp
import com.leekleak.trafficlight.R
import com.leekleak.trafficlight.database.AppPreferenceRepo
import com.leekleak.trafficlight.database.TrafficSnapshot
import com.leekleak.trafficlight.services.notifications.SpeedNotification.Companion.NOTIFICATION_CHANNEL_ID_SILENT
import com.leekleak.trafficlight.util.PageTitle
import com.leekleak.trafficlight.util.categoryTitleSmall
import com.leekleak.trafficlight.util.openLink
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.rememberHazeState
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject

@Composable
fun NotificationSettings(paddingValues: PaddingValues) {
    val appPreferenceRepo: AppPreferenceRepo = koinInject()
    val viewModel: SettingsVM = koinViewModel()
    val activity = LocalActivity.current
    val scope = rememberCoroutineScope()
    val hazeState = rememberHazeState()

    LazyColumn(
        Modifier
            .background(MaterialTheme.colorScheme.surface)
            .fillMaxSize()
            .hazeSource(hazeState),
        contentPadding = paddingValues
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.BAKLAVA) {
            categoryTitleSmall { stringResource(R.string.live_notification) }
            item {
                val liveNotification by viewModel.liveNotification.collectAsState()
                Row (
                    modifier = Modifier.height(IntrinsicSize.Min),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SwitchPreference(
                        modifier = Modifier.weight(1f),
                        title = stringResource(R.string.live_notification),
                        icon = painterResource(R.drawable.app_badging),
                        value = liveNotification,
                        onValueChanged = { scope.launch { appPreferenceRepo.setLiveNotification(it) } }
                    )
                    IconPreference(
                        title = stringResource(R.string.help),
                        painter = painterResource(R.drawable.help),
                        onClick = { openLink(activity, "https://github.com/leekleak/traffic-light/wiki/Troubleshooting#notifications") },
                    )
                }
            }
        }
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
            Row (
                modifier = Modifier.height(IntrinsicSize.Min),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                NavigatePreference(
                    modifier = Modifier.weight(1f),
                    title = stringResource(R.string.disconnected_from_network),
                    icon = painterResource(R.drawable.signal_disconnected),
                    onClick = { viewModel.openNotificationChannelSettings(activity, NOTIFICATION_CHANNEL_ID_SILENT) },
                )
                IconPreference(
                    title = stringResource(R.string.help),
                    painter = painterResource(R.drawable.help),
                    onClick = { openLink(activity, "https://github.com/leekleak/traffic-light/wiki/Hide-status-bar-icon-when-disconnected") },
                )
            }
        }
    }
    PageTitle (true, hazeState, stringResource(R.string.notifications))
}