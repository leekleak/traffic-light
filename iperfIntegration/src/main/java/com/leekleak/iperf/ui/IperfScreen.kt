@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package com.leekleak.iperf.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ButtonGroup
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialShapes.Companion.Cookie12Sided
import androidx.compose.material3.Text
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.leekleak.core.R
import com.leekleak.core.ui.components.BackAction
import com.leekleak.core.ui.components.HazeScaffold
import com.leekleak.core.ui.components.iconToggleButton
import com.leekleak.core.ui.navigation.Navigator
import com.leekleak.iperf.ui.client.ClientScreen
import com.leekleak.iperf.ui.server.ServerScreen

@Composable
fun IperfScreen(
    navigator: Navigator,
    viewModel: IperfScreenVM
) {
    val uiState by viewModel.uiState
    val myIp = uiState.myIp
    val showServer = uiState.showServer

    HazeScaffold(
        title = stringResource(R.string.iperf3),
        backAction = BackAction.Normal(navigator),
        scrollState = null,
    ) { contentPadding ->
        val topPadding = contentPadding.calculateTopPadding()
        val layoutDirection = LocalLayoutDirection.current
        val startPadding = contentPadding.calculateStartPadding(layoutDirection)
        val endPadding = contentPadding.calculateEndPadding(layoutDirection)
        val bottomPadding = contentPadding.calculateBottomPadding()
        Column(
            modifier = Modifier.padding(top = topPadding, bottom = bottomPadding),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            ButtonGroup(
                modifier = Modifier
                    .padding(start = startPadding, end = endPadding)
                    .widthIn(max = 500.dp)
                    .align(Alignment.CenterHorizontally),
                horizontalArrangement = Arrangement.spacedBy(
                    4.dp,
                    Alignment.CenterHorizontally
                ),
                expandedRatio = 0.05f,
                overflowIndicator = {}
            ) {
                iconToggleButton(
                    selected = !showServer,
                    weight = 1f,
                    onSelect = { viewModel.setShowServer(false) }
                ) {
                    Icon(painterResource(R.drawable.arrow_downward_alt), null)
                    Text(stringResource(R.string.client))
                }
                iconToggleButton(
                    selected = showServer,
                    weight = 1f,
                    onSelect = { viewModel.setShowServer(true) }
                ) {
                    Icon(painterResource(R.drawable.arrow_upward_alt), null)
                    Text(stringResource(R.string.server))
                }
            }

            AnimatedContent(showServer) { isServer ->
                if (!isServer) {
                    ClientScreen(
                        navigator = navigator,
                        myIp = myIp
                    )
                } else {
                    ServerScreen(
                        navigator = navigator,
                        myIp = myIp
                    )
                }
            }
        }
    }
}

@Composable
fun PlayButton(
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isIperfRunning: Boolean,
    isIperfStopping: Boolean = false,
    onStartTest: () -> Unit,
    onStopTest: () -> Unit,
) {
    val haptic = LocalHapticFeedback.current
    val context = LocalContext.current

    val spin = remember { Animatable(0f) }
    val scale = animateFloatAsState(if (isIperfRunning) 0.75f else 1f)
    val colors = ButtonDefaults.buttonColors()
    val isEnabled = (isIperfRunning || enabled) && !isIperfStopping

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) onStartTest()
    }

    LaunchedEffect(isIperfRunning) {
        if (!isIperfRunning) return@LaunchedEffect
        while (true) spin.animateTo(spin.value + 360f, tween(15000, easing = LinearEasing))
    }

    Box(modifier = modifier.size(128.dp), contentAlignment = Alignment.Center) {
        Button(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    rotationZ = spin.value
                    scaleX = scale.value
                    scaleY = scale.value
                },
            onClick = {
                if (!isIperfRunning) {
                    val hasPermission = Build.VERSION.SDK_INT < Build.VERSION_CODES.CINNAMON_BUN ||
                        ContextCompat.checkSelfPermission(
                            context,
                            Manifest.permission.ACCESS_LOCAL_NETWORK
                        ) == PackageManager.PERMISSION_GRANTED
                    if (hasPermission) {
                        onStartTest()
                        haptic.performHapticFeedback(HapticFeedbackType.ToggleOn)
                    } else {
                        permissionLauncher.launch(Manifest.permission.ACCESS_LOCAL_NETWORK)
                    }
                } else {
                    onStopTest()
                    haptic.performHapticFeedback(HapticFeedbackType.ToggleOff)
                }
            },
            enabled = isEnabled,
            shape = Cookie12Sided.toShape(),
            colors = colors,
        ) {}

        AnimatedContent(
            targetState = isIperfRunning,
        ) {
            Icon(
                painter = painterResource(if (it) R.drawable.stop else R.drawable.play_arrow),
                contentDescription = null,
                modifier = Modifier.size(56.dp),
                tint = if (isEnabled) colors.contentColor else colors.disabledContentColor
            )
        }
    }
}
