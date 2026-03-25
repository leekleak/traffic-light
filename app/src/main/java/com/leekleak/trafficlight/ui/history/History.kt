package com.leekleak.trafficlight.ui.history

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ButtonGroup
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import coil3.compose.rememberAsyncImagePainter
import com.leekleak.trafficlight.R
import com.leekleak.trafficlight.charts.LineGraph
import com.leekleak.trafficlight.charts.ScrollableBarGraph
import com.leekleak.trafficlight.charts.model.ScrollableBarData
import com.leekleak.trafficlight.database.Mobile
import com.leekleak.trafficlight.database.Wifi
import com.leekleak.trafficlight.model.AppIcon
import com.leekleak.trafficlight.model.NetworkUsageManager
import com.leekleak.trafficlight.util.CategoryTitleText
import com.leekleak.trafficlight.util.getName
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
    val networkUsageManager: NetworkUsageManager = koinInject()
    val viewModel: HistoryVM = koinViewModel()
    val haptic = LocalHapticFeedback.current

    var appDay by remember { mutableStateOf( LocalDate.now()) }
    var showMonth by remember { mutableStateOf(false) }

    LaunchedEffect(appDay, showMonth) {
        viewModel.updateQuery(appDay, showMonth)
    }

    val appList by remember { viewModel.appList }.collectAsState()
    var appSelected by remember { mutableIntStateOf(-1) }

    val days = remember { getDatesForTimespan() }

    val usage: List<ScrollableBarData> by remember {
        networkUsageManager.daysUsage(days.first, days.second, listOf(Mobile, Wifi))
    }.collectAsState(List(MAX_DAYS) {
        ScrollableBarData(LocalDate.now())
    })

    val selectedUsage by remember { viewModel.totalUsage }.collectAsState(null)
    val totalMaximum = remember(selectedUsage) { selectedUsage?.usages?.values?.sum() }
    val appTotal = remember(appList) { viewModel.appUsageSum(appList) }
    val sidePadding = remember(paddingValues) { paddingValues.calculateLeftPadding(LayoutDirection.Ltr) }

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
            CategoryTitleText(stringResource(R.string.history))
            ScrollableBarGraph(usage) {
                appDay = days.first.plusDays(it.toLong())
            }
            Row(
                Modifier.fillMaxWidth()
                    .clip(MaterialTheme.shapes.small)
                    .background(colorScheme.primary)
                    .padding(horizontal = 4.dp)
            ) {
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
                        totalWifi = selectedUsage?.usages?.get(Wifi) ?: 0L,
                        totalCellular = selectedUsage?.usages?.get(Mobile) ?: 0L,
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
                        val painter = item.drawableResource?.let { icon = true; painterResource(it) } ?:
                            rememberAsyncImagePainter(AppIcon(item.packageName))
                        AppItem(
                            totalWifi = item.usage.usages[Wifi] ?: 0L,
                            totalCellular = item.usage.usages[Mobile] ?: 0L,
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
                                    totalWifi = (it.usages[Wifi] ?: 0L) - (appTotal.usages[Wifi] ?: 0L),
                                    totalCellular = (it.usages[Mobile] ?: 0L) - (appTotal.usages[Mobile] ?: 0L),
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
}

fun getDatesForTimespan(): Pair<LocalDate, LocalDate> {
    val now = LocalDate.now().plusDays(1)
    val base = now.minusDays(MAX_DAYS.toLong())
    return Pair(base, now)
}

@Composable
fun AppItem(
    modifier: Modifier = Modifier,
    totalWifi: Long,
    totalCellular: Long,
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
                            data = Pair(totalWifi, totalCellular)
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
                        data = Pair(totalWifi, totalCellular)
                    )
                }
            }
        }
    }
}

@Composable
fun LineGraphHeader(lineGraph: @Composable (() -> Unit)) {
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
                text = stringResource(R.string.wifi),
                style = MaterialTheme.typography.titleMedium,
                color = colorScheme.tertiary
            )
            Text(
                modifier = Modifier.weight(1f),
                text = stringResource(R.string.cellular),
                textAlign = TextAlign.End,
                style = MaterialTheme.typography.titleMedium,
                color = colorScheme.tertiary
            )
        }
        lineGraph()
    }
}
