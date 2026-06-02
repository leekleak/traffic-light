package com.leekleak.trafficlight.ui.settings

import android.os.Build
import androidx.activity.compose.LocalActivity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.widthIn
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
import com.leekleak.trafficlight.util.DataSize
import com.leekleak.trafficlight.util.PageTitle
import com.leekleak.trafficlight.util.categoryTitleSmall
import com.leekleak.trafficlight.util.openLink
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.rememberHazeState
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject
import kotlin.math.pow

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

        categoryTitleSmall { stringResource(R.string.notification_channels) }
        item {
            val speedThreshold by appPreferenceRepo.speedThreshold.collectAsState(false)

            SwitchPreference (
                title = stringResource(R.string.speed_threshold),
                summary = stringResource(R.string.speed_threshold_description),
                icon = painterResource(R.drawable.visibility_off),
                value = speedThreshold,
                onValueChanged = {
                    scope.launch { appPreferenceRepo.setSpeedThreshold(it) }
                }
            )

            val speedThresholdBytes by appPreferenceRepo.speedThresholdBytes.collectAsState(-1024)
            val speedBits by appPreferenceRepo.speedBits.collectAsState(false)
            val disconnectedString = stringResource(R.string.disconnected)
            val values = remember(speedBits, disconnectedString) {
                (listOf(-1L) + (0..10).map { 2.0.pow(it).toLong() }).map {
                    it to if (it < 0) disconnectedString else DataSize(it * 1024).toString(speed = true, inBits = speedBits)
                }
            }

            AnimatedVisibility(
                visible = speedThreshold,
                enter = fadeIn(tween()) + slideInVertically() + expandVertically(),
                exit = fadeOut(tween()) + slideOutVertically() + shrinkVertically()
            ) {
                Column {
                    SliderPreference(
                        modifierLabelText = Modifier.widthIn(min = 128.dp),
                        title = stringResource(R.string.threshold),
                        icon = painterResource(R.drawable.horizontal_align_right),
                        value = speedThresholdBytes / 1024,
                        values = values,
                        onValueChanged = {
                            scope.launch { appPreferenceRepo.setSpeedThresholdBytes(it * 1024L) }
                        }
                    )
                    NavigatePreference(
                        title = stringResource(R.string.help),
                        icon = painterResource(R.drawable.help),
                        onClick = {
                            openLink(
                                activity,
                                "https://github.com/leekleak/traffic-light/wiki/Hide-status-bar-icon-when-disconnected"
                            )
                        }
                    )
                }
            }
        }
    }
    PageTitle (true, hazeState, stringResource(R.string.notifications))
}