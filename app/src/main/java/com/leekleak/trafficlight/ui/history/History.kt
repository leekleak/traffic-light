package com.leekleak.trafficlight.ui.history

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import com.leekleak.trafficlight.R
import com.leekleak.trafficlight.charts.BarGraph
import com.leekleak.trafficlight.charts.LineGraph
import com.leekleak.trafficlight.database.DayUsage
import com.leekleak.trafficlight.database.HourlyUsageRepo.Companion.dayUsageToBarData
import com.leekleak.trafficlight.model.PreferenceRepo
import com.leekleak.trafficlight.ui.theme.card
import com.leekleak.trafficlight.util.SizeFormatter
import com.leekleak.trafficlight.util.categoryTitle
import com.leekleak.trafficlight.util.getName
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.compose.koinInject
import java.time.LocalDate
import java.time.format.TextStyle

@Composable
fun History(paddingValues: PaddingValues, backStack: NavBackStack<NavKey>) {
    val viewModel: HistoryVM = viewModel()
    val haptic = LocalHapticFeedback.current

    var selected by remember { mutableIntStateOf(-1) }
    val visibleSizes = remember { mutableStateMapOf<Int, Long>(-1 to 0) }
    val maximum by remember { derivedStateOf {
        if (visibleSizes.values.max() > 2) visibleSizes.values.max() else Long.MAX_VALUE
    } }

    val startDate = LocalDate.now()
    val endDate = LocalDate.now()

    val pagerState = rememberPagerState { 10 }

    var appDay by remember { mutableStateOf( LocalDate.now()) }
    LaunchedEffect(pagerState.currentPage) {
        appDay = startDate.minusDays(pagerState.currentPage.toLong())
    }

    val appList by remember(appDay) { viewModel.getAllAppUsage(appDay) }.collectAsState(initial = listOf())
    val appMaximum = appList.maxOfOrNull { it.usage.totalWifi + it.usage.totalCellular } ?: 0
    var appSelected by remember { mutableIntStateOf(-1) }

    LazyColumn(
        modifier = Modifier
            .background(MaterialTheme.colorScheme.surface)
            .fillMaxSize(),
        contentPadding = paddingValues,
        verticalArrangement = Arrangement.spacedBy(6.dp)

    ) {
        categoryTitle(R.string.history)
        item {
            HorizontalPager(pagerState) { page ->
                val date = startDate.minusDays(page.toLong())
                val usageFlow = remember(date) {
                    if (startDate == endDate) {
                        viewModel.hourlyUsageRepo.singleDayUsageFlowBar(date)
                    } else {
                        viewModel.hourlyUsageRepo.daysUsage(date, date)
                    }
                }

                val usage by usageFlow.collectAsState(listOf())
                Column {
                    Text(date.toString())
                    if (usage.isNotEmpty()) {
                        BarGraph(usage)
                    }
                }
            }
        }
        categoryTitle(R.string.app_usage)
        itemsIndexed(appList) { index, item ->
            AppItem(item, index, appSelected, appMaximum) { appSelected = it }
        }
    }
}

@Composable
fun HistoryItem(
    viewModel: HistoryVM,
    visibleSizes: SnapshotStateMap<Int, Long>,
    i: Int,
    selected: Int,
    maximum: Long,
    onClick: (i: Int) -> Unit,
    onAppUsageOpen: (d: LocalDate) -> Unit
) {
    val date = LocalDate.now().minusDays(i.toLong())
    val usageBasic by viewModel.dayUsageBasic(date).collectAsState(DayUsage(totalWifi = 1, totalCellular = 1))
    val totalWifi = usageBasic.totalWifi
    val totalCellular = usageBasic.totalCellular

    DisposableEffect(totalWifi, totalCellular) {
        visibleSizes[i] = totalWifi + totalCellular
        onDispose { visibleSizes.remove(i) }
    }

    Column (Modifier.card()) {
        Box (Modifier.clickable { onClick(if (selected != i) i else -1) }) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Column {
                    Text(
                        modifier = Modifier.width(86.sp.value.dp),
                        text = date.toString().substring(5),
                        autoSize = TextAutoSize.StepBased(8.sp, 26.sp),
                        maxLines = 1,
                        fontFamily = classyFont(),
                        textAlign = TextAlign.Center
                    )
                    AnimatedVisibility(selected == i) {
                        Text(
                            modifier = Modifier.width(86.sp.value.dp),
                            text = date.dayOfWeek.getName(TextStyle.FULL_STANDALONE),
                            autoSize = TextAutoSize.StepBased(8.sp, 18.sp),
                            maxLines = 1,
                            fontFamily = classyFont(),
                            textAlign = TextAlign.Center
                        )
                    }
                }

                AnimatedContent(selected == i) { selected ->
                    if (!selected) {
                        LineGraph(
                            maximum = maximum,
                            data = Pair(totalWifi, totalCellular)
                        )
                    } else {
                        Row (
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(MaterialTheme.shapes.medium)
                                .background(MaterialTheme.colorScheme.background)
                                .padding(8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            DataBadge(
                                iconId = R.drawable.wifi,
                                description = stringResource(R.string.wifi),
                                bgTint = MaterialTheme.colorScheme.primary,
                                tint = MaterialTheme.colorScheme.onPrimary,
                                value = totalWifi
                            )
                            DataBadge(
                                iconId = R.drawable.cellular,
                                description = stringResource(R.string.cellular),
                                bgTint = MaterialTheme.colorScheme.tertiary,
                                tint = MaterialTheme.colorScheme.onTertiary,
                                value = totalCellular
                            )
                        }
                    }
                }
            }
        }
        AnimatedVisibility(
            visible = selected == i,
            enter = expandVertically(spring(0.7f, Spring.StiffnessMedium)),
            exit = shrinkVertically(spring(0.7f, Spring.StiffnessMedium))
        ) {
            val usage by viewModel.dayUsage(date).collectAsState(DayUsage())
            Column {
                Box (modifier = Modifier.padding(4.dp)){
                    BarGraph(dayUsageToBarData(usage))
                }
                Button(
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.fillMaxWidth().padding(start = 4.dp, end = 4.dp),
                    onClick = { onAppUsageOpen(date) },
                ) {
                    Icon(
                        modifier = Modifier.padding(end = 4.dp),
                        painter = painterResource(R.drawable.app),
                        contentDescription = null
                    )
                    Text(text = stringResource(R.string.app_usage))
                }
            }
        }
    }
}

@Composable
fun DataBadge (
    iconId: Int,
    description: String,
    bgTint: Color,
    tint: Color,
    value: Long
) {
    val sizeFormatter = remember { SizeFormatter() }
    Box (modifier = Modifier.clip(MaterialTheme.shapes.small)) {
        Row(
            modifier = Modifier
                .background(bgTint)
                .padding(8.dp),
            horizontalArrangement = Arrangement.spacedBy (4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                painter = painterResource(iconId),
                contentDescription = description,
                tint = tint
            )
            Text(
                text = sizeFormatter.format(value, 1),
                color = tint
            )
        }
    }
}

@Composable
fun classyFont(): FontFamily? {
    val preferenceRepo: PreferenceRepo = koinInject()
    val expressiveFonts by preferenceRepo.expressiveFonts.collectAsState(true)

    return if (expressiveFonts) {
        FontFamily(
            Font(
                R.font.momo_trust_display
            ),
        )
    } else {
        null
    }
}
