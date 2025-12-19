package com.leekleak.trafficlight.ui.history

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.viewmodel.compose.viewModel
import com.leekleak.trafficlight.R
import com.leekleak.trafficlight.charts.BarGraph
import com.leekleak.trafficlight.charts.LineGraph
import com.leekleak.trafficlight.database.AppUsage
import com.leekleak.trafficlight.database.HourlyUsageRepo
import com.leekleak.trafficlight.model.PreferenceRepo
import com.leekleak.trafficlight.ui.theme.card
import com.leekleak.trafficlight.util.SizeFormatter
import com.leekleak.trafficlight.util.categoryTitle
import com.leekleak.trafficlight.util.px
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import java.time.LocalDate
import kotlin.math.roundToInt

enum class TimeSpan {
    Day,
    Week,
    Month;

    fun getDays(): Long {
        return when (this) {
            Day -> 0
            Week -> 7
            Month -> 30
        }
    }
}

@Composable
fun History(paddingValues: PaddingValues) {
    val hourlyUsageRepo: HourlyUsageRepo = koinInject()
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()

    val startDate = LocalDate.now()

    val pagerState = rememberPagerState { 10 }

    var timespan by remember { mutableStateOf(TimeSpan.Day) }
    var appDay by remember { mutableStateOf( LocalDate.now()) }
    var appDay2 by remember { mutableStateOf( LocalDate.now()) }
    LaunchedEffect(pagerState.currentPage, timespan) {
        when (timespan) {
            TimeSpan.Day -> {
                appDay = startDate.minusDays(pagerState.currentPage.toLong())
                appDay2 = appDay
            }
            TimeSpan.Week -> {
                appDay = startDate.minusDays(startDate.dayOfWeek.value.toLong()-1).minusWeeks(pagerState.currentPage.toLong())
                appDay2 = appDay.plusWeeks(1)
            }
            TimeSpan.Month -> {
                appDay = startDate.minusDays(startDate.dayOfMonth.toLong()-1).minusMonths(pagerState.currentPage.toLong())
                appDay2 = appDay.plusMonths(1)
            }
        }
    }

    val appList by remember(appDay, appDay2) { hourlyUsageRepo.getAllAppUsage(appDay, appDay2) }.collectAsState(initial = listOf())
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
            Row {
                Button(
                    onClick = {
                        timespan = if (timespan == TimeSpan.Day) TimeSpan.Week else TimeSpan.Month
                        scope.launch {
                            pagerState.scrollToPage(0)
                        }
                    }
                ) { Text("Back") }
                AnimatedContent(appDay) {
                    Text(it.toString())
                }
            }
        }
        item {
            HorizontalPager(
                modifier = Modifier
                    .card()
                    .padding(6.dp)
                    .clip(MaterialTheme.shapes.medium)
                    .background(MaterialTheme.colorScheme.background),
                state = pagerState,
                reverseLayout = true
            ) { page ->
                val usageFlow = remember(appDay, appDay2, timespan) {
                    if (appDay == appDay2) {
                        hourlyUsageRepo.singleDayUsageFlowBar(appDay)
                    } else {
                        hourlyUsageRepo.daysUsage(appDay, appDay2)
                    }
                }

                val usage by usageFlow.collectAsState(listOf())
                if (usage.isNotEmpty()) {
                    BarGraph(
                        data = usage,
                        finalGridPoint = if (timespan == TimeSpan.Day) "24" else "",
                        centerLabels = timespan != TimeSpan.Day
                    )
                }
            }
        }
        categoryTitle(R.string.app_usage)
        itemsIndexed(appList) { index, item ->
            AppItem(item, index, appSelected, appMaximum) {
                appSelected = it
                haptic.performHapticFeedback(HapticFeedbackType.ContextClick)
            }
        }
    }
}

@Composable
fun AppItem(
    appUsage: AppUsage,
    i: Int,
    selected: Int,
    maximum: Long,
    onClick: (i: Int) -> Unit,
) {
    val totalWifi = appUsage.usage.totalWifi
    val totalCellular = appUsage.usage.totalCellular

    Column (Modifier.card()) {
        Column (Modifier.clickable { onClick(if (selected != i) i else -1) }) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                val width = 32.dp.px.roundToInt()
                val bitmap = appUsage.drawable?.toBitmap(width, width)
                bitmap?.let { Image(bitmap = it.asImageBitmap(), contentDescription = null) }

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
            AnimatedVisibility (
                visible = selected == i,
                enter = expandVertically(spring(0.7f, Spring.StiffnessMedium)),
                exit = shrinkVertically(spring(0.7f, Spring.StiffnessMedium))
            ) {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        textAlign = TextAlign.Center,
                        text = appUsage.name + "\n" + appUsage.appInfo.packageName
                    )
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
