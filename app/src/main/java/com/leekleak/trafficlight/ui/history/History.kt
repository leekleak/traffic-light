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
import androidx.compose.foundation.lazy.items
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
import com.leekleak.trafficlight.R
import com.leekleak.trafficlight.charts.BarGraph
import com.leekleak.trafficlight.charts.LineGraph
import com.leekleak.trafficlight.charts.model.BarData
import com.leekleak.trafficlight.database.AppUsage
import com.leekleak.trafficlight.database.HourlyUsageRepo
import com.leekleak.trafficlight.model.PreferenceRepo
import com.leekleak.trafficlight.ui.history.TimeSpan.Day
import com.leekleak.trafficlight.ui.history.TimeSpan.Month
import com.leekleak.trafficlight.ui.history.TimeSpan.Week
import com.leekleak.trafficlight.ui.theme.card
import com.leekleak.trafficlight.util.SizeFormatter
import com.leekleak.trafficlight.util.categoryTitle
import com.leekleak.trafficlight.util.px
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlin.math.roundToInt

enum class TimeSpan {
    Day,
    Week,
    Month;

    fun getDays(): Int {
        return when (this) {
            Day -> 0
            Week -> 7
            Month -> 30
        }
    }

    // Is it fine to hard-code this? No. Can I be bothered to calculate this procedurally? No.
    fun getPages(): Int {
        return when (this) {
            Day -> 96
            Week -> 14
            Month -> 4
        }
    }
}

@Composable
fun History(paddingValues: PaddingValues) {
    val hourlyUsageRepo: HourlyUsageRepo = koinInject()
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()

    var timespan by remember { mutableStateOf(Day) }
    var appDay by remember { mutableStateOf( LocalDate.now()) }
    var appDay2 by remember { mutableStateOf( LocalDate.now()) }

    val pagerState = rememberPagerState { timespan.getPages() }

    LaunchedEffect(pagerState.currentPage, timespan) {
        val days = getDatesForTimespan(timespan, pagerState.currentPage.toLong())
        appDay = days.first
        appDay2 = days.second
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
                        timespan = if (timespan == Day) Week else Month
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
                val days = remember(timespan) { getDatesForTimespan(timespan, page.toLong()) }
                val usageFlow = remember(timespan) {
                    if (days.first == days.second) {
                        hourlyUsageRepo.singleDayUsageFlowBar(days.first)
                    } else {
                        hourlyUsageRepo.daysUsage(days.first, days.second)
                    }
                }

                val usage: List<BarData> by usageFlow.collectAsState(List(timespan.getDays()) { BarData() })
                BarGraph(
                    data = usage,
                    finalGridPoint = if (timespan == Day) "24" else "",
                    centerLabels = timespan != Day
                ) { index ->
                    when (timespan) {
                        Day -> return@BarGraph
                        Week -> {
                            timespan = Day
                            val n = ChronoUnit.DAYS.between(
                                days.first.plusDays(index.toLong()),
                                LocalDate.now()
                            ).toInt()
                            scope.launch { pagerState.scrollToPage(n) }
                        }
                        Month -> {
                            timespan = Week
                            val n = ChronoUnit.DAYS.between(
                                days.first.plusDays(index.toLong()),
                                LocalDate.now()
                            ).toInt()
                            scope.launch { pagerState.scrollToPage(n/7) }
                        }
                    }
                }
            }
        }
        categoryTitle(R.string.app_usage)
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

fun getDatesForTimespan(span: TimeSpan, page: Long): Pair<LocalDate, LocalDate> {
    val now = LocalDate.now()
    val pair = when (span) {
        Day -> {
            val base = now.minusDays(page)
            Pair(base, base)
        }
        Week -> {
            val base = now.minusDays(now.dayOfWeek.value.toLong()-1).minusWeeks(page)
            Pair(base, base.plusWeeks(1))
        }
        Month -> {
            val base = now.minusDays(Day.getPages().toLong())
            Pair(base, now)
        }
    }
    return pair
}

@Composable
fun AppItem(
    appUsage: AppUsage,
    selected: Boolean,
    maximum: Long,
    onClick: () -> Unit,
) {
    val totalWifi = appUsage.usage.totalWifi
    val totalCellular = appUsage.usage.totalCellular

    Column (Modifier.card()) {
        Column (Modifier.clickable { onClick() }) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                val width = 32.dp.px.roundToInt()
                val bitmap = appUsage.drawable?.toBitmap(width, width)
                bitmap?.let { Image(bitmap = it.asImageBitmap(), contentDescription = null) }

                AnimatedContent(selected) { selected ->
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
                visible = selected,
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
