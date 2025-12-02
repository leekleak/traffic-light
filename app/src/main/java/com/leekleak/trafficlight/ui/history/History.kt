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
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.leekleak.trafficlight.R
import com.leekleak.trafficlight.charts.BarGraph
import com.leekleak.trafficlight.charts.LineGraph
import com.leekleak.trafficlight.charts.model.BarData
import com.leekleak.trafficlight.database.HourUsage
import com.leekleak.trafficlight.ui.theme.card
import com.leekleak.trafficlight.util.SizeFormatter
import com.leekleak.trafficlight.util.categoryTitle
import com.leekleak.trafficlight.util.categoryTitleSmall
import com.leekleak.trafficlight.util.getName
import com.leekleak.trafficlight.util.padHour
import java.time.LocalDate
import java.time.format.TextStyle

@Composable
fun History(paddingValues: PaddingValues) {
    val viewModel: HistoryVM = viewModel()
    val haptic = LocalHapticFeedback.current

    var selected by remember { mutableIntStateOf(-1) }
    val visibleSizes = remember { mutableStateMapOf<Int, Long>(-1 to 0) }

    LazyColumn(
        modifier = Modifier.background(MaterialTheme.colorScheme.surface).fillMaxSize(),
        contentPadding = paddingValues
    ) {
        categoryTitle(R.string.history)
        if (LocalDate.now().dayOfMonth != 1) {
            categoryTitleSmall(LocalDate.now().month.getName(TextStyle.FULL_STANDALONE))
        }
        for (index in 0..90) {
            val day = LocalDate.now().minusDays(index.toLong())
            if (day.dayOfMonth == 1) {
                categoryTitleSmall(day.month.minus(1L).getName(TextStyle.FULL_STANDALONE))
            }
            item {
                Box(Modifier.padding(bottom = 6.dp)) {
                    HistoryItem(viewModel, visibleSizes, index + 1, selected) { i: Int ->
                        selected = i
                        haptic.performHapticFeedback(HapticFeedbackType.ContextClick)
                    }
                }
            }
        }
    }
}

@Composable
fun HistoryItem(
    viewModel: HistoryVM,
    visibleSizes: SnapshotStateMap<Int, Long>,
    i: Int,
    selected: Int,
    onClick: (i: Int) -> Unit
) {
    val date = LocalDate.now().minusDays(i.toLong())
    val usageBasic = viewModel.dayUsageBasic(date)
    val totalWifi = usageBasic.totalWifi
    val totalCellular = usageBasic.totalCellular

    DisposableEffect(Unit) {
        visibleSizes[i] = totalWifi + totalCellular
        onDispose { visibleSizes.remove(i) }
    }

    val maximum by remember(visibleSizes) {
        derivedStateOf { visibleSizes.maxOf { it.value } }
    }

    Column (Modifier.card()) {
        Box (
            modifier = Modifier
                .clip(MaterialTheme.shapes.large)
                .clickable { onClick(if (selected != i) i else -1) },
        ) {
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
            val usage = viewModel.dayUsage(date).hours.map { it.value }
            Box(modifier = Modifier.padding(4.dp)) {
                BarGraph(dayUsageToBarData(usage.map { it.toHourUsage() }))
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

fun dayUsageToBarData(hours: List<HourUsage>): List<BarData> {
    val data: MutableList<BarData> = mutableListOf()
    for (i in 0..22 step 2) {
        data.add(BarData(padHour(i), 0.0, 0.0))
    }
    if (hours.isNotEmpty()) {
        for (i in 0..<12) {
            data[i] = BarData(
                padHour(i * 2),
                hours[i].totalCellular.toDouble(),
                hours[i].totalWifi.toDouble()
            )
        }
    }
    return data
}

fun classyFont(): FontFamily =
    FontFamily(
        Font(
            R.font.momo_trust_display
        ),
    )
