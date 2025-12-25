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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ButtonGroup
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import com.leekleak.trafficlight.charts.LineGraph
import com.leekleak.trafficlight.charts.ScrollableBarGraph
import com.leekleak.trafficlight.charts.model.ScrollableBarData
import com.leekleak.trafficlight.database.AppUsage
import com.leekleak.trafficlight.database.HourlyUsageRepo
import com.leekleak.trafficlight.model.PreferenceRepo
import com.leekleak.trafficlight.ui.theme.card
import com.leekleak.trafficlight.util.CategoryTitleText
import com.leekleak.trafficlight.util.SizeFormatter
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
    val haptic = LocalHapticFeedback.current

    var appDay by remember { mutableStateOf( LocalDate.now()) }
    var showMonth by remember { mutableStateOf(false) }

    val appList by remember(appDay, showMonth) {
        if (!showMonth) {
            hourlyUsageRepo.getAllAppUsage(appDay, appDay)
        } else {
            val start = appDay.minusDays(appDay.dayOfMonth.toLong()-1L)
            val end = start.plusMonths(1)
            hourlyUsageRepo.getAllAppUsage(start, end)
        }
    }.collectAsState(listOf())
    val appMaximum = appList.maxOfOrNull { it.usage.totalWifi + it.usage.totalCellular } ?: 0
    var appSelected by remember { mutableIntStateOf(-1) }

    val days = remember { getDatesForTimespan() }
    val usageFlow = remember { hourlyUsageRepo.daysUsage(days.first, days.second) }

    LazyColumn(
        modifier = Modifier
            .background(MaterialTheme.colorScheme.surface)
            .statusBarsPadding()
            .fillMaxSize(),
        contentPadding = paddingValues,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        stickyHeader {
            Column {
                Column (Modifier.background(MaterialTheme.colorScheme.background)) {
                    CategoryTitleText(stringResource(R.string.history))
                    Box(
                        modifier = Modifier
                            .card()
                            .padding(6.dp)
                            .clip(MaterialTheme.shapes.medium)
                    ) {
                        val usage: List<ScrollableBarData> by usageFlow.collectAsState(List(MAX_DAYS) {
                            ScrollableBarData(
                                LocalDate.now()
                            )
                        })
                        Box(
                            modifier = Modifier
                                .clip(MaterialTheme.shapes.medium)
                                .background(MaterialTheme.colorScheme.background)
                        ) {
                            ScrollableBarGraph(usage) {
                                appDay = days.first.plusDays(it.toLong())
                            }
                        }
                    }
                    Row(Modifier.fillMaxWidth()) {
                        CategoryTitleText(stringResource(R.string.app_usage))
                        ButtonGroup(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.End),
                            expandedRatio = 0f, // TODO: Remove on a day google fixes compose.
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
                }
            }
        }
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

fun getDatesForTimespan(): Pair<LocalDate, LocalDate> {
    val now = LocalDate.now()
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
