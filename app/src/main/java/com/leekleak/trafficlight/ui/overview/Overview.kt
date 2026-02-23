package com.leekleak.trafficlight.ui.overview

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import com.leekleak.trafficlight.R
import com.leekleak.trafficlight.charts.BarGraph
import com.leekleak.trafficlight.charts.model.BarData
import com.leekleak.trafficlight.database.DataPlanDao
import com.leekleak.trafficlight.database.DayUsage
import com.leekleak.trafficlight.database.HourlyUsageRepo
import com.leekleak.trafficlight.database.HourlyUsageRepo.Companion.dayUsageToBarData
import com.leekleak.trafficlight.model.PreferenceRepo
import com.leekleak.trafficlight.services.PermissionManager
import com.leekleak.trafficlight.services.UsageService
import com.leekleak.trafficlight.ui.navigation.PlanConfig
import com.leekleak.trafficlight.ui.theme.card
import com.leekleak.trafficlight.util.DataSize
import com.leekleak.trafficlight.util.categoryTitle
import com.leekleak.trafficlight.widget.Warning
import kotlinx.coroutines.delay
import org.koin.compose.koinInject
import java.time.LocalDate

@Composable
fun Overview(
    paddingValues: PaddingValues,
    backStack: NavBackStack<NavKey>
) {
    val viewModel: OverviewVM = viewModel()

    val todayUsage by viewModel.todayUsage.collectAsState(DayUsage())
    val weeklyUsage by viewModel.hourlyUsageRepo.weekUsage().collectAsState(listOf())

    val permissionManager: PermissionManager = koinInject()
    val shizukuPermission by permissionManager.shizukuPermissionFlow.collectAsState(false)

    val dataPlanDao: DataPlanDao = koinInject()

    val activePlans = remember { dataPlanDao.getActive() }

    /**
     * Generally the notification service is responsible for updating daily usage,
     * however some people may prefer to use the app exclusively for historical data tracking
     * so if the notification is disabled the app should still periodically update the usage.
     */
    val hourlyUsageRepo: HourlyUsageRepo = koinInject()
    val preferenceRepo: PreferenceRepo = koinInject()
    val notification by preferenceRepo.notification.collectAsState(true)
    LaunchedEffect(notification) {
        if (!notification) {
            while (true) {
                UsageService.todayUsage = hourlyUsageRepo.singleDayUsage(LocalDate.now())
                delay(5000)
            }
        }
    }

    val columnState = rememberLazyListState()
    LazyColumn(
        modifier = Modifier
            .background(MaterialTheme.colorScheme.surface)
            .fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = paddingValues,
        state = columnState
    ) {
        categoryTitle { stringResource(R.string.data_plans) }
        if (!shizukuPermission && activePlans.isEmpty()) {
            item {
                Warning(
                    title = stringResource(R.string.shizuku_required),
                    description = stringResource(R.string.shizuku_required_description),
                )
            }
        }

        items(activePlans, {it.subscriberID}) {
            if (it.dataMax != 0L) {
                ConfiguredDataPlan(it) {
                    backStack.add(PlanConfig(it.subscriberID))
                }
            } else {
                UnconfiguredDataPlan(it) {
                    backStack.add(PlanConfig(it.subscriberID))
                }
            }
        }

        overviewTab(
            label = R.string.today,
            data = dayUsageToBarData(todayUsage),
        )

        if (weeklyUsage.isNotEmpty()) {
            overviewTab(
                label = R.string.this_week,
                data = weeklyUsage,
                finalGridPoint = "",
                centerLabels = true
            )
        }
    }
}

fun LazyListScope.overviewTab(
    label: Int,
    data: List<BarData>,
    finalGridPoint: String = "24",
    centerLabels: Boolean = false
) {
    val cellular = data.sumOf { it.y1 }.toLong()
    val wifi = data.sumOf { it.y2 }.toLong()
    categoryTitle { stringResource(label) }
    item {
        Column (verticalArrangement = Arrangement.spacedBy(8.dp)){
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)
            ) {
                SummaryItem(
                    painter = painterResource(R.drawable.wifi),
                    contentDescription = stringResource(R.string.wifi),
                    tint = MaterialTheme.colorScheme.primary,
                    data = { wifi }
                )
                SummaryItem(
                    painter = painterResource(R.drawable.cellular),
                    contentDescription = stringResource(R.string.cellular),
                    tint = MaterialTheme.colorScheme.tertiary,
                    data = { cellular }
                )
            }
            Box(
                modifier = Modifier
                    .card()
                    .padding(6.dp)
            ) {
                BarGraph(
                    data = data,
                    finalGridPoint = finalGridPoint,
                    centerLabels = centerLabels
                )
            }
        }
    }
}

@Composable
fun RowScope.SummaryItem(
    painter: Painter,
    contentDescription: String,
    tint: Color,
    data: () -> Long
) {
    val animation = remember { Animatable(0f) }
    val haptic = LocalHapticFeedback.current
    Row (
        modifier = Modifier
            .weight(1f + animation.value / 5f)
            .card()
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        haptic.performHapticFeedback(HapticFeedbackType.GestureThresholdActivate)
                        animation.animateTo(
                            1f,
                            spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMedium)
                        )
                        tryAwaitRelease()
                        haptic.performHapticFeedback(HapticFeedbackType.GestureEnd)
                        animation.animateTo(0f)
                    },
                )
            },
        horizontalArrangement = Arrangement.Center,
    ) {
        val text = DataSize(value = data().toDouble(), precision = 2).toStringParts()
        val viewModel: OverviewVM = viewModel()
        val expressiveFonts by viewModel.preferenceRepo.expressiveFonts.collectAsState(true)
        Text(
            fontSize = if (expressiveFonts) 64.sp else 48.sp,
            text = text[0],
            fontFamily = chonkyFont(animation.value),
            color = tint
        )
        Column (
            modifier = Modifier
                .fillMaxHeight()
                .padding(top = 3.dp, start = 2.dp),
            verticalArrangement = Arrangement.spacedBy((-8).dp)
        ) {
            Text(
                fontSize = if (expressiveFonts) 36.sp else 28.sp,
                text = "." + text[1].padEnd(2, '0'),
                maxLines = 1,
                fontFamily = chonkyFont(animation.value),
                color = tint,
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    fontSize = if (expressiveFonts) 22.sp else 18.sp,
                    text = text[2],
                    fontFamily = chonkyFont(animation.value),
                    color = tint,
                )
                Icon(
                    painter = painter,
                    contentDescription = contentDescription,
                    tint = tint,
                )
            }
        }
    }
}

@OptIn(ExperimentalTextApi::class)
@Composable
fun chonkyFont(animation: Float): FontFamily? {
    val viewModel: OverviewVM = viewModel()
    val expressiveFonts by viewModel.preferenceRepo.expressiveFonts.collectAsState(true)

    return if (expressiveFonts) {
        FontFamily(
            Font(
                R.font.jaro,
                variationSettings = FontVariation.Settings(
                    FontVariation.Setting("opsz", 72f - animation * 60f)
                )
            ),
        )
    } else {
        null
    }
}
