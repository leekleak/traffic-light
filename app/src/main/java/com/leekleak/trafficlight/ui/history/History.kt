package com.leekleak.trafficlight.ui.history

import android.app.Activity
import android.content.Intent
import androidx.activity.compose.LocalActivity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ButtonGroup
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipAnchorPosition
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfoV2
import androidx.compose.material3.rememberBottomSheetState
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
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
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.leekleak.core.R
import com.leekleak.core.charts.LineGraph
import com.leekleak.core.charts.ScrollableBarGraph
import com.leekleak.core.ui.components.BackAction
import com.leekleak.core.ui.components.HazeScaffold
import com.leekleak.core.ui.components.SearchField
import com.leekleak.core.ui.components.ShizukuStoppedTag
import com.leekleak.core.ui.components.SlideAnimatedVisibility
import com.leekleak.core.ui.components.iconToggleButton
import com.leekleak.core.ui.navigation.LocalNavigationBarPadding
import com.leekleak.core.ui.theme.googleSans
import com.leekleak.core.ui.theme.historyItemFont
import com.leekleak.core.ui.theme.shelfShape
import com.leekleak.core.ui.theme.variableRoundedEnd
import com.leekleak.core.ui.theme.variableRoundedStart
import com.leekleak.core.util.DataSize
import com.leekleak.core.util.getName
import com.leekleak.core.util.openLink
import com.leekleak.core.util.rememberIs24HourFormat
import com.leekleak.core.util.toDp
import com.leekleak.core.util.toLocaleHourString
import com.leekleak.trafficlight.database.DataDirection
import com.leekleak.trafficlight.database.DataType
import com.leekleak.trafficlight.database.DropdownItem
import com.leekleak.trafficlight.database.UsageQuery
import com.leekleak.trafficlight.model.AppManager
import com.leekleak.trafficlight.model.AppManager.Companion.allApp
import com.leekleak.trafficlight.model.AppManager.Companion.otherUsersApp
import com.leekleak.trafficlight.model.AppManager.Companion.removedApp
import com.leekleak.trafficlight.model.AppManager.Companion.tetheringApp
import com.leekleak.trafficlight.model.DataUID
import com.leekleak.trafficlight.model.DataUIDApp
import com.leekleak.trafficlight.model.search
import com.leekleak.trafficlight.ui.plans.AppSelector
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.LocalTime
import kotlin.time.Duration.Companion.milliseconds

val imageWidth = 32.dp

@Composable
fun History(
    uiState: HistoryUiState,
    appManager: AppManager,
    refresh: () -> Unit,
    updateDateQuery: (day: LocalDate, showMonth: Boolean) -> Unit,
    updateQuery: (Int, UsageQuery) -> Unit,
    updateQueries: (UsageQuery, UsageQuery) -> Unit,
    updateListQuery: (ListParam) -> Unit,
    persistFilters: () -> Unit,
    resetFilters: () -> Unit,
    openApp: (Activity?, Intent?) -> Unit,
    openPackageSettings: suspend (Activity?, Int) -> Unit,
) {
    LifecycleResumeEffect(Unit) {
        refresh()
        onPauseOrDispose {}
    }

    HazeScaffold(
        title = stringResource(R.string.history),
        scrollState = null,
        hazeState = null,
        backAction = BackAction.None,
        extraPadding = LocalNavigationBarPadding.current,
        actions = {
            if (uiState.showShizukuNotRunning) {
                ShizukuStoppedTag()
            }
        }
    ) { paddingValues ->
        val layoutDirection = LocalLayoutDirection.current
        val startPadding = remember(paddingValues, layoutDirection) {
            paddingValues.calculateStartPadding(layoutDirection)
        }
        val endPadding = remember(paddingValues, layoutDirection) {
            paddingValues.calculateEndPadding(layoutDirection)
        }
        val listContentPadding = PaddingValues(
            start = 16.dp,
            top = 16.dp,
            end = 16.dp,
            bottom = paddingValues.calculateBottomPadding()
        )
        val isWidescreen = currentWindowAdaptiveInfoV2()
            .windowSizeClass
            .isWidthAtLeastBreakpoint(600)

        if (isWidescreen) {
            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(top = paddingValues.calculateTopPadding(), start = startPadding),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    HistoryGraphAndFilters(
                        uiState = uiState,
                        appManager = appManager,
                        updateDateQuery = updateDateQuery,
                        updateQuery = updateQuery,
                        updateListQuery = updateListQuery,
                        persistFilters = persistFilters,
                        resetFilters = resetFilters,
                    )
                    HistoryLegend(uiState)
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .padding(top = paddingValues.calculateTopPadding())
                ) {
                    HistoryList(
                        uiState = uiState,
                        updateQueries = updateQueries,
                        openApp = openApp,
                        openPackageSettings = openPackageSettings,
                        contentPadding = listContentPadding,
                    )
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .padding(
                        start = startPadding,
                        end = endPadding,
                        top = paddingValues.calculateTopPadding()
                    ),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                HistoryGraphAndFilters(
                    uiState = uiState,
                    appManager = appManager,
                    updateDateQuery = updateDateQuery,
                    updateQuery = updateQuery,
                    updateListQuery = updateListQuery,
                    persistFilters = persistFilters,
                    resetFilters = resetFilters,
                )
            }
            HistoryLegend(uiState, Modifier.padding(start = startPadding, end = endPadding))
            HistoryList(
                uiState = uiState,
                updateQueries = updateQueries,
                openApp = openApp,
                openPackageSettings = openPackageSettings,
                contentPadding = listContentPadding,
            )
        }
    }
}

@Composable
private fun HistoryGraphAndFilters(
    uiState: HistoryUiState,
    appManager: AppManager,
    updateDateQuery: (day: LocalDate, showMonth: Boolean) -> Unit,
    updateQuery: (Int, UsageQuery) -> Unit,
    updateListQuery: (ListParam) -> Unit,
    persistFilters: () -> Unit,
    resetFilters: () -> Unit,
) {
    val haptic = LocalHapticFeedback.current

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        ScrollableBarGraph(uiState.usage) {
            updateDateQuery(
                uiState.timespanStartDate.plusDays(it.toLong()),
                uiState.dateParams.showMonth
            )
        }
        Row(
            Modifier
                .fillMaxWidth()
                .clip(MaterialTheme.shapes.small)
                .background(colorScheme.primary)
                .padding(horizontal = 4.dp)
        ) {
            var showFilter by remember { mutableStateOf(false) }
            if (showFilter) HistoryFilter(
                uiState = uiState,
                appManager = appManager,
                updateQuery = updateQuery,
                updateListQuery = updateListQuery,
                persistFilters = persistFilters,
                resetFilters = resetFilters,
            ) { showFilter = false }
            val buttonUnselectedColors = IconButtonDefaults.iconButtonColors(
                containerColor = colorScheme.surfaceContainer,
                contentColor = colorScheme.onSurfaceVariant
            )
            ButtonGroup(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(
                    4.dp,
                    Alignment.CenterHorizontally
                ),
                expandedRatio = 0.05f,
                overflowIndicator = {}
            ) {
                iconToggleButton(
                    showBadge = uiState.filtersChanged,
                    selected = showFilter,
                    onSelect = { showFilter = true },
                    unselectedColors = buttonUnselectedColors
                ) {
                    Icon(
                        painter = painterResource(R.drawable.filter_list),
                        contentDescription = stringResource(R.string.filter)
                    )
                }
                toggleableItem(
                    onCheckedChange = {
                        updateDateQuery(uiState.dateParams.day, true)
                        haptic.performHapticFeedback(HapticFeedbackType.ToggleOn)
                    },
                    label = uiState.dateParams.day.month.getName(java.time.format.TextStyle.FULL),
                    checked = !uiState.dateParams.showMonth,
                    weight = 3f
                )
                toggleableItem(
                    onCheckedChange = {
                        updateDateQuery(uiState.dateParams.day, false)
                        haptic.performHapticFeedback(HapticFeedbackType.ToggleOn)
                    },
                    label = uiState.dateParams.day.dayOfMonth.toString(),
                    checked = uiState.dateParams.showMonth,
                    weight = 1f
                )
            }
        }
    }
}

@Composable
private fun HistoryLegend(uiState: HistoryUiState, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        HistoryLegendItem(
            usageQuery = uiState.queries.first,
            byteUsage = uiState.totalUsage.usage1,
            showUsage = uiState.appList.isNotEmpty(),
            foregroundColor = colorScheme.primary,
            shape = variableRoundedStart
        )
        HistoryLegendItem(
            usageQuery = uiState.queries.second,
            byteUsage = uiState.totalUsage.usage2,
            showUsage = uiState.appList.isNotEmpty(),
            foregroundColor = colorScheme.tertiary,
            shape = variableRoundedEnd
        )
    }
}

@Composable
private fun HistoryList(
    uiState: HistoryUiState,
    updateQueries: (UsageQuery, UsageQuery) -> Unit,
    openApp: (Activity?, Intent?) -> Unit,
    openPackageSettings: suspend (Activity?, Int) -> Unit,
    contentPadding: PaddingValues,
) {
    if (uiState.listParam == ListParam.AppList) {
        AppList(
            uiState = uiState,
            updateQueries = updateQueries,
            openApp = openApp,
            openPackageSettings = openPackageSettings,
            contentPadding = contentPadding,
        )
    } else {
        HourList(
            uiState = uiState,
            updateQueries = updateQueries,
            openApp = openApp,
            openPackageSettings = openPackageSettings,
            contentPadding = contentPadding,
        )
    }
}

@Composable
private fun AppList(
    uiState: HistoryUiState,
    updateQueries: (UsageQuery, UsageQuery) -> Unit,
    openApp: (Activity?, Intent?) -> Unit,
    openPackageSettings: suspend (Activity?, Int) -> Unit,
    contentPadding: PaddingValues,
) {
    val context = LocalContext.current

    var appSelected by remember { mutableIntStateOf(-1) }
    val totalMaximum = remember(uiState.appList) {
        uiState.appList.maxOfOrNull { it.usage.totalUsage } ?: 0
    }

    val listState = rememberLazyListState()
    LazyColumn(
        modifier = Modifier
            .padding(top = 8.dp)
            .clip(shelfShape)
            .background(colorScheme.surfaceContainer)
            .fillMaxSize(),
        contentPadding = contentPadding,
        verticalArrangement = Arrangement.spacedBy(4.dp),
        state = listState
    ) {
        items(uiState.appList, key = { it.app.uid }, contentType = { "app" }) { item ->
            Box(Modifier.animateItem()) {
                AppItem(
                    usageQueries = uiState.queries,
                    updateQueries = updateQueries,
                    openApp = openApp,
                    openPackageSettings = openPackageSettings,
                    app = item.app,
                    usage1 = item.usage.usage1,
                    usage2 = item.usage.usage2,
                    name = item.app.getName(context),
                    isSelected = { appSelected == item.app.uid },
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
    uiState: HistoryUiState,
    updateQueries: (UsageQuery, UsageQuery) -> Unit,
    openApp: (Activity?, Intent?) -> Unit,
    openPackageSettings: suspend (Activity?, Int) -> Unit,
    contentPadding: PaddingValues,
) {
    var hourSelected by remember { mutableIntStateOf(-1) }
    val maximum = remember(uiState.hourList) { uiState.hourList.sumOf { it.usage.totalUsage } }
    val totalUsage1 = remember(uiState.hourList) { uiState.hourList.sumOf { it.usage.usage1 } }
    val totalUsage2 = remember(uiState.hourList) { uiState.hourList.sumOf { it.usage.usage2 } }
    val textMeasurer = rememberTextMeasurer()
    val font = remember { historyItemFont() }
    val is24HourFormat by rememberIs24HourFormat()
    val labelFontSize = MaterialTheme.typography.titleMedium.fontSize
    val measurement = remember(is24HourFormat, font, labelFontSize) {
        textMeasurer.measure(
            text = LocalTime.MIDNIGHT.toLocaleHourString(is24HourFormat, true),
            style = TextStyle(
                fontFamily = font,
                fontSize = labelFontSize,
            )
        )
    }
    val listState = rememberLazyListState()
    LazyColumn(
        modifier = Modifier
            .padding(top = 8.dp)
            .clip(shelfShape)
            .background(colorScheme.surfaceContainer)
            .fillMaxSize(),
        contentPadding = contentPadding,
        verticalArrangement = Arrangement.spacedBy(6.dp),
        state = listState
    ) {
        item {
            Box(Modifier.animateItem()) {
                val key = -10
                AppItem(
                    usageQueries = uiState.queries,
                    updateQueries = updateQueries,
                    openApp = openApp,
                    openPackageSettings = openPackageSettings,
                    usage1 = totalUsage1,
                    usage2 = totalUsage2,
                    name = stringResource(R.string.total_usage),
                    isSelected = { hourSelected == key },
                    maximum = maximum,
                    onClick = {hourSelected = if (key != hourSelected) key else -1}
                ) {
                    Box (Modifier
                        .width(measurement.size.width.toDp + 8.dp)
                        .height(32.dp)) {
                        Icon(
                            modifier = Modifier.align(Alignment.Center),
                            painter = painterResource(R.drawable.clock),
                            contentDescription = stringResource(R.string.total_usage)
                        )
                    }
                }
            }
        }
        items(uiState.hourList, key = { it.start.hour }, contentType = { "hour" }) { item ->
            Box(Modifier.animateItem()) {
                AppItem(
                    usageQueries = uiState.queries,
                    updateQueries = updateQueries,
                    openApp = openApp,
                    openPackageSettings = openPackageSettings,
                    usage1 = item.usage.usage1,
                    usage2 = item.usage.usage2,
                    name = item.toString(is24HourFormat),
                    isSelected = { hourSelected == item.start.hour },
                    maximum = maximum,
                    onClick = {hourSelected = if (item.start.hour != hourSelected) item.start.hour else -1}
                ) {
                    Box (Modifier
                        .width(measurement.size.width.toDp + 8.dp)
                        .height(32.dp)) {
                        Text(
                            modifier = Modifier.align(Alignment.Center),
                            text = item.start.toLocalTime().toLocaleHourString(is24HourFormat, true),
                            fontFamily = font,
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
private fun RowScope.HistoryLegendItem(
    usageQuery: UsageQuery,
    byteUsage: Long,
    showUsage: Boolean,
    foregroundColor: Color,
    shape: Shape,
) {
    Column(
        modifier = Modifier
            .weight(1f)
            .heightIn(72.dp)
            .background(colorScheme.background, shape)
            .border(1.dp, foregroundColor, shape = shape)
            .padding(vertical = 4.dp, horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Row(
            modifier = Modifier
        ) {
            Icon(
                painter = painterResource(usageQuery.dataType.iconRes),
                contentDescription = stringResource(usageQuery.dataType.nameRes),
                tint = foregroundColor
            )
            AnimatedVisibility(usageQuery.dataType != DataType.None) {
                Row {
                    Icon(
                        painter = painterResource(usageQuery.dataDirection.iconRes),
                        contentDescription = stringResource(usageQuery.dataDirection.nameRes),
                        tint = foregroundColor
                    )
                    usageQuery.dataUID.GetIcon(Modifier.size(24.dp), foregroundColor)
                }
            }
        }
        SlideAnimatedVisibility(showUsage) {
            val usage = DataSize(byteUsage)
            val font = remember { historyItemFont() }
            Text(
                text = usage.toString(),
                color = foregroundColor,
                fontFamily = font,
                fontSize = 20.sp
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryFilter(
    uiState: HistoryUiState,
    appManager: AppManager,
    updateQuery: (Int, UsageQuery) -> Unit,
    updateListQuery: (ListParam) -> Unit,
    persistFilters: () -> Unit,
    resetFilters: () -> Unit,
    onDismiss: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val context = LocalContext.current

    val font = remember { googleSans(weight = 600f) }

    val sheetState = rememberBottomSheetState(
        initialValue = SheetValue.Hidden,
        enabledValues = setOf(SheetValue.Hidden, SheetValue.Expanded)
    )
    ModalBottomSheet (
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        Column(modifier = Modifier.systemBarsPadding()) {
            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                Text(
                    text = stringResource(R.string.filter),
                    style = MaterialTheme.typography.headlineSmallEmphasized,
                    fontFamily = font
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    HistoryItemSettings(
                        appManager = appManager,
                        updateQuery = updateQuery,
                        title = stringResource(R.string.primary),
                        n = 1,
                        query = uiState.queries.first
                    )
                    HistoryItemSettings(
                        appManager = appManager,
                        updateQuery = updateQuery,
                        title = stringResource(R.string.secondary),
                        n = 2,
                        query = uiState.queries.second
                    )
                }
                HorizontalDivider(Modifier.padding(vertical = 8.dp))
                Text(
                    text = stringResource(R.string.list),
                    style = MaterialTheme.typography.headlineSmallEmphasized,
                    fontFamily = font
                )
                ButtonGroup(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(
                        4.dp,
                        Alignment.CenterHorizontally
                    ),
                    expandedRatio = 0.05f,
                    overflowIndicator = {}
                ) {
                    iconToggleButton(
                        onSelect = { updateListQuery(ListParam.AppList) },
                        text = ListParam.AppList.getString(context),
                        icon = { Icon(painterResource(R.drawable.apps), null) },
                        enabled = !uiState.forceHourList,
                        selected = uiState.listParam == ListParam.AppList,
                        weight = 1f
                    )
                    iconToggleButton(
                        onSelect = { updateListQuery(ListParam.HourList) },
                        text = ListParam.HourList.getString(context),
                        icon = { Icon(painterResource(R.drawable.clock_analog), null) },
                        selected = uiState.listParam == ListParam.HourList,
                        weight = 1f
                    )
                }
                HorizontalDivider(Modifier.padding(vertical = 8.dp))
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState(Int.MAX_VALUE))
                    .padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
            ) {
                val contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp)
                Spacer(Modifier.width(8.dp))
                Button (
                    onClick = {
                        resetFilters()
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    },
                    enabled = uiState.filtersChanged,
                    contentPadding = contentPadding,
                    colors = ButtonDefaults.filledTonalButtonColors()
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(painterResource(R.drawable.reset_wrench), null)
                        Text(stringResource(R.string.reset))
                    }
                }
                Button (
                    onClick = {
                        persistFilters()
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    },
                    enabled = uiState.filtersChanged,
                    contentPadding = contentPadding,
                    colors = ButtonDefaults.filledTonalButtonColors()
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(painterResource(R.drawable.archive), null)
                        Text(stringResource(R.string.set_as_default))
                    }
                }
                Spacer(Modifier.width(8.dp))
            }
        }
    }
}

@Composable
fun RowScope.HistoryItemSettings(
    appManager: AppManager,
    updateQuery: (Int, UsageQuery) -> Unit,
    title: String,
    n: Int,
    query: UsageQuery
) {
    val hapticFeedback = LocalHapticFeedback.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    Column (modifier = Modifier.weight(1f)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            color = if (n == 1) colorScheme.primary else colorScheme.tertiary,
        )
        FilterDropdownButton(
            n = n,
            enabled = true,
            onSelect = {
                updateQuery(n, query.copy(dataType = it))
                hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
            },
            text = stringResource(query.dataType.nameRes),
            items = DataType.entries
        ) {
            Icon(painterResource(query.dataType.iconRes), null)
        }
        FilterDropdownButton(
            n = n,
            enabled = query.dataType != DataType.None,
            onSelect = {
                updateQuery(n, query.copy(dataDirection = it))
                hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
            },
            text = stringResource(query.dataDirection.nameRes),
            items = DataDirection.entries
        ) {
            Icon(painterResource(query.dataDirection.iconRes), null)
        }

        var showAppPicker by remember { mutableStateOf(false) }
        FilterButton(
            n = n,
            enabled = query.dataType != DataType.None,
            onClick = {
                showAppPicker = true
                hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
            },
            text = query.dataUID.getName(context)
        ) {
            query.dataUID.GetIcon(Modifier.size(24.dp))
        }

        if (showAppPicker) {
            AppSearchDialog (
                appManager,
                onSelect = { uid ->
                    scope.launch {
                        updateQuery(n, query.copy(dataUID = appManager.getAppForUID(uid)))
                        hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                    }
                }
            ){
                showAppPicker = false
            }
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class, FlowPreview::class)
private fun AppSearchDialog(
    appManager: AppManager,
    onSelect: (uid: Int) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberBottomSheetState(SheetValue.Hidden)
    ModalBottomSheet (
        modifier = Modifier.imePadding(),
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        val textFieldState = rememberTextFieldState()
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

        val includedApps by produceState(emptyList()) { value = listOf(allApp, tetheringApp, removedApp).plus(appManager.getAllApps()) }
        val searchResults by produceState(initialValue = includedApps, includedApps) {
            snapshotFlow { textFieldState.text.toString() }
                .debounce(150.milliseconds)
                .collect { text ->
                    value = withContext(Dispatchers.Default) {
                        includedApps.search(text, context)
                    }
                }
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            AppSelector(searchResults, Modifier.fillMaxWidth()) { uid -> onSelect(uid) }
            Box(Modifier.padding(horizontal = 8.dp)) {
                SearchField(
                    modifier = Modifier.focusRequester(focusRequester),
                    textFieldState = textFieldState,
                    placeholder = stringResource(R.string.search),
                    icon = R.drawable.search
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FilterButton(
    n: Int,
    enabled: Boolean,
    onClick: (() -> Unit),
    text: String,
    icon: @Composable (() -> Unit)
) {
    val state = rememberTooltipState()
    TooltipBox(
        modifier = Modifier.fillMaxWidth(),
        positionProvider = TooltipDefaults.rememberTooltipPositionProvider(
            TooltipAnchorPosition.Above,
            4.dp
        ),
        tooltip = { PlainTooltip { Text(text) } },
        state = state
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
                icon()
                Text(text, maxLines = 1, softWrap = false)
            }
        }
    }
}

@Composable
private fun <T : DropdownItem> FilterDropdownButton(
    n: Int,
    enabled: Boolean,
    onSelect: (item: T) -> Unit,
    items: List<T>,
    text: String,
    icon: @Composable (() -> Unit)
) {
    val scrollState = rememberScrollState()
    var expanded by remember { mutableStateOf(false)}
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentSize(Alignment.TopStart)
    ) {
        FilterButton(
            n = n,
            enabled = enabled,
            onClick = {expanded = true},
            text = text,
            icon = icon
        )
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            shape = MaterialTheme.shapes.medium,
            scrollState = scrollState,
        ) {
            items.forEach {
                it.DropdownMenuItem { onSelect(it) }
            }
        }
    }
}

@Composable
fun AppItem(
    modifier: Modifier = Modifier,
    usageQueries: Pair<UsageQuery, UsageQuery>,
    updateQueries: (UsageQuery, UsageQuery) -> Unit,
    openApp: (Activity?, Intent?) -> Unit,
    openPackageSettings: suspend (Activity?, Int) -> Unit,
    app: DataUID? = null,
    usage1: Long,
    usage2: Long,
    name: String,
    isSelected: () -> Boolean,
    maximum: Long,
    onClick: () -> Unit = {},
    icon: @Composable (() -> Unit)
) {
    val haptic = LocalHapticFeedback.current

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
                ExpandableHeader(isSelected = isSelected, name = name, usageQueries = usageQueries)
                val graphUsage1 = if (usageQueries.first.dataType != DataType.None) usage1 else null
                val graphUsage2 = if (usageQueries.second.dataType != DataType.None) usage2 else null
                LineGraph(
                    maximum = maximum,
                    data = Pair(graphUsage1, graphUsage2)
                )
                ExpandableActions(
                    isSelected = isSelected,
                    modifier = modifier,
                    usageQueries = usageQueries,
                    updateQueries = updateQueries,
                    openApp = openApp,
                    openPackageSettings = openPackageSettings,
                    app = app,
                )
            }
        }
    }
}

@Composable
private fun ExpandableHeader(
    isSelected: () -> Boolean,
    name: String,
    usageQueries: Pair<UsageQuery, UsageQuery>,
) {
    val selected = isSelected()
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        AnimatedVisibility(
            visible = selected,
            enter = expandVertically(spring(0.7f, Spring.StiffnessMedium)),
            exit = shrinkVertically(spring(0.7f, Spring.StiffnessMedium))
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(
                    modifier = Modifier.heightIn(min = 32.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = name,
                        fontWeight = FontWeight.Bold,
                    )
                }
                LineGraphHeader(usageQueries)
            }
        }
    }
}

@Composable
private fun ExpandableActions(
    isSelected: () -> Boolean,
    modifier: Modifier,
    usageQueries: Pair<UsageQuery, UsageQuery>,
    updateQueries: (UsageQuery, UsageQuery) -> Unit,
    openApp: (Activity?, Intent?) -> Unit,
    openPackageSettings: suspend (Activity?, Int) -> Unit,
    app: DataUID?,
) {
    val selected = isSelected()
    val haptic = LocalHapticFeedback.current
    val activity = LocalActivity.current
    val scope = rememberCoroutineScope()

    AnimatedVisibility(
        visible = selected && app != null,
        enter = expandVertically(spring(0.7f, Spring.StiffnessMedium)),
        exit = shrinkVertically(spring(0.7f, Spring.StiffnessMedium))
    ) {
        Column(modifier.padding(top = 6.dp)) {
            Row (
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                Button(
                    modifier = Modifier.padding(end = 4.dp),
                    shape = MaterialTheme.shapes.large,
                    onClick = {
                        app?.let {
                            updateQueries(
                                usageQueries.first.copy(dataUID = it),
                                usageQueries.second.copy(dataUID = it),
                            )
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        }
                    }
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(painterResource(R.drawable.custom), null)
                        Text(stringResource(R.string.quick_filter))
                    }
                }
                if (app is DataUIDApp) {
                    val launchIntent by produceState<Intent?>(null, app.packageName) {
                        value = withContext(Dispatchers.IO) {
                            activity?.packageManager?.getLaunchIntentForPackage(app.packageName)
                        }
                    }
                    FilledIconButton(
                        enabled = launchIntent != null,
                        onClick = {
                            openApp(activity, launchIntent)
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        }
                    ) {
                        Icon(
                            painterResource(R.drawable.open_in_new),
                            stringResource(R.string.open_app)
                        )
                    }
                    FilledIconButton(
                        onClick = {
                            scope.launch {
                                openPackageSettings(activity, app.uid)
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            }
                        }
                    ) {
                        Icon(
                            painterResource(R.drawable.settings),
                            stringResource(R.string.settings)
                        )
                    }
                } else if (app == otherUsersApp) {
                    Button(
                        modifier = Modifier.padding(horizontal = 4.dp),
                        shape = MaterialTheme.shapes.large,
                        onClick = { openLink(activity, "https://github.com/leekleak/traffic-light/wiki/Troubleshooting#network-usage") },
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(painterResource(R.drawable.help), null)
                            Text(stringResource(R.string.help))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun LineGraphHeader(usageQueries: Pair<UsageQuery, UsageQuery>) {
    val context = LocalContext.current

    Column (Modifier.fillMaxWidth()) {
        Row {
            if (usageQueries.first.dataType != DataType.None) {
                Text(
                    modifier = Modifier.weight(1f),
                    text = usageQueries.first.toString(context),
                    style = MaterialTheme.typography.titleMedium,
                    color = colorScheme.tertiary
                )
            }
            if (usageQueries.second.dataType != DataType.None) {
                Text(
                    modifier = Modifier.weight(1f),
                    text = usageQueries.second.toString(context),
                    textAlign = TextAlign.End,
                    style = MaterialTheme.typography.titleMedium,
                    color = colorScheme.tertiary
                )
            }
        }
    }
}
