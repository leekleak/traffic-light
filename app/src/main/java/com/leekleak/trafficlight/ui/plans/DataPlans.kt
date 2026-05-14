package com.leekleak.trafficlight.ui.plans

import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.snapping.SnapPosition
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PageSize
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.leekleak.trafficlight.R
import com.leekleak.trafficlight.charts.AppGraph
import com.leekleak.trafficlight.charts.BarGraph
import com.leekleak.trafficlight.database.AppPreferenceRepo
import com.leekleak.trafficlight.database.AppUsage
import com.leekleak.trafficlight.database.DataPlanDao
import com.leekleak.trafficlight.ui.navigation.Navigator
import com.leekleak.trafficlight.ui.navigation.PlanConfigKey
import com.leekleak.trafficlight.ui.theme.card
import com.leekleak.trafficlight.util.CategoryTitleText
import com.leekleak.trafficlight.util.DataSize
import com.leekleak.trafficlight.util.MiniCard
import com.leekleak.trafficlight.util.MiniCardState
import com.leekleak.trafficlight.util.PageTitle
import com.leekleak.trafficlight.util.openLink
import com.leekleak.trafficlight.util.shelfShape
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject

@Composable
fun DataPlans(
    paddingValues: PaddingValues,
) {
    val viewModel: DataPlansVM = koinViewModel()
    val navigator: Navigator = koinInject()

    LifecycleResumeEffect(Unit) {
        viewModel.refresh()
        onPauseOrDispose {}
    }

    val paddingSide = paddingValues.calculateLeftPadding(LayoutDirection.Ltr)
    val paddingTop = paddingValues.calculateTopPadding()
    val paddingBottom = paddingValues.calculateBottomPadding()
    val listContentPadding = PaddingValues(paddingSide, 0.dp, paddingSide, paddingBottom)

    PageTitle(false, null, stringResource(R.string.data_plans))

    Column(
        modifier = Modifier.padding(top = paddingTop),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        DataPlanPager(paddingSide + 8.dp, navigator)
        DataPlanInsights(listContentPadding)
    }
}

@Composable
private fun DataPlanPager(
    horizontalPadding: Dp,
    navigator: Navigator,
) {
    val dataPlanDao: DataPlanDao = koinInject()
    val appPreferenceRepo: AppPreferenceRepo = koinInject()
    val viewModel: DataPlansVM = koinViewModel()

    val activity = LocalActivity.current
    val scope = rememberCoroutineScope()

    val activePlans by remember { dataPlanDao.getActivePlansFlow() }.collectAsState(listOf())
    val shizukuHint by remember { appPreferenceRepo.shizukuHint }.collectAsState(false)
    val shizukuTracking by remember { appPreferenceRepo.shizukuTracking }.collectAsState(true)

    val pagerState = rememberPagerState(pageCount = {
        activePlans.size + if (shizukuHint && !shizukuTracking) 1 else 0
    })

    LaunchedEffect(activePlans.size, pagerState.currentPage) {
        viewModel.selectDataPlan(
            if (pagerState.currentPage < activePlans.size) {
                activePlans[pagerState.currentPage]
            } else {
                null
            }
        )
    }

    HorizontalPager(
        state = pagerState,
        contentPadding = PaddingValues(vertical = 8.dp, horizontal = horizontalPadding),
        pageSpacing = horizontalPadding / 2,
        snapPosition = SnapPosition.Center,
        pageSize = PageSize.Fill
    ) { page ->
        if (page < activePlans.size) {
            val plan = activePlans[page]
            if (plan.dataMax != 0L) {
                ConfiguredDataPlan(plan) {
                    navigator.goTo(PlanConfigKey(plan))
                }
            } else {
                UnconfiguredDataPlan(plan, true) {
                    navigator.goTo(PlanConfigKey(plan))
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .height(200.dp)
                    .card()
                    .padding(16.dp),
                horizontalAlignment = Alignment.End
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(painterResource(R.drawable.warning), null)
                    Text(modifier = Modifier.fillMaxWidth(), fontWeight = FontWeight.Bold, text = stringResource(R.string.shizuku_hint))
                }
                Text(modifier = Modifier.fillMaxWidth(), text = stringResource(R.string.shizuku_hint_description))
                Row(
                    modifier = Modifier.fillMaxHeight(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.Bottom
                ) {
                    Button(
                        colors = ButtonDefaults.filledTonalButtonColors(),
                        shape = MaterialTheme.shapes.large,
                        onClick = {
                            openLink(
                                activity,
                                "https://github.com/leekleak/traffic-light/wiki/Setting-up-Shizuku-for-multi%E2%80%90SIM-tracking"
                            )
                        },
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Icon(painterResource(R.drawable.help), null)
                            Text(stringResource(R.string.help))
                        }
                    }
                    Button(
                        onClick = { scope.launch { appPreferenceRepo.setShizukuHint(false) } }
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Icon(painterResource(R.drawable.close), null)
                            Text(stringResource(R.string.close))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DataPlanInsights(contentPadding: PaddingValues) {
    val viewModel: DataPlansVM = koinViewModel()
    val dataPlan by viewModel.selectedDataPlan.collectAsState(null)
    val topAppsList by viewModel.topApps.collectAsState()
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
        item{}
        dataPlan?.let { plan ->
            if (plan.dataMax > 0) usageInsights()
            thisWeek()
            if (plan.dataMax > 0) budgetInsights()
            if (topAppsList.isNotEmpty()) topApps(topAppsList)
        }
    }
}

private fun LazyListScope.usageInsights() {
    item(key = "usage") {
        Column(Modifier.animateItem()) {
            CategoryTitleText(stringResource(R.string.usage))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                val viewModel: DataPlansVM = koinViewModel()
                val dataSafety by viewModel.dataSafety.collectAsState()
                MiniCard(
                    state = dataSafety,
                    baseColor = colorScheme.surface,
                    icon = painterResource(R.drawable.shield),
                    title = stringResource(R.string.safety)
                ) { fontFamily ->
                    Text(
                        text = when (dataSafety) {
                            MiniCardState.POSITIVE -> stringResource(R.string.safe)
                            MiniCardState.NEUTRAL -> stringResource(R.string.neutral)
                            MiniCardState.NEGATIVE -> stringResource(R.string.unsafe)
                        },
                        fontFamily = fontFamily,
                        fontSize = 24.sp
                    )
                }

                val trend by viewModel.trend.collectAsState()
                val state = when {
                    trend > 50 -> MiniCardState.NEGATIVE
                    trend < -25 -> MiniCardState.POSITIVE
                    else -> MiniCardState.NEUTRAL
                }
                MiniCard(
                    state = state,
                    baseColor = colorScheme.surface,
                    icon = when (state) {
                        MiniCardState.NEGATIVE -> painterResource(R.drawable.trending_up)
                        MiniCardState.POSITIVE -> painterResource(R.drawable.trending_down)
                        MiniCardState.NEUTRAL -> painterResource(R.drawable.trending_flat)
                    },
                    title = stringResource(R.string.trend)
                ) { fontFamily ->
                    Text(
                        text = if (trend < 1000) "%+d%%".format(trend.toInt())
                               else stringResource(R.string.very_big),
                        fontFamily = fontFamily,
                        fontSize = 24.sp
                    )
                }
            }
        }
    }
}

private fun LazyListScope.budgetInsights() {
    item(key = "budget") {
        Column(Modifier.animateItem()) {
            CategoryTitleText(stringResource(R.string.budget))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                val viewModel: DataPlansVM = koinViewModel()
                val todayBudget by viewModel.todayBudget.collectAsState()
                MiniCard(
                    state = MiniCardState.NEUTRAL,
                    baseColor = colorScheme.surface,
                    icon = painterResource(R.drawable.today),
                    title = stringResource(R.string.today)
                ) { fontFamily ->
                    val string by remember { derivedStateOf { DataSize(todayBudget).toStringParts() } }
                    Text(
                        modifier = Modifier.fillMaxWidth(),
                        fontFamily = fontFamily,
                        text = buildAnnotatedString {
                            withStyle(style = SpanStyle(fontSize = 24.sp)) {
                                append("${string.first}${string.second}")
                            }
                            withStyle(style = SpanStyle(fontSize = 20.sp)) {
                                append(string.third)
                            }
                        }
                    )
                }

                val remainingDailyBudget by viewModel.remainingDailyBudget.collectAsState()
                MiniCard(
                    state = MiniCardState.NEUTRAL,
                    baseColor = colorScheme.surface,
                    icon = painterResource(R.drawable.calendar_month),
                    title = stringResource(R.string.daily)
                ) { fontFamily ->
                    val string by remember { derivedStateOf { DataSize(remainingDailyBudget).toStringParts() } }
                    Text(
                        modifier = Modifier.fillMaxWidth(),
                        fontFamily = fontFamily,
                        text = buildAnnotatedString {
                            withStyle(style = SpanStyle(fontSize = 24.sp)) {
                                append("${string.first}${string.second}")
                            }
                            withStyle(style = SpanStyle(fontSize = 20.sp)) {
                                append(string.third)
                            }
                        }
                    )
                }
            }
        }
    }
}

private fun LazyListScope.thisWeek() {
    item(key = "this_week") {
        Column(Modifier.animateItem()) {
            val viewModel: DataPlansVM = koinViewModel()
            val weekUsage by viewModel.weekUsage.collectAsState()
            CategoryTitleText(stringResource(R.string.this_week))
            Box(
                modifier = Modifier
                    .clip(MaterialTheme.shapes.medium)
                    .background(colorScheme.surface)
                    .padding(6.dp)
            ) {
                BarGraph(
                    data = weekUsage,
                    showLegend = false,
                    centerLabels = true
                )
            }
        }
    }
}

private fun LazyListScope.topApps(topAppsList: List<AppUsage>) {
    item(key = "top_apps") {
        Column(Modifier.animateItem()) {
            CategoryTitleText(stringResource(R.string.top_apps))
            Box(
                modifier = Modifier
                    .clip(MaterialTheme.shapes.medium)
                    .background(colorScheme.surface)
                    .padding(6.dp)
            ) {
                AppGraph(topAppsList)
            }
        }
    }
}