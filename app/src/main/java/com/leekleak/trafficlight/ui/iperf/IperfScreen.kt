@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package com.leekleak.trafficlight.ui.iperf

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.leekleak.trafficlight.R
import com.leekleak.trafficlight.ui.components.BackAction
import com.leekleak.trafficlight.ui.components.HazeScaffold
import com.leekleak.trafficlight.ui.navigation.NAVBAR_PADDING
import com.leekleak.trafficlight.ui.navigation.Navigator
import com.leekleak.trafficlight.util.iconToggleButton

@Composable
fun IperfScreen(
    navigator: Navigator,
    viewModel: IperfScreenVM
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val myIp = uiState.myIp
    val showServer = uiState.showServer

    HazeScaffold(
        title = stringResource(R.string.iperf3) + " (Beta)",
        backAction = BackAction.None,
        scrollState = null,
        extraPadding = PaddingValues(bottom = NAVBAR_PADDING),
    ) { contentPadding ->
        val topPadding = contentPadding.calculateTopPadding()
        val sidePadding = contentPadding.calculateLeftPadding(LayoutDirection.Ltr)
        val bottomPadding = contentPadding.calculateBottomPadding()
        Column(
            modifier = Modifier.padding(top = topPadding, bottom = bottomPadding),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            ButtonGroup(
                modifier = Modifier
                    .padding(start = sidePadding, end = sidePadding)
                    .fillMaxWidth(),
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
    isIperfRunning: Boolean,
    isIperfStopping: Boolean = false,
    onStartTest: () -> Unit,
    onStopTest: () -> Unit,
) {
    val rotation = remember { Animatable(0f) }
    val size by animateDpAsState(if (!isIperfRunning) 128.dp else 96.dp)

    LaunchedEffect(isIperfRunning) {
        if (isIperfRunning) {
            rotation.animateTo(
                targetValue = rotation.value + 360f,
                animationSpec = infiniteRepeatable(
                    animation = tween(15000, easing = LinearEasing)
                )
            )
        }
    }

    Button(
        modifier = modifier
            .graphicsLayer { rotationZ = rotation.value }
            .size(size),
        onClick = {
            if (!isIperfRunning) {
                onStartTest()
            } else {
                onStopTest()
            }
        },
        enabled = !isIperfStopping,
        shape = Cookie12Sided.toShape(),
    ) {
        val modifier = Modifier.size(56.dp)
        Box(modifier = Modifier
            .fillMaxSize()
            .graphicsLayer { rotationZ = -rotation.value }) {
            AnimatedContent(
                targetState = isIperfRunning,
                modifier = Modifier.align(Alignment.Center),
                transitionSpec =  { fadeIn().togetherWith(fadeOut()) }
            ) {
                Icon(
                    painter = painterResource(if (it) R.drawable.stop else R.drawable.play_arrow),
                    contentDescription = null,
                    modifier = modifier
                )
            }
        }
    }
}
