@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package com.leekleak.trafficlight.ui.iperf

import android.widget.Toast
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.input.rememberTextFieldState
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
import androidx.compose.runtime.mutableStateListOf
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
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.leekleak.iperfintegration.IPerf3Provider
import com.leekleak.iperfintegration.IntervalEvent
import com.leekleak.iperfintegration.IperfCallback
import com.leekleak.iperfintegration.IperfEvent
import com.leekleak.trafficlight.R
import com.leekleak.trafficlight.charts.SpeedGraph
import com.leekleak.trafficlight.database.IPerfEntry
import com.leekleak.trafficlight.ui.components.BackAction
import com.leekleak.trafficlight.ui.components.HazeScaffold
import com.leekleak.trafficlight.ui.navigation.NAVBAR_PADDING
import com.leekleak.trafficlight.ui.settings.FancyDialog
import com.leekleak.trafficlight.ui.theme.card
import com.leekleak.trafficlight.ui.theme.googleSans
import com.leekleak.trafficlight.util.CategoryTitleSmallText
import com.leekleak.trafficlight.util.DataSize
import com.leekleak.trafficlight.util.SearchField
import com.leekleak.trafficlight.util.animateAlignmentAsState
import com.leekleak.trafficlight.util.formattedParts
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

        val entries by viewModel.iperfEntries.collectAsStateWithLifecycle()
        val selectedEntry = entries.firstOrNull { it.selected }

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
                    fillWidth = true,
                    onSelect = { showServer = false }
                ) {
                    Icon(painterResource(R.drawable.arrow_downward_alt), null)
                    Text(stringResource(R.string.client))
                }
                iconToggleButton(
                    selected = showServer,
                    fillWidth = true,
                    onSelect = { showServer = true }
                ) {
                    Icon(painterResource(R.drawable.arrow_upward_alt), null)
                    Text(stringResource(R.string.server))
                }
            }

            AnimatedContent(showServer) {
                if (!it) {
                    ClientScreen(
                        selectedEntry = selectedEntry,
                        entries = entries,
                        selectEntry = viewModel::selectEntry,
                        deleteEntry = viewModel::deleteEntry,
                        myIp = myIp,
                        iPerf3Provider = iPerf3Provider
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
private fun ClientScreen(
    selectedEntry: IPerfEntry?,
    entries: List<IPerfEntry>,
    selectEntry: (IPerfEntry) -> Unit,
    deleteEntry: (IPerfEntry) -> Unit,
    iPerf3Provider: IPerf3Provider,
    myIp: String?,
) {
    var showEntrySelector by remember { mutableStateOf(false) }
    var showEntryCreator by remember { mutableStateOf(false) }
    var showEntryDeletion: IPerfEntry? by remember { mutableStateOf(null) }

    if (showEntrySelector) {
        EntrySelectorComponent(
            onDismissRequest = { showEntrySelector = false },
            entries = entries,
            selectEntry = selectEntry,
            setShowEntryDeletion = { showEntryDeletion = it },
            setShowEntryCreator = { showEntryCreator = true }
        )
    }

    showEntryDeletion?.let {
        EntryDeletionComponent(
            onDismissRequest = { showEntryDeletion = null },
            entry = it,
            deleteEntry = deleteEntry
        )
    }

    if (showEntryCreator) {
        EntryCreatorComponent(
            onDismissRequest = {
                showEntryCreator = false
            },
            entries = entries,
            selectEntry = selectEntry,
            myIp = myIp
        )
    }

    Box(
        modifier = Modifier.fillMaxSize(),
    ) {
        Button(
            modifier = Modifier
                .padding(top = 4.dp)
                .align(Alignment.TopCenter),
            shape = MaterialTheme.shapes.medium,
            onClick = { showEntrySelector = true },
            contentPadding = PaddingValues(start = 12.dp, top = 4.dp, end = 8.dp, bottom = 4.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (selectedEntry == null) {
                    Text(text = stringResource(R.string.no_server_selected))
                } else {
                    Column {
                        Text(selectedEntry.name, fontWeight = FontWeight.Bold)
                        Text(selectedEntry.ip + ":" + selectedEntry.port)
                    }
                }
                Icon(painterResource(R.drawable.arrow_drop_down), null)
            }
        }

        val data = remember { mutableStateListOf<Float>() }
        val iPerfRunning by iPerf3Provider.running.collectAsStateWithLifecycle()

        SpeedGraph(
            modifier = Modifier
                .fillMaxHeight(0.5f)
                .align(Alignment.BottomCenter),
            data = data,
            running = iPerfRunning
        )

        val alignment by animateAlignmentAsState(if (iPerfRunning) Alignment.BottomCenter else Alignment.Center)
        PlayButton(
            modifier = Modifier
                .align(alignment)
                .padding(32.dp),
            arguments = selectedEntry?.let { arrayOf("-c", it.ip, "-p", it.port, "-i", "0.5") },
            addData = data::add,
            clearData = data::clear,
            iPerf3Provider = iPerf3Provider
        )

        AnimatedVisibility(
            visible = iPerfRunning,
            modifier = Modifier.align(Alignment.Center),
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            val currentItem = data.lastOrNull() ?: return@AnimatedVisibility
            val text = DataSize(byteValue = currentItem.toLong()).formattedParts(extraPrecision = true, speed = true)

            val interactionSource = remember { MutableInteractionSource() }
            val pressed by interactionSource.collectIsPressedAsState()
            val width by animateFloatAsState(
                targetValue = if (pressed) 55f else 50f,
                animationSpec = spring()
            )
            val weight by animateFloatAsState(if (pressed) 800f else 500f, spring())
            val fontFamily1 = remember(weight, width) { googleSans(weight = weight, width = width, roundness = 60f) }

            Text(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 256.dp)
                    .clickable(
                        interactionSource = interactionSource,
                        onClick = {},
                        indication = null
                    ),
                textAlign = TextAlign.Center,
                text = buildAnnotatedString {
                    withStyle(style = SpanStyle(fontFamily = fontFamily1, fontSize = 88.sp)) {
                        append("${text.first}${text.second}")
                    }
                    withStyle(style = SpanStyle(fontFamily = fontFamily1, fontSize = 42.sp)) {
                        appendLine(text.third)
                    }
                }
            )
        }
    }
}

@Composable
private fun PlayButton(
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

@Composable
private fun EntryDeletionComponent(
    onDismissRequest: () -> Unit,
    entry: IPerfEntry,
    deleteEntry: (IPerfEntry) -> Unit
) {
    FancyDialog(
        onDismissRequest = onDismissRequest,
        title = stringResource(R.string.remove_server),
        icon = painterResource(R.drawable.deleted),
        actionButton = {
            Button(onClick = {
                deleteEntry(entry)
                onDismissRequest()
            }) {
                Text(stringResource(R.string.remove))
            }
        }
    ) {
        Text(stringResource(R.string.remove_server_question))
    }
}

@Composable
private fun EntrySelectorComponent(
    onDismissRequest: () -> Unit,
    entries: List<IPerfEntry>,
    selectEntry: (IPerfEntry) -> Unit,
    setShowEntryDeletion: (IPerfEntry) -> Unit,
    setShowEntryCreator: () -> Unit
) {
    FancyDialog(
        onDismissRequest = onDismissRequest,
        title = stringResource(R.string.select_server),
        icon = painterResource(R.drawable.language),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Spacer(Modifier)
        entries.forEach {
            Column(
                Modifier
                    .fillMaxWidth()
                    .card(MaterialTheme.colorScheme.surfaceContainerHigh)
                    .combinedClickable(
                        onClick = {
                            selectEntry(it)
                            onDismissRequest()
                        },
                        onLongClick = {
                            setShowEntryDeletion(it)
                        }
                    )
                    .padding(vertical = 8.dp, horizontal = 12.dp)
            ) {
                Text(text = it.name)
                Text(text = it.ip + ":" + it.port)
            }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .card(MaterialTheme.colorScheme.surfaceContainerHigh)
                .clickable(onClick = { setShowEntryCreator() })
                .padding(vertical = 8.dp, horizontal = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(painterResource(R.drawable.add), null)
            Text(stringResource(R.string.add_server))
        }
    }
}

@Composable
private fun EntryCreatorComponent(
    onDismissRequest: () -> Unit,
    entries: List<IPerfEntry>,
    selectEntry: (IPerfEntry) -> Unit,
    myIp: String?
) {
    val nameFieldState = rememberTextFieldState()
    var nameFieldError: String? by remember { mutableStateOf(null) }
    val ipFieldState = rememberTextFieldState()
    var ipFieldError: String? by remember { mutableStateOf(null) }
    val portFieldState = rememberTextFieldState()
    var portFieldError: String? by remember { mutableStateOf(null) }

    val emptyNameError = stringResource(R.string.name_cannot_be_empty)
    val usedNameError = stringResource(R.string.name_already_used)
    //val invalidIpError = stringResource(R.string.invalid_ip_address) Not worth checking as ip address could also be a domain name
    val invalidPortError = stringResource(R.string.invalid_port)
    FancyDialog(
        onDismissRequest = onDismissRequest,
        title = stringResource(R.string.add_server),
        icon = painterResource(R.drawable.add),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        actionButton = {
            Button(onClick = {
                val name = nameFieldState.text.toString().trim()
                val validName = name.isNotBlank() && entries.find { it.name == name } == null

                val ip = ipFieldState.text.toString().trim()

                val port = portFieldState.text.toString().toIntOrNull()
                val validPort = port != null && port in 0..65535

                nameFieldError = if (!validName) {
                    if (name.isBlank()) {
                        emptyNameError
                    } else {
                        usedNameError
                    }
                } else null
                portFieldError = if (!validPort) { invalidPortError } else null
                if (validName && validPort) {
                    selectEntry(
                        IPerfEntry(
                            name = name,
                            ip = ip,
                            port = portFieldState.text.toString(),
                            selected = true
                        )
                    )
                    onDismissRequest()
                }
            }) {
                Text(stringResource(R.string.save))
            }
        }
    ) {
        Column {
            CategoryTitleSmallText(stringResource(R.string.server_name))
            SearchField(
                textFieldState = nameFieldState,
                placeholder = stringResource(R.string.my_server),
                isError = nameFieldError
            )
        }
        Column {
            CategoryTitleSmallText(stringResource(R.string.server_ip_address))
            val placeholderIP = myIp?.replaceAfterLast(".", "xxx")
            SearchField(
                textFieldState = ipFieldState,
                placeholder = placeholderIP ?: "192.168.xxx.xxx",
                isError = ipFieldError
            )
        }
        Column {
            CategoryTitleSmallText(stringResource(R.string.network_port))
            SearchField(
                textFieldState = portFieldState,
                placeholder = "5201",
                isError = portFieldError
            )
        }
    }
}