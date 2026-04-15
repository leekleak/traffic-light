package com.leekleak.trafficlight.ui.history

import androidx.activity.compose.LocalActivity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.ExperimentalFoundationApi
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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ButtonGroup
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.leekleak.trafficlight.R
import com.leekleak.trafficlight.charts.LineGraph
import com.leekleak.trafficlight.charts.ScrollableBarGraph
import com.leekleak.trafficlight.charts.model.ScrollableBarData
import com.leekleak.trafficlight.database.UsageQuery
import com.leekleak.trafficlight.database.getIcon
import com.leekleak.trafficlight.database.getName
import com.leekleak.trafficlight.database.getNext
import com.leekleak.trafficlight.model.AppManager
import com.leekleak.trafficlight.model.AppManager.Companion.allApp
import com.leekleak.trafficlight.model.AppManager.Companion.removedApp
import com.leekleak.trafficlight.model.AppManager.Companion.tetheringApp
import com.leekleak.trafficlight.model.AppManager.Companion.unknownApp
import com.leekleak.trafficlight.model.DataUID
import com.leekleak.trafficlight.model.DataUIDApp
import com.leekleak.trafficlight.ui.overview.AppSelector
import com.leekleak.trafficlight.ui.theme.card
import com.leekleak.trafficlight.ui.theme.momoTrustDisplayFont
import com.leekleak.trafficlight.util.CategoryTitleText
import com.leekleak.trafficlight.util.getName
import com.leekleak.trafficlight.util.toDp
import com.leekleak.trafficlight.util.toLocaleHourString
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject
import java.time.LocalTime

const val MAX_DAYS = 90
val imageWidth = 32.dp

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun History(paddingValues: PaddingValues) {
    val viewModel: HistoryVM = koinViewModel()
    val haptic = LocalHapticFeedback.current

    val usage: List<ScrollableBarData> by viewModel.usageFlow.collectAsState()
    val sidePadding = remember(paddingValues) { paddingValues.calculateLeftPadding(LayoutDirection.Ltr) }

    val usageQuery1 by viewModel.query1Flow.collectAsState()
    val usageQuery2 by viewModel.query2Flow.collectAsState()
    val listParam by viewModel.listParamFlow.collectAsState()
    val dateParams by viewModel.dateParamsFlow.collectAsState()

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
                viewModel.updateDateQuery(day = viewModel.datesForTimespan.first.plusDays(it.toLong()))
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
                            val filtersChanged by viewModel.filtersChanged.collectAsState()
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
                                BadgedBox({ if (filtersChanged) { Badge() } }) {
                                    Icon(
                                        painter = painterResource(R.drawable.filter_list),
                                        contentDescription = stringResource(R.string.filter)
                                    )
                                }
                            }
                        },
                        menuContent = {}
                    )
                    toggleableItem(
                        onCheckedChange = {
                            viewModel.updateDateQuery(showMonth = true)
                            haptic.performHapticFeedback(HapticFeedbackType.ToggleOn)
                        },
                        label = dateParams.day.month.getName(java.time.format.TextStyle.FULL),
                        checked = !dateParams.showMonth,
                        weight = 3f
                    )
                    toggleableItem(
                        onCheckedChange = {
                            viewModel.updateDateQuery(showMonth = false)
                            haptic.performHapticFeedback(HapticFeedbackType.ToggleOn)
                        },
                        label = dateParams.day.dayOfMonth.toString(),
                        checked = dateParams.showMonth,
                        weight = 1f
                    )
                }
            }
        }
        if (listParam == ListParam.AppList) AppList(sidePadding, paddingValues)
        else HourList(sidePadding, paddingValues)
    }
}

@Composable
private fun AppList(
    sidePadding: Dp,
    paddingValues: PaddingValues,
) {
    val viewModel: HistoryVM = koinViewModel()
    val context = LocalContext.current

    val appList by remember { viewModel.appList }.collectAsState()
    var appSelected by remember { mutableIntStateOf(-1) }
    val totalMaximum = remember(appList) { appList.find { it.app.uidQuery == null }?.usage?.totalUsage ?: 0 }

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
        items(appList, { it.app.uid }) { item ->
            Box(Modifier.animateItem()) {
                AppItem(
                    app = item.app,
                    usage1 = item.usage.usage1,
                    usage2 = item.usage.usage2,
                    name = item.app.getName(context),
                    selected = item.app.uid == appSelected,
                    maximum = totalMaximum,
                    onClick = {appSelected = if (appSelected != item.app.uid) item.app.uid else -1}
                ) {
                    item.app.GetIcon(Modifier.size(imageWidth))
                }
            }
        }
    }
}

@Composable
private fun HourList(
    sidePadding: Dp,
    paddingValues: PaddingValues,
) {
    val viewModel: HistoryVM = koinViewModel()
    val context = LocalContext.current

    val hourList by remember { viewModel.hourList }.collectAsState()
    var hourSelected by remember { mutableIntStateOf(-1) }
    val maximum by remember { derivedStateOf { hourList.sumOf { it.usage.totalUsage } } }
    val textMeasurer = rememberTextMeasurer()
    val measurement = textMeasurer.measure(
        text = LocalTime.MIDNIGHT.toLocaleHourString(context, true),
        style = TextStyle(
            fontFamily = momoTrustDisplayFont(),
            fontSize = MaterialTheme.typography.titleMedium.fontSize,
        )
    )
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
        items(hourList, { it.start.hour }) { item ->
            Box(Modifier.animateItem()) {
                AppItem(
                    usage1 = item.usage.usage1,
                    usage2 = item.usage.usage2,
                    name = item.toString(context),
                    selected = item.start.hour == hourSelected,
                    maximum = maximum,
                    onClick = {hourSelected = if (item.start.hour != hourSelected) item.start.hour else -1}
                ) {
                    Box (Modifier.width(measurement.size.width.toDp + 8.dp).height(32.dp)) {
                        Text(
                            modifier = Modifier.align(Alignment.Center),
                            text = item.start.toLocalTime().toLocaleHourString(context, true),
                            fontFamily = momoTrustDisplayFont(),
                            fontSize = MaterialTheme.typography.titleMedium.fontSize,
                            textAlign = TextAlign.Center,
                        )
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
                usageQuery1.dataUID.GetIcon(Modifier.size(24.dp), foregroundColor)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun HistoryFilter(onDismiss: () -> Unit) {
    val haptic = LocalHapticFeedback.current
    val context = LocalContext.current
    val viewModel: HistoryVM = koinViewModel()

    val usageQuery1 by viewModel.query1Flow.collectAsState()
    val usageQuery2 by viewModel.query2Flow.collectAsState()
    val listParam by viewModel.listParamFlow.collectAsState()

    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .card()
                .padding(16.dp)
        ) {
            Text(
                text = stringResource(R.string.filter),
                style = MaterialTheme.typography.headlineSmallEmphasized
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
            HorizontalDivider(Modifier.padding(vertical = 8.dp))
            Text(
                text = stringResource(R.string.list),
                style = MaterialTheme.typography.headlineSmallEmphasized
            )
            val forceHourList by viewModel.forceHourList.collectAsState()
            ButtonGroup(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(
                    4.dp,
                    Alignment.CenterHorizontally
                ),
                expandedRatio = 0.05f,
                overflowIndicator = {}
            ) {
                toggleableItem(
                    onCheckedChange = {
                        viewModel.updateListQuery(ListParam.AppList)
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    },
                    label = ListParam.AppList.getString(context),
                    enabled = !forceHourList,
                    checked = listParam == ListParam.AppList,
                    weight = 1f
                )
                toggleableItem(
                    onCheckedChange = {
                        viewModel.updateListQuery(ListParam.HourList)
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    },
                    label = ListParam.HourList.getString(context),
                    checked = listParam == ListParam.HourList,
                    weight = 1f
                )
            }
            HorizontalDivider(Modifier.padding(vertical = 8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)
            ) {
                TextButton(
                    onClick = {
                        viewModel.resetFilters()
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    }
                ) {
                    Text(stringResource(R.string.reset))
                }
                TextButton(
                    onClick = {
                        viewModel.persistFilters()
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onDismiss()
                    }
                ) {
                    Text(stringResource(R.string.persist))
                }
                Button(onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onDismiss()
                }) {
                    Text(stringResource(R.string.close))
                }
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
    val context = LocalContext.current
    val viewModel: HistoryVM = koinViewModel()
    val appManager: AppManager = koinInject()

    Column (modifier = Modifier.weight(1f)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            color = if (n == 1) colorScheme.primary else colorScheme.tertiary,
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
            query.dataUID.GetIcon(Modifier.size(24.dp))
            Text(query.dataUID.getName(context))
        }

        if (showAppPicker) {
            AppSearchDialog (
                onSelect = { uid ->
                    viewModel.updateQuery(n, query.copy(dataUID = appManager.getAppForUID(uid)))
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
        val context = LocalContext.current

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

        val includedApps by remember { appManager.allAppsFlow }.collectAsState(emptyList())
        val appsPlusOther by remember { derivedStateOf {
            listOf(allApp, tetheringApp, removedApp).plus(includedApps)
        } }
        var query by remember { mutableStateOf("") }
        val searchResults by remember {
            derivedStateOf {
                if (query.isEmpty()) appsPlusOther
                else appsPlusOther.filter { it.getName(context).lowercase().contains(query.lowercase()) }
            }
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            AppSelector(searchResults) { uid -> onSelect(uid) }
            SearchBar(
                modifier = Modifier
                    .padding(horizontal = 8.dp)
                    .fillMaxWidth()
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
        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp),
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
    app: DataUID? = null,
    usage1: Long,
    usage2: Long,
    name: String,
    selected: Boolean,
    maximum: Long,
    onClick: () -> Unit = {},
    icon: @Composable (() -> Unit)
) {
    val haptic = LocalHapticFeedback.current
    val activity = LocalActivity.current
    val viewModel: HistoryVM = koinViewModel()
    val appManager: AppManager = koinInject()
    val usageQuery1 by viewModel.query1Flow.collectAsState()
    val usageQuery2 by viewModel.query2Flow.collectAsState()

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
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                icon()
                Column {
                    AnimatedVisibility(
                        visible = selected,
                        enter = expandVertically(spring(0.7f, Spring.StiffnessMedium)),
                        exit = shrinkVertically(spring(0.7f, Spring.StiffnessMedium))
                    ) {
                        Column {
                            Text(
                                modifier = Modifier
                                    .height(32.dp)
                                    .wrapContentHeight(Alignment.CenterVertically),
                                text = name,
                                fontWeight = FontWeight.Bold,
                            )
                            LineGraphHeader()
                        }
                    }
                    LineGraph(
                        maximum = maximum,
                        data = Pair(usage1, usage2)
                    )
                    val noOptionApps = listOf(allApp, unknownApp)
                    AnimatedVisibility(
                        visible = selected && app != null && !noOptionApps.contains(app),
                        enter = expandVertically(spring(0.7f, Spring.StiffnessMedium)),
                        exit = shrinkVertically(spring(0.7f, Spring.StiffnessMedium))
                    ) {
                        Column {
                            HorizontalDivider(Modifier.padding(vertical = 4.dp))
                            Row (
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End,
                            ) {
                                Button(
                                    shape = MaterialTheme.shapes.small,
                                    onClick = {
                                        app?.uid?.let {
                                            viewModel.updateQuery(1, usageQuery1.copy(dataUID = appManager.getAppForUID(it)))
                                            viewModel.updateQuery(2, usageQuery2.copy(dataUID = appManager.getAppForUID(it)))
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        }
                                    }
                                ) {
                                    Text(stringResource(R.string.quick_filter))
                                }
                                if (app is DataUIDApp) {
                                    FilledIconButton(
                                        onClick = {
                                            viewModel.openPackageSettings(activity, app.uid)
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        }
                                    ) {
                                        Icon(
                                            painterResource(R.drawable.settings),
                                            stringResource(R.string.settings)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun LineGraphHeader() {
    val context = LocalContext.current
    val viewModel: HistoryVM = koinViewModel()

    val usageQuery1 by viewModel.query1Flow.collectAsState()
    val usageQuery2 by viewModel.query2Flow.collectAsState()

    Column (Modifier.fillMaxWidth()) {
        Row {
            if (usageQuery1.dataType.isNotEmpty()) {
                Text(
                    modifier = Modifier.weight(1f),
                    text = usageQuery1.toString(context),
                    style = MaterialTheme.typography.titleMedium,
                    color = colorScheme.tertiary
                )
            }
            if (usageQuery2.dataType.isNotEmpty()) {
                Text(
                    modifier = Modifier.weight(1f),
                    text = usageQuery2.toString(context),
                    textAlign = TextAlign.End,
                    style = MaterialTheme.typography.titleMedium,
                    color = colorScheme.tertiary
                )
            }
        }
    }
}
