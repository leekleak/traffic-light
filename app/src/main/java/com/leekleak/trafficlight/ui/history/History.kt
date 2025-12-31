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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ButtonGroup
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.viewmodel.compose.viewModel
import com.leekleak.trafficlight.R
import com.leekleak.trafficlight.charts.LineGraph
import com.leekleak.trafficlight.charts.ScrollableBarGraph
import com.leekleak.trafficlight.charts.classyFont
import com.leekleak.trafficlight.charts.model.ScrollableBarData
import com.leekleak.trafficlight.database.AppUsage
import com.leekleak.trafficlight.database.HourlyUsageRepo
import com.leekleak.trafficlight.ui.theme.card
import com.leekleak.trafficlight.util.CategoryTitleText
import com.leekleak.trafficlight.util.getName
import com.leekleak.trafficlight.util.px
import org.koin.compose.koinInject
import java.time.LocalDate
import java.time.format.TextStyle
import kotlin.math.roundToInt

const val MAX_DAYS = 96

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun History(paddingValues: PaddingValues) {
    val hourlyUsageRepo: HourlyUsageRepo = koinInject()
    val viewModel: HistoryVM = viewModel()
    val haptic = LocalHapticFeedback.current

    var appDay by remember { mutableStateOf( LocalDate.now()) }
    var showMonth by remember { mutableStateOf(false) }

    LaunchedEffect(appDay, showMonth) {
        viewModel.updateQuery(appDay, showMonth)
    }

    val appList by viewModel.appList.collectAsState()
    val appMaximum = appList.maxOfOrNull { it.usage.totalWifi + it.usage.totalCellular } ?: 0
    var appSelected by remember { mutableIntStateOf(-1) }

    val days = remember { getDatesForTimespan() }
    val usageFlow = remember { hourlyUsageRepo.daysUsage(days.first, days.second) }

    Column (
        modifier = Modifier
            .padding(
                start = paddingValues.calculateLeftPadding(LayoutDirection.Ltr),
                end = paddingValues.calculateLeftPadding(LayoutDirection.Ltr),
            )
            .statusBarsPadding()
    ) {
        CategoryTitleText(stringResource(R.string.history))
        Box(
            modifier = Modifier
                .card()
                .padding(6.dp)
                .clip(MaterialTheme.shapes.medium)
        ) {
            val usage: List<ScrollableBarData> by usageFlow.collectAsState(List(MAX_DAYS) {
                ScrollableBarData(LocalDate.now())
            })
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
                horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.End),
                expandedRatio = 0f, // TODO: Remove the day google fixes compose.
                overflowIndicator = {}
            ) {
                toggleableItem(
                    onCheckedChange = {showMonth = true},
                    label = appDay.month.getName(TextStyle.FULL),
                    checked = showMonth
                )
                toggleableItem(
                    onCheckedChange = {showMonth = false},
                    label = appDay.dayOfMonth.toString(),
                    checked = !showMonth
                )
            }
        }
        LazyColumn(
            modifier = Modifier
                .background(colorScheme.surface)
                .fillMaxSize(),
            contentPadding = PaddingValues(0.dp, 0.dp, 0.dp, paddingValues.calculateBottomPadding()),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            item("scroll holder") { }
            items(appList, { it.name }) { item ->
                Box(Modifier.animateItem()) {
                    AppItem(item, item.uid == appSelected, appMaximum) {
                        appSelected = if (appSelected != item.uid) item.uid else -1
                        haptic.performHapticFeedback(HapticFeedbackType.ContextClick)
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
    appUsage: AppUsage,
    selected: Boolean,
    maximum: Long,
    onClick: () -> Unit,
) {
    val context = LocalContext.current

    val totalWifi = appUsage.usage.totalWifi
    val totalCellular = appUsage.usage.totalCellular

    val imageWidth = 32.dp

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
            val icon = context.packageManager.getApplicationIcon(appUsage.packageName)
            Image(
                bitmap = icon.toBitmap(imageWidth.px.roundToInt(), imageWidth.px.roundToInt()).asImageBitmap(),
                contentDescription = null
            )

            AnimatedContent(selected) { selected ->
                if (!selected) {
                    LineGraph(
                        maximum = maximum,
                        data = Pair(totalWifi, totalCellular)
                    )
                } else {
                    Text(
                        text = appUsage.name,
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
                LineGraph(
                    maximum = maximum,
                    data = Pair(totalWifi, totalCellular)
                )
            }
        }
    }
}
