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
import com.leekleak.trafficlight.services.notifications.SpeedNotification.Companion.NOTIFICATION_CHANNEL_ID_LOW_SPEED
import com.leekleak.trafficlight.util.DataSize
import com.leekleak.trafficlight.util.PageTitle
import com.leekleak.trafficlight.util.categoryTitleSmall
import com.leekleak.trafficlight.util.openLink
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.rememberHazeState
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject
import kotlin.math.roundToLong

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
        categoryTitleSmall { stringResource(R.string.appearance) }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.BAKLAVA) {
            item {
                val liveNotification by viewModel.liveNotification.collectAsState()
                val separateUpDown by appPreferenceRepo.separateUpDown.collectAsState(false)
                Row (
                    modifier = Modifier.height(IntrinsicSize.Min),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SwitchPreference(
                        modifier = Modifier.weight(1f),
                        title = stringResource(R.string.live_notification),
                        icon = painterResource(R.drawable.app_badging),
                        value = liveNotification,
                        enabled = !separateUpDown,
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
        item {
            val separateUpDown by appPreferenceRepo.separateUpDown.collectAsState(false)
            val liveNotification by viewModel.liveNotification.collectAsState()
            SwitchPreference(
                title = stringResource(R.string.separate_upload_and_download),
                summary = null,
                icon = painterResource(R.drawable.speed_separate),
                value = separateUpDown,
                enabled = !liveNotification,
                onValueChanged = { scope.launch { appPreferenceRepo.setSeparateUpDown(it) } }
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

        categoryTitleSmall { stringResource(R.string.speed_threshold) }
        item {
            val speedThresholdEnabled by appPreferenceRepo.speedThresholdEnabled.collectAsState(false)
            SwitchPreference(
                title = stringResource(R.string.speed_threshold_switch),
                summary = stringResource(R.string.speed_threshold_switch_description),
                icon = painterResource(R.drawable.speed_notification),
                value = speedThresholdEnabled,
                onValueChanged = { scope.launch { appPreferenceRepo.setSpeedThresholdEnabled(it) } }
            )
        }
        item {
            val speedThresholdEnabled by appPreferenceRepo.speedThresholdEnabled.collectAsState(false)
            val speedThresholdBytes by appPreferenceRepo.speedThresholdBytes.collectAsState(128L * 1024L)
            val speedBits by appPreferenceRepo.speedBits.collectAsState(false)
            val speedLabel = DataSize(speedThresholdBytes).toString(speed = true, inBits = speedBits)

            SliderPreference(
                title = stringResource(R.string.speed_threshold_threshold),
                summary = null,
                icon = painterResource(R.drawable.speed),
                value = speedThresholdBytes / 1024,
                valueLabel = speedLabel,
                valueRange = 0L..1024L,
                stepSize = 16,
                enabled = speedThresholdEnabled,
                onValueChanged = { newValue ->
                    scope.launch { appPreferenceRepo.setSpeedThresholdBytes(((newValue / 8f).roundToLong()) * 8 * 1024L) }
                }
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
    PageTitle(true, hazeState, stringResource(R.string.notifications))
}