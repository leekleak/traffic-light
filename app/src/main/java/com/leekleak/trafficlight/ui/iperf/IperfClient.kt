package com.leekleak.trafficlight.ui.iperf

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonGroup
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.leekleak.trafficlight.R
import com.leekleak.trafficlight.charts.SpeedGraph
import com.leekleak.trafficlight.database.IPerfEntry
import com.leekleak.trafficlight.ui.settings.FancyDialog
import com.leekleak.trafficlight.ui.theme.card
import com.leekleak.trafficlight.ui.theme.googleSans
import com.leekleak.trafficlight.util.CategoryTitleSmallText
import com.leekleak.trafficlight.util.DataSize
import com.leekleak.trafficlight.util.SearchField
import com.leekleak.trafficlight.util.animateAlignmentAsState
import com.leekleak.trafficlight.util.formattedParts
import com.leekleak.trafficlight.util.iconToggleButton
import org.koin.compose.viewmodel.koinViewModel
import java.util.UUID

enum class IperfProtocol {
    TCP, UDP // SCTP should also be here, but it doesn't work and no one uses it either way
}

@Composable
fun ClientScreen(
    myIp: String?,
    viewModel: IperfClientVM = koinViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    if (uiState.showEntrySelector) {
        EntrySelectorComponent(
            onDismissRequest = { viewModel.setShowEntrySelector(false) },
            entries = uiState.entries,
            selectEntry = viewModel::selectEntry,
            setShowEntryEdit = {
                viewModel.setEditEntry(it)
                viewModel.setShowEntryCreator(true)
            },
            setShowEntryDeletion = {
                viewModel.setEditEntry(it)
                viewModel.setShowEntryDeletion(true)
            },
            setShowEntryCreator = { viewModel.setShowEntryCreator(true) }
        )
    }

    if (uiState.showEntryDeletion) {
        uiState.editEntry?.let { entry ->
            EntryDeletionComponent(
                onDismissRequest = { viewModel.setShowEntryDeletion(false) },
                deleteEntry = {
                    viewModel.deleteEntry(entry)
                    viewModel.setShowEntryCreator(false)
                    viewModel.setEditEntry(null)
                }
            )
        }
    }

    if (uiState.showEntryCreator) {
        EntryCreatorComponent(
            onDismissRequest = {
                viewModel.setShowEntryCreator(false)
                viewModel.setEditEntry(null)
            },
            entry = uiState.editEntry,
            selectEntry = viewModel::selectEntry,
            myIp = myIp
        )
    }

    if (uiState.showTestSettings) {
        TestSettingsComponent(
            onDismissRequest = { viewModel.setShowTestSettings(false) },
            selectedProtocol = uiState.selectedProtocol,
            onProtocolSelected = viewModel::setSelectedProtocol
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
            onClick = { viewModel.setShowEntrySelector(true) },
            contentPadding = PaddingValues(start = 12.dp, top = 4.dp, end = 8.dp, bottom = 4.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (uiState.selectedEntry == null) {
                    Text(text = stringResource(R.string.no_server_selected))
                } else {
                    Column {
                        Text(uiState.selectedEntry!!.name, fontWeight = FontWeight.Bold)
                        Text(uiState.selectedEntry!!.ip + ":" + uiState.selectedEntry!!.port)
                    }
                }
                Icon(painterResource(R.drawable.arrow_drop_down), null)
            }
        }

        val data = remember { mutableStateListOf<Float>() }
        val iPerfRunning by viewModel.iPerf3Provider.running.collectAsStateWithLifecycle()

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
            arguments = uiState.arguments,
            addData = data::add,
            clearData = data::clear,
            iPerf3Provider = viewModel.iPerf3Provider
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

        AnimatedVisibility(
            visible = !iPerfRunning,
            modifier = Modifier.align(Alignment.BottomCenter),
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Button(onClick = { viewModel.setShowTestSettings(true) }) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(painterResource(R.drawable.settings), null)
                    Text(modifier = Modifier.padding(start = 8.dp), text = stringResource(R.string.settings))
                }
            }
        }
    }
}


@Composable
private fun EntrySelectorComponent(
    onDismissRequest: () -> Unit,
    entries: List<IPerfEntry>,
    selectEntry: (IPerfEntry) -> Unit,
    setShowEntryEdit: (IPerfEntry) -> Unit,
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
            var showDropdown by remember { mutableStateOf(false) }
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
                            showDropdown = true
                        }
                    )
                    .padding(vertical = 8.dp, horizontal = 12.dp)
            ) {
                Text(text = it.name)
                Text(text = it.ip + ":" + it.port)
                DropdownMenu(
                    expanded = showDropdown,
                    onDismissRequest = {showDropdown = false},
                    shape = MaterialTheme.shapes.medium,
                ) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.edit)) },
                        leadingIcon = { Icon(painterResource(R.drawable.edit), null) },
                        onClick = {
                            showDropdown = false
                            setShowEntryEdit(it)
                        }
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.delete)) },
                        leadingIcon = { Icon(painterResource(R.drawable.deleted), null) },
                        onClick = {
                            showDropdown = false
                            setShowEntryDeletion(it)
                        }
                    )
                }
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
    entry: IPerfEntry?,
    selectEntry: (IPerfEntry) -> Unit,
    myIp: String?,
) {
    val nameFieldState = rememberTextFieldState(entry?.name ?: "")
    var nameFieldError: String? by remember { mutableStateOf(null) }
    val ipFieldState = rememberTextFieldState(entry?.ip ?: "")
    var ipFieldError: String? by remember { mutableStateOf(null) }
    val portFieldState = rememberTextFieldState(entry?.port ?: "")
    var portFieldError: String? by remember { mutableStateOf(null) }

    val emptyNameError = stringResource(R.string.name_cannot_be_empty)
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
                val validName = name.isNotBlank()

                val ip = ipFieldState.text.toString().trim()

                val validPort = portFieldState.text.toString().split("-").all {
                     val port = it.toIntOrNull()
                     port != null && port in 0..65535
                }

                nameFieldError = if (!validName) { emptyNameError } else null
                portFieldError = if (!validPort) { invalidPortError } else null
                if (validName && validPort) {
                    selectEntry(
                        entry?.copy(
                            name = name,
                            ip = ip,
                            port = portFieldState.text.toString()
                        ) ?: IPerfEntry(
                            uuid = UUID.randomUUID().toString(),
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
            CategoryTitleSmallText(stringResource(R.string.server_address))
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

@Composable
private fun EntryDeletionComponent(
    onDismissRequest: () -> Unit,
    deleteEntry: () -> Unit
) {
    FancyDialog(
        onDismissRequest = onDismissRequest,
        title = stringResource(R.string.remove_server),
        icon = painterResource(R.drawable.deleted),
        actionButton = {
            Button(onClick = {
                deleteEntry()
                onDismissRequest()
            }) {
                Text(stringResource(R.string.remove))
            }
        }
    ) {
        Text(stringResource(R.string.remove_server_question))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TestSettingsComponent(
    onDismissRequest: () -> Unit,
    selectedProtocol: IperfProtocol,
    onProtocolSelected: (IperfProtocol) -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismissRequest
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 16.dp),
        ) {
            CategoryTitleSmallText(stringResource(R.string.protocol))
            ButtonGroup(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(
                    4.dp,
                    Alignment.CenterHorizontally
                ),
                overflowIndicator = {}
            ) {
                iconToggleButton(
                    selected = selectedProtocol == IperfProtocol.TCP,
                    onSelect = { onProtocolSelected(IperfProtocol.TCP) },
                    text = "TCP",
                    weight = 1f
                )
                iconToggleButton(
                    selected = selectedProtocol == IperfProtocol.UDP,
                    onSelect = { onProtocolSelected(IperfProtocol.UDP) },
                    text = "UDP",
                    weight = 1f
                )
            }
        }
    }
}
