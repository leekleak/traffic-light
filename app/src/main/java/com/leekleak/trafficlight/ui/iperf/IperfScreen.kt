@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package com.leekleak.trafficlight.ui.iperf

import android.widget.Toast
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.leekleak.iperfintegration.IPerf3Provider
import com.leekleak.iperfintegration.IntervalEvent
import com.leekleak.iperfintegration.IperfCallback
import com.leekleak.iperfintegration.IperfEvent
import com.leekleak.trafficlight.R
import com.leekleak.trafficlight.ui.components.BackAction
import com.leekleak.trafficlight.ui.components.HazeScaffold
import com.leekleak.trafficlight.ui.navigation.NAVBAR_PADDING
import com.leekleak.trafficlight.util.iconToggleButton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun IperfScreen(
    viewModel: IperfScreenVM
) {
    val myIp by viewModel.ipFlow.collectAsStateWithLifecycle()
    val iPerf3Provider = viewModel.iPerf3Provider

    HazeScaffold(
        title = stringResource(R.string.iperf3) + " (Beta)",
        backAction = BackAction.None,
        scrollState = null,
        extraPadding = PaddingValues(bottom = NAVBAR_PADDING),
    ) { contentPadding ->
        var showServer by remember { mutableStateOf(false) }

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
                    onSelect = { showServer = false }
                ) {
                    Icon(painterResource(R.drawable.arrow_downward_alt), null)
                    Text(stringResource(R.string.client))
                }
                iconToggleButton(
                    selected = showServer,
                    weight = 1f,
                    onSelect = { showServer = true }
                ) {
                    Icon(painterResource(R.drawable.arrow_upward_alt), null)
                    Text(stringResource(R.string.server))
                }
            }

            AnimatedContent(showServer) {
                if (!it) {
                    ClientScreen(
                        myIp = myIp
                    )
                } else {
                    ServerScreen(
                        myIp = myIp,
                        iPerf3Provider = iPerf3Provider
                    )
                }
            }
        }
    }
}

@Composable
fun PlayButton(
    modifier: Modifier,
    arguments: Array<String>?,
    addData: (Float) -> Unit, // bytes/sec
    clearData: () -> Unit,
    iPerf3Provider: IPerf3Provider,
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val iPerfRunning by iPerf3Provider.running.collectAsStateWithLifecycle()
    val iPerfStopping by iPerf3Provider.stopping.collectAsStateWithLifecycle()

    val rotation = remember { Animatable(0f) }
    val size by animateDpAsState(if (!iPerfRunning) 128.dp else 96.dp)

    LaunchedEffect(iPerfRunning) {
        if (iPerfRunning) {
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
            if (!iPerfRunning) {
                clearData()
                scope.launch { // Intentionally scope to ui instance so the test gets canceled automatically and doesn't leak
                    if (arguments == null) return@launch
                    iPerf3Provider.runTest(
                        arguments,
                        object : IperfCallback {
                            override fun onOutput(event: IperfEvent) {
                                if (event is IntervalEvent) {
                                    event.results.forEach { result ->
                                        addData(result.bitsPerSecond.toFloat()/8f)
                                    }
                                }
                            }

                            override fun onError(error: String) {
                                scope.launch {
                                    withContext(Dispatchers.Main) {
                                        Toast.makeText(context, error, Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }

                            override fun onComplete() {}
                        }
                    )
                }
            } else {
                iPerf3Provider.stopTest()
            }
        },
        enabled = !iPerfStopping,
        shape = Cookie12Sided.toShape(),
    ) {
        val modifier = Modifier.size(56.dp)
        Box(modifier = Modifier
            .fillMaxSize()
            .graphicsLayer { rotationZ = -rotation.value }) {
            AnimatedContent(
                targetState = iPerfRunning,
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

@Composable
private fun ServerScreen(
    myIp: String?,
    iPerf3Provider: IPerf3Provider
) {
    Box(
        modifier = Modifier.fillMaxSize(),
    ) {
        if (myIp != null) {
            Button(
                modifier = Modifier
                    .padding(top = 4.dp)
                    .align(Alignment.TopCenter),
                shape = MaterialTheme.shapes.medium,
                onClick = {},
                contentPadding = PaddingValues(start = 12.dp, top = 4.dp, end = 12.dp, bottom = 4.dp)
            ) {
                Column {
                    Text(stringResource(R.string.server_address), fontWeight = FontWeight.Bold)
                    Text("$myIp:5201")
                }
            }
        }

        PlayButton(
            modifier = Modifier.padding(32.dp).align(Alignment.Center),
            arguments = arrayOf("-s", "-p", "5201"),
            addData = {},
            clearData = {},
            iPerf3Provider = iPerf3Provider
        )
    }
}
