package com.leekleak.trafficlight.ui.plans

import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.snapping.SnapPosition
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
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
import com.leekleak.trafficlight.charts.ExtraGraph
import com.leekleak.trafficlight.charts.model.BarData
import com.leekleak.trafficlight.database.AppUsage
import com.leekleak.trafficlight.database.DataPlan
import com.leekleak.trafficlight.database.DataPlanSnapshot
import com.leekleak.trafficlight.integrations.Ad
import com.leekleak.trafficlight.integrations.AdType
import com.leekleak.trafficlight.ui.components.BackAction
import com.leekleak.trafficlight.ui.components.HazeScaffold
import com.leekleak.trafficlight.ui.navigation.NAVBAR_PADDING
import com.leekleak.trafficlight.ui.settings.InfoCard
import com.leekleak.trafficlight.ui.theme.LocalSizeMetric
import com.leekleak.trafficlight.ui.theme.card
import com.leekleak.trafficlight.util.CategoryTitleText
import com.leekleak.trafficlight.util.DataSize
import com.leekleak.trafficlight.util.MiniCard
import com.leekleak.trafficlight.util.MiniCardState
import com.leekleak.trafficlight.util.TrendCard
import com.leekleak.trafficlight.util.openLink
import com.leekleak.trafficlight.util.shelfShape

@Composable
fun DataPlans(
    uiState: DataPlansUiState,
    selectDataPlan: (DataPlan?) -> Unit,
    disableShizukuHint: () -> Unit,
    goToPlanConfig: (DataPlan) -> Unit,
    refresh: () -> Unit,
) {
    LifecycleResumeEffect(Unit) {
        refresh()
        onPauseOrDispose {}
    }

    HazeScaffold(
        title = stringResource(R.string.data_plans),
        hazeState = null,
        scrollState = null,
        backAction = BackAction.None,
        extraPadding = PaddingValues(bottom = NAVBAR_PADDING),
    ) { paddingValues ->
        val paddingSide = paddingValues.calculateLeftPadding(LayoutDirection.Ltr)
        val paddingTop = paddingValues.calculateTopPadding()
        val paddingBottom = paddingValues.calculateBottomPadding()
        val listContentPadding = PaddingValues(paddingSide, 0.dp, paddingSide, paddingBottom)
        Spacer(Modifier.height(paddingTop))
        DataPlanPager(
            horizontalPadding = paddingSide + 8.dp,
            uiState = uiState,
            selectDataPlan = selectDataPlan,
            disableShizukuHint = disableShizukuHint,
            goToPlanConfig = goToPlanConfig
        )
        DataPlanInsights(listContentPadding, uiState)
    }
}

@Composable
private fun DataPlanPager(
    horizontalPadding: Dp,
    uiState: DataPlansUiState,
    selectDataPlan: (DataPlan?) -> Unit,
    disableShizukuHint: () -> Unit,
    goToPlanConfig: (DataPlan) -> Unit,
) {
    val activity = LocalActivity.current

    val activePlans = uiState.activePlans

    val pagerState = rememberPagerState(pageCount = {
        activePlans.size + if (uiState.shizukuHint && !uiState.shizukuTracking) 1 else 0
    })

    LaunchedEffect(activePlans.size, pagerState.currentPage) {
        selectDataPlan(
            if (pagerState.currentPage < activePlans.size) {
                activePlans[pagerState.currentPage]
            } else {
                null
            }
        )
    }

    HorizontalPager(
        modifier = Modifier.height(220.dp),
        state = pagerState,
        contentPadding = PaddingValues(horizontal = horizontalPadding),
        pageSpacing = horizontalPadding / 2,
        snapPosition = SnapPosition.Center,
        pageSize = PageSize.Fill
    ) { page ->
        if (page < activePlans.size) {
            val plan = activePlans[page]
            val snapshot = uiState.planSnapshots[plan]

            if (plan.configured) {
                ConfiguredDataPlan(plan, snapshot) { goToPlanConfig(plan) }
            } else {
                UnconfiguredDataPlan(plan, snapshot) { goToPlanConfig(plan) }
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
                        onClick = disableShizukuHint
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
private fun DataPlanInsights(
    contentPadding: PaddingValues,
    uiState: DataPlansUiState
) {
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
        val plan = uiState.plan
        val snapshot = uiState.planSnapshots[plan]
        if (plan != null && plan.mainDataSize.byteValue == 0L) {
            item {
                InfoCard(
                    title = stringResource(R.string.hint),
                    description = stringResource(R.string.press_the_card_to_configure_plan_data),
                    icon = painterResource(R.drawable.info),
                    backgroundColor = colorScheme.surface
                )
            }
        }
        if (plan != null && snapshot != null) {
            if (plan.note.isNotEmpty()) {
                item(key = "note") {
                    Box(Modifier.animateItem()) {
                        InfoCard(
                            title = stringResource(R.string.note),
                            description = plan.note,
                            icon = painterResource(R.drawable.sticky_note_2),
                            backgroundColor = colorScheme.surface
                        )
                    }
                }
            }
            if (plan.mainDataSize.byteValue > 0) usageInsights(uiState)
            extras(snapshot)
            uiState.weekUsage?.let { thisWeek(it) }
            if (uiState.adsEnabled) item { Ad(true, AdType.NativeBanner, colorScheme.surface) }
            if (plan.mainDataSize.byteValue > 0) budgetInsights(uiState)
            uiState.topApps?.let { topApps(it) }
        }
    }
}

private fun LazyListScope.extras(snapshot: DataPlanSnapshot) {
    val activeExtras = snapshot.extras.filter { !it.expired }
    if (activeExtras.isEmpty()) return

    item(key = "extras") {
        CategoryTitleText(stringResource(R.string.extras))
        Column(
            modifier = Modifier.animateItem(),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            activeExtras.chunked(2).forEach { chunk ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ExtraGraph(
                        modifier = Modifier
                            .weight(1f)
                            .background(colorScheme.surface, MaterialTheme.shapes.medium),
                        extra = chunk[0]
                    )
                    if (chunk.size > 1) {
                        ExtraGraph(
                            modifier = Modifier
                                .weight(1f)
                                .background(colorScheme.surface, MaterialTheme.shapes.medium),
                            extra = chunk[1]
                        )
                    } else {
                        Box(Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

private fun LazyListScope.usageInsights(uiState: DataPlansUiState) {
    item(key = "usage") {
        Column(Modifier.animateItem()) {
            CategoryTitleText(stringResource(R.string.usage))
            Row(
                modifier = Modifier.height(IntrinsicSize.Max),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val dataSafety = uiState.dataSafety
                MiniCard(
                    state = dataSafety,
                    baseColor = colorScheme.surface,
                    icon = painterResource(R.drawable.shield),
                    title = stringResource(R.string.safety),
                    tooltipText = stringResource(R.string.safety_tooltip),
                    description = AnnotatedString(
                        when (dataSafety) {
                            MiniCardState.POSITIVE -> stringResource(R.string.safe)
                            MiniCardState.NEUTRAL -> stringResource(R.string.neutral)
                            MiniCardState.NEGATIVE -> stringResource(R.string.unsafe)
                        }
                    )
                )

                TrendCard(
                    trend = uiState.trend,
                    baseColor = colorScheme.surface
                )
            }
        }
    }
}

private fun LazyListScope.budgetInsights(uiState: DataPlansUiState) {
    item(key = "budget") {
        Column(Modifier.animateItem()) {
            CategoryTitleText(stringResource(R.string.budget))
            val metric = LocalSizeMetric.current
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                val todayBudget = uiState.todayBudget
                val todayString by remember(todayBudget, metric) { derivedStateOf {
                    DataSize(todayBudget).toStringParts(metric = metric)
                } }
                MiniCard(
                    state = MiniCardState.NEUTRAL,
                    baseColor = colorScheme.surface,
                    icon = painterResource(R.drawable.today),
                    title = stringResource(R.string.today),
                    description = buildAnnotatedString {
                        withStyle(style = SpanStyle(fontSize = 24.sp)) {
                            append("${todayString.first}${todayString.second}")
                        }
                        withStyle(style = SpanStyle(fontSize = 20.sp)) {
                            append(todayString.third)
                        }
                    }
                )

                val remainingDailyBudget = uiState.remainingDailyBudget
                val remainingString by remember(remainingDailyBudget, metric) { derivedStateOf {
                    DataSize(remainingDailyBudget).toStringParts(metric = metric)
                } }
                MiniCard(
                    state = MiniCardState.NEUTRAL,
                    baseColor = colorScheme.surface,
                    icon = painterResource(R.drawable.calendar_month),
                    title = stringResource(R.string.daily),
                    description = buildAnnotatedString {
                        withStyle(style = SpanStyle(fontSize = 24.sp)) {
                            append("${remainingString.first}${remainingString.second}")
                        }
                        withStyle(style = SpanStyle(fontSize = 20.sp)) {
                            append(remainingString.third)
                        }
                    }
                )
            }
        }
    }
}

private fun LazyListScope.thisWeek(weekUsage: List<BarData>) {
    item(key = "this_week") {
        Column(Modifier.animateItem()) {
            CategoryTitleText(stringResource(R.string.this_week))
            Box(
                modifier = Modifier
                    .clip(MaterialTheme.shapes.medium)
                    .background(colorScheme.surface)
                    .padding(6.dp)
            ) {
                BarGraph(weekUsage)
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