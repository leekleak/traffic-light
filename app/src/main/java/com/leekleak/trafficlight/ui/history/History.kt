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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonGroup
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.accompanist.drawablepainter.rememberDrawablePainter
import com.leekleak.trafficlight.R
import com.leekleak.trafficlight.charts.LineGraph
import com.leekleak.trafficlight.charts.ScrollableBarGraph
import com.leekleak.trafficlight.charts.classyFont
import com.leekleak.trafficlight.charts.model.ScrollableBarData
import com.leekleak.trafficlight.database.HourlyUsageRepo
import com.leekleak.trafficlight.ui.theme.card
import com.leekleak.trafficlight.util.CategoryTitleText
import com.leekleak.trafficlight.util.getName
import org.koin.compose.koinInject
import java.time.LocalDate
import java.time.format.TextStyle
import kotlin.math.max

const val MAX_DAYS = 96
val imageWidth = 32.dp

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun History(paddingValues: PaddingValues) {
    val hourlyUsageRepo: HourlyUsageRepo = koinInject()
    val viewModel: HistoryVM = viewModel()
    val haptic = LocalHapticFeedback.current
    val context = LocalContext.current

    var appDay by remember { mutableStateOf( LocalDate.now()) }
    var showMonth by remember { mutableStateOf(false) }

    LaunchedEffect(appDay, showMonth) {
        viewModel.updateQuery(appDay, showMonth)
    }

    val appList by remember { viewModel.appList }.collectAsState()
    var appSelected by remember { mutableIntStateOf(-1) }

    val days = remember { getDatesForTimespan() }
    val usageFlow = remember { hourlyUsageRepo.daysUsage(days.first, days.second) }
    val usage: List<ScrollableBarData> by usageFlow.collectAsState(List(MAX_DAYS) {
        ScrollableBarData(LocalDate.now())
    })

    val selectedUsage by remember { viewModel.totalUsage }.collectAsState(null)
    val totalMaximum = remember(selectedUsage) { selectedUsage?.let { it.totalWifi + it.totalCellular } }
    val appTotal = remember(appList) { viewModel.appUsageSum(appList) }

    Column (
        modifier = Modifier
            .padding(
                start = paddingValues.calculateLeftPadding(LayoutDirection.Ltr),
                end = paddingValues.calculateLeftPadding(LayoutDirection.Ltr),
            )
            .statusBarsPadding(),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        CategoryTitleText(stringResource(R.string.history))
        Box(
            modifier = Modifier
                .card()
                .padding(6.dp)
                .clip(MaterialTheme.shapes.medium)
        ) {
            Box(
                modifier = Modifier
                    .clip(MaterialTheme.shapes.medium)
                    .background(colorScheme.background)
            ) {
                ScrollableBarGraph(usage) {
                    appDay = days.first.plusDays(it.toLong())
                }
            }
        }
        Row(Modifier.fillMaxWidth()) {
            ButtonGroup(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterHorizontally),
                expandedRatio = 0.05f,
                overflowIndicator = {}
            ) {
                toggleableItem(
                    onCheckedChange = {
                        showMonth = true
                        haptic.performHapticFeedback(HapticFeedbackType.ToggleOn)
                    },
                    label = appDay.month.getName(TextStyle.FULL),
                    checked = showMonth,
                    weight = 3f
                )
                toggleableItem(
                    onCheckedChange = {
                        showMonth = false
                        haptic.performHapticFeedback(HapticFeedbackType.ToggleOn)
                    },
                    label = appDay.dayOfMonth.toString(),
                    checked = !showMonth,
                    weight = 1f
                )
            }
        }
        val listState = rememberLazyListState()
        LazyColumn(
            modifier = Modifier
                .background(colorScheme.surface)
                .fillMaxSize(),
            contentPadding = PaddingValues(0.dp, 0.dp, 0.dp, paddingValues.calculateBottomPadding()),
            verticalArrangement = Arrangement.spacedBy(6.dp),
            state = listState
        ) {
            stickyHeader {
                Box (
                    Modifier
                        .fillMaxWidth()
                        .background(
                            color = colorScheme.surface,
                            shape = RoundedCornerShape(0.dp, 0.dp, 24.dp, 24.dp)
                        )
                ) {
                    AppItem(
                        totalWifi = selectedUsage?.totalWifi ?: 0L,
                        totalCellular = selectedUsage?.totalCellular ?: 0L,
                        painter = painterResource(R.drawable.data_usage),
                        icon = true,
                        name = stringResource(R.string.total_usage),
                        selected = !listState.canScrollBackward,
                        maximum = max(totalMaximum ?: 0L, 1)
                    )
                }
            }
            if (totalMaximum != null) {
                items(appList, { it.name }) { item ->
                    Box(Modifier.animateItem()) {
                        val painter = rememberDrawablePainter(context.packageManager.getApplicationIcon(item.packageName))
                        AppItem(
                            totalWifi = item.usage.totalWifi,
                            totalCellular = item.usage.totalCellular,
                            painter = painter,
                            name = item.name,
                            selected = item.uid == appSelected,
                            maximum = totalMaximum
                        ) {
                            appSelected = if (appSelected != item.uid) item.uid else -1
                            haptic.performHapticFeedback(HapticFeedbackType.ContextClick)
                        }
                    }
                }
                selectedUsage?.let {
                    if ((appTotal.totalCellular != it.totalCellular || appTotal.totalWifi != it.totalWifi) && appList.isNotEmpty()) {
                        item {
                            val uid = -99
                            Box(Modifier.animateItem()) {
                                AppItem(
                                    totalWifi = it.totalWifi - appTotal.totalWifi,
                                    totalCellular = it.totalCellular - appTotal.totalCellular,
                                    painter = painterResource(R.drawable.help),
                                    name = "Unknown",
                                    selected = uid == appSelected,
                                    maximum = totalMaximum
                                ) {
                                    appSelected = if (appSelected != uid) uid else -1
                                    haptic.performHapticFeedback(HapticFeedbackType.ContextClick)
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
    totalWifi: Long,
    totalCellular: Long,
    painter: Painter,
    icon: Boolean = false,
    name: String,
    selected: Boolean,
    maximum: Long,
    onClick: () -> Unit = {},
) {
    Column (
        modifier = Modifier
            .clip(MaterialTheme.shapes.small)
            .background(colorScheme.surfaceContainer)
            .clickable { onClick() }
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
                    contentDescription = null
                )
            } else {
                Image(
                    modifier = Modifier.size(imageWidth),
                    painter = painter,
                    contentDescription = null
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
                        fontFamily = classyFont()
                    )
                }
            }
        }
        AnimatedVisibility (
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
