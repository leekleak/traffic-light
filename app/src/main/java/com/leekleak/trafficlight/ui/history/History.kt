package com.leekleak.trafficlight.ui.history

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ButtonGroup
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SearchBar
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.SearchBarValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberSearchBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import coil3.compose.rememberAsyncImagePainter
import com.leekleak.trafficlight.R
import com.leekleak.trafficlight.charts.LineGraph
import com.leekleak.trafficlight.charts.ScrollableBarGraph
import com.leekleak.trafficlight.charts.model.ScrollableBarData
import com.leekleak.trafficlight.database.DataType
import com.leekleak.trafficlight.database.DataUID
import com.leekleak.trafficlight.database.Mobile
import com.leekleak.trafficlight.database.UsageQuery
import com.leekleak.trafficlight.database.Wifi
import com.leekleak.trafficlight.database.getIcon
import com.leekleak.trafficlight.database.getName
import com.leekleak.trafficlight.database.getNext
import com.leekleak.trafficlight.model.AppIcon
import com.leekleak.trafficlight.model.AppManager
import com.leekleak.trafficlight.ui.overview.AppSelector
import com.leekleak.trafficlight.ui.overview.specialApps
import com.leekleak.trafficlight.ui.theme.card
import com.leekleak.trafficlight.util.CategoryTitleText
import com.leekleak.trafficlight.util.getName
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject
import java.time.LocalDate
import java.time.format.TextStyle
import kotlin.math.max

const val MAX_DAYS = 90
val imageWidth = 32.dp

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun History(paddingValues: PaddingValues) {
    val viewModel: HistoryVM = koinViewModel()
    val haptic = LocalHapticFeedback.current

    var appDay by remember { mutableStateOf( LocalDate.now()) }
    var showMonth by remember { mutableStateOf(false) }

    LaunchedEffect(appDay, showMonth) {
        viewModel.updateDateQuery(appDay, showMonth)
    }

    val usageTypes = listOf(Mobile, Wifi)
    val usage: List<ScrollableBarData> by viewModel.usageFlow.collectAsState()
    val sidePadding = remember(paddingValues) { paddingValues.calculateLeftPadding(LayoutDirection.Ltr) }

    val usageQuery1 by viewModel.query1Flow.collectAsState()
    val usageQuery2 by viewModel.query2Flow.collectAsState()

    Column {
        Column (
            modifier = Modifier
                .padding(
                    start = sidePadding,
                    end = sidePadding,
                    top = paddingValues.calculateTopPadding()
                ),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Box(Modifier.fillMaxWidth()) {
                CategoryTitleText(stringResource(R.string.history))
                Row(
                    modifier = Modifier.align(Alignment.CenterEnd),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    HistoryLegendItem(usageQuery1, colorScheme.primary, colorScheme.onPrimary)
                    HistoryLegendItem(usageQuery2, colorScheme.tertiary, colorScheme.onTertiary)
                }
            }
            ScrollableBarGraph(usage) {
                appDay = viewModel.datesForTimespan.first.plusDays(it.toLong())
            }
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(MaterialTheme.shapes.small)
                    .background(colorScheme.primary)
                    .padding(horizontal = 4.dp)
            ) {
                var showFilter by remember { mutableStateOf(false) }
                if (showFilter) HistoryFilter { showFilter = false }
                ButtonGroup(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(
                        4.dp,
                        Alignment.CenterHorizontally
                    ),
                    expandedRatio = 0.05f,
                    overflowIndicator = {}
                ) {
                    customItem(
                        buttonGroupContent = {
                            val source = remember { MutableInteractionSource() }
                            val press by source.collectIsPressedAsState()
                            val cornerRadius by animateDpAsState(if (press) 24.dp else 6.dp)
                            IconButton(
                                modifier = Modifier.animateWidth(source),
                                colors = IconButtonDefaults.iconButtonColors(
                                    containerColor = colorScheme.surfaceContainer,
                                    contentColor = colorScheme.onSurfaceVariant
                                ),
                                shape = RoundedCornerShape(cornerRadius),
                                interactionSource = source,
                                onClick = {
                                    showFilter = true
                                    haptic.performHapticFeedback(HapticFeedbackType.ToggleOn)
                                }
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.filter_list),
                                    contentDescription = stringResource(R.string.filter)
                                )
                            }
                        },
                        menuContent = {}
                    )
                    toggleableItem(
                        onCheckedChange = {
                            showMonth = true
                            haptic.performHapticFeedback(HapticFeedbackType.ToggleOn)
                        },
                        label = appDay.month.getName(TextStyle.FULL),
                        checked = !showMonth,
                        weight = 3f
                    )
                    toggleableItem(
                        onCheckedChange = {
                            showMonth = false
                            haptic.performHapticFeedback(HapticFeedbackType.ToggleOn)
                        },
                        label = appDay.dayOfMonth.toString(),
                        checked = showMonth,
                        weight = 1f
                    )
                }
            }
        }
        AppList(sidePadding, paddingValues, usageTypes)
    }
}

@Composable
private fun AppList(
    sidePadding: Dp,
    paddingValues: PaddingValues,
    usageTypes: List<DataType>,
) {
    val viewModel: HistoryVM = koinViewModel()

    val appList by remember { viewModel.appList }.collectAsState()
    val appTotal = remember(appList) { viewModel.appUsageSum(appList) }
    var appSelected by remember { mutableIntStateOf(-1) }
    val selectedUsage by remember { viewModel.totalUsage }.collectAsState(null)
    val totalMaximum = remember(selectedUsage) { selectedUsage?.usages?.values?.sum() }

    val listState = rememberLazyListState()
    LazyColumn(
        modifier = Modifier
            .padding(top = 8.dp)
            .clip(MaterialTheme.shapes.large)
            .background(colorScheme.surfaceContainer)
            .fillMaxSize(),
        contentPadding = PaddingValues(sidePadding, sidePadding, sidePadding, paddingValues.calculateBottomPadding()),
        verticalArrangement = Arrangement.spacedBy(6.dp),
        state = listState
    ) {
        if (totalMaximum != null) {
            item {
                val uid = -100
                AppItem(
                    usage1 = selectedUsage?.usages?.get(usageTypes[1]) ?: 0L,
                    usage2 = selectedUsage?.usages?.get(usageTypes[0]) ?: 0L,
                    painter = painterResource(R.drawable.data_usage),
                    icon = true,
                    name = stringResource(R.string.total_usage),
                    selected = uid == appSelected,
                    maximum = max(totalMaximum, 1)
                ) {
                    appSelected = if (appSelected != uid) uid else -1
                }
            }
            items(appList, { it.uid }) { item ->
                Box(Modifier.animateItem()) {
                    var icon = false
                    val painter = item.drawableResource?.let { icon = true; painterResource(it) }
                        ?: rememberAsyncImagePainter(AppIcon(item.packageName))
                    AppItem(
                        usage1 = item.usage.usages[usageTypes[1]] ?: 0L,
                        usage2 = item.usage.usages[usageTypes[0]] ?: 0L,
                        painter = painter,
                        name = item.name,
                        icon = icon,
                        selected = item.uid == appSelected,
                        maximum = totalMaximum
                    ) {
                        appSelected = if (appSelected != item.uid) item.uid else -1
                    }
                }
            }
            selectedUsage?.let {
                if ((appTotal.usages[Mobile] != it.usages[Mobile] || appTotal.usages[Wifi] != it.usages[Wifi]) && appList.isNotEmpty()) {
                    item {
                        val uid = -99

                        Box(Modifier.animateItem()) {
                            AppItem(
                                usage1 = (it.usages[usageTypes[1]] ?: 0L) - (appTotal.usages[usageTypes[1]] ?: 0L),
                                usage2 = (it.usages[usageTypes[0]] ?: 0L) - (appTotal.usages[usageTypes[0]] ?: 0L),
                                painter = painterResource(R.drawable.help),
                                icon = true,
                                name = stringResource(R.string.unknown),
                                selected = uid == appSelected,
                                maximum = totalMaximum
                            ) {
                                appSelected = if (appSelected != uid) uid else -1
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HistoryLegendItem(
    usageQuery1: UsageQuery,
    backgroundColor: Color,
    foregroundColor: Color,
) {
    val context = LocalContext.current
    Row(
        modifier = Modifier
            .background(backgroundColor, MaterialTheme.shapes.medium)
            .padding(vertical = 4.dp, horizontal = 8.dp)
    ) {
        Icon(
            painter = painterResource(usageQuery1.dataType.getIcon()),
            contentDescription = stringResource(usageQuery1.dataType.getName()),
            tint = foregroundColor
        )
        AnimatedVisibility(usageQuery1.dataType.isNotEmpty()) {
            Row {
                Icon(
                    painter = painterResource(usageQuery1.dataDirection.getIcon()),
                    contentDescription = stringResource(usageQuery1.dataDirection.getName()),
                    tint = foregroundColor
                )
                usageQuery1.dataUID.getIcon(foregroundColor)
            }
        }
    }
}

@Composable
fun HistoryFilter(
    onDismiss: () -> Unit
) {
    val viewModel: HistoryVM = koinViewModel()

    val usageQuery1 by viewModel.query1Flow.collectAsState()
    val usageQuery2 by viewModel.query2Flow.collectAsState()
    Dialog(
        onDismissRequest = onDismiss
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .card()
                .padding(16.dp)
        ) {
            Text(
                text = stringResource(R.string.filter),
                style = MaterialTheme.typography.headlineMedium
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                HistoryItemSettings(
                    stringResource(R.string.primary),
                    1,
                    usageQuery1
                )
                HistoryItemSettings(
                    stringResource(R.string.secondary),
                    2,
                    usageQuery2
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RowScope.HistoryItemSettings(
    title: String,
    n: Int,
    query: UsageQuery
) {
    val hapticFeedback = LocalHapticFeedback.current
    val viewModel: HistoryVM = koinViewModel()
    val context = LocalContext.current

    Column (modifier = Modifier.weight(1f)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium
        )
        FilterButton(
            n = n,
            enabled = true,
            onClick = {
                viewModel.updateQuery(n, query.copy(dataType = query.dataType.getNext()))
                hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
            }
        ) {
            Icon(painterResource(query.dataType.getIcon()), null)
            Text(stringResource(query.dataType.getName()))
        }
        FilterButton(
            n = n,
            enabled = query.dataType.isNotEmpty(),
            onClick = {
                viewModel.updateQuery(n, query.copy(dataDirection = query.dataDirection.getNext()))
                hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
            }
        ) {
            Icon(painterResource(query.dataDirection.getIcon()), null)
            Text(stringResource(query.dataDirection.getName()))
        }

        var showAppPicker by remember { mutableStateOf(false) }
        FilterButton(
            n = n,
            enabled = query.dataType.isNotEmpty(),
            onClick = {
                showAppPicker = true
                hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
            }
        ) {
            query.dataUID.getIcon()
            Text(query.dataUID.getName(context) ?: "")
        }

        if (showAppPicker) {
            AppSearchDialog (
                onSelect = { uid ->
                    viewModel.updateQuery(n, query.copy(dataUID = DataUID(uid)))
                    hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                }
            ){
                showAppPicker = false
            }
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
private fun AppSearchDialog(onSelect: (uid: Int) -> Unit, onDismiss: () -> Unit) {
    val sheetState = rememberModalBottomSheetState()
    ModalBottomSheet (
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        val appManager: AppManager = koinInject()
        val searchBarState = rememberSearchBarState(SearchBarValue.Expanded)
        val focusRequester = remember { FocusRequester() }
        val keyboardState by rememberUpdatedState(WindowInsets.isImeVisible)
        var searchFocused by remember { mutableStateOf(false) }
        val scope = rememberCoroutineScope()

        LaunchedEffect(Unit) {
            focusRequester.requestFocus()
        }

        LaunchedEffect(keyboardState) {
            if (!keyboardState && searchFocused) {
                scope.launch { sheetState.hide() }.invokeOnCompletion {
                    if (!sheetState.isVisible) onDismiss()
                }
            }
            else if (keyboardState) {
                searchFocused = true
            }
        }

        val includedApps by remember { appManager.suspiciousApps }.collectAsState(emptyList())
        var query by remember { mutableStateOf("") }
        val searchResults by remember {
            derivedStateOf {
                if (query.isEmpty()) includedApps.sortedByDescending { specialApps.indexOf(it.packageName) }
                else includedApps.filter { it.label.lowercase().contains(query.lowercase()) }
            }
        }
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            AppSelector(searchResults) { uid -> onSelect(uid) }
            SearchBar(
                modifier = Modifier
                    .focusRequester(focusRequester),
                state = searchBarState,
                inputField = {
                    SearchBarDefaults.InputField(
                        query = query,
                        onQueryChange = { query = it },
                        onSearch = {},
                        expanded = false,
                        onExpandedChange = {},
                        leadingIcon = {
                            Icon(
                                painter = painterResource(R.drawable.search),
                                contentDescription = null
                            )
                        }
                    )
                }
            )
        }
    }
}

@Composable
private fun FilterButton(
    n: Int,
    enabled: Boolean,
    onClick: () -> Unit,
    buttonContent: @Composable (() -> Unit)
) {
    Button(
        modifier = Modifier.fillMaxWidth(),
        enabled = enabled,
        shape = MaterialTheme.shapes.small,
        colors = ButtonDefaults.buttonColors(
            containerColor = if (n == 1) colorScheme.primary else colorScheme.tertiary,
            contentColor = if (n == 1) colorScheme.onPrimary else colorScheme.onTertiary
        ),
        contentPadding = PaddingValues(),
        onClick = onClick
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            buttonContent()
        }
    }
}

@Composable
fun AppItem(
    modifier: Modifier = Modifier,
    usage1: Long,
    usage2: Long,
    painter: Painter,
    icon: Boolean = false,
    name: String,
    selected: Boolean,
    maximum: Long,
    onClick: () -> Unit = {},
) {
    val haptic = LocalHapticFeedback.current
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        Column(
            modifier = modifier
                .clip(MaterialTheme.shapes.small)
                .background(colorScheme.surface)
                .clickable {
                    onClick()
                    haptic.performHapticFeedback(HapticFeedbackType.ContextClick)
                }
        ) {
            Row(
                modifier = Modifier.padding(4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (icon) {
                    Icon(
                        modifier = Modifier.size(imageWidth),
                        painter = painter,
                        contentDescription = name
                    )
                } else {
                    Image(
                        modifier = Modifier.size(imageWidth),
                        painter = painter,
                        contentDescription = name
                    )
                }
                AnimatedContent(selected) { selected ->
                    if (!selected) {
                        LineGraph(
                            maximum = maximum,
                            data = Pair(usage1, usage2)
                        )
                    } else {
                        Text(
                            text = name,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
            AnimatedVisibility(
                visible = selected,
                enter = expandVertically(spring(0.7f, Spring.StiffnessMedium)),
                exit = shrinkVertically(spring(0.7f, Spring.StiffnessMedium))
            ) {
                LineGraphHeader {
                    LineGraph(
                        maximum = maximum,
                        data = Pair(usage1, usage2)
                    )
                }
            }
        }
    }
}

@Composable
fun LineGraphHeader(lineGraph: @Composable (() -> Unit)) {
    val context = LocalContext.current
    val viewModel: HistoryVM = koinViewModel()

    val usageQuery1 by viewModel.query1Flow.collectAsState()
    val usageQuery2 by viewModel.query2Flow.collectAsState()

    val offset = imageWidth + 12.dp
    Column (
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 4.dp, end = offset + 4.dp)
            .offset(offset, 0.dp),
    ) {
        Row {
            Text(
                modifier = Modifier.weight(1f),
                text = usageQuery1.toString(context),
                style = MaterialTheme.typography.titleMedium,
                color = colorScheme.tertiary
            )
            Text(
                modifier = Modifier.weight(1f),
                text = usageQuery2.toString(context),
                textAlign = TextAlign.End,
                style = MaterialTheme.typography.titleMedium,
                color = colorScheme.tertiary
            )
        }
        lineGraph()
    }
}
