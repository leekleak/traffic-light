package com.leekleak.trafficlight.ui.plans

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.snapping.SnapPosition
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PageSize
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfoV2
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.leekleak.core.R
import com.leekleak.core.ui.components.BackAction
import com.leekleak.core.ui.components.HazeScaffold
import com.leekleak.core.ui.navigation.LocalNavigationBarPadding
import com.leekleak.core.ui.theme.LocalSizeMetric
import com.leekleak.core.ui.theme.card
import com.leekleak.core.ui.theme.googleSans
import com.leekleak.core.util.CategoryTitleText
import com.leekleak.core.util.DataSize
import com.leekleak.core.util.MiniCard
import com.leekleak.core.util.MiniCardState
import com.leekleak.core.util.TrendCard
import com.leekleak.core.util.shelfShape
import com.leekleak.trafficlight.BuildConfig
import com.leekleak.trafficlight.charts.ActivityDay
import com.leekleak.trafficlight.charts.ActivityGraph
import com.leekleak.trafficlight.charts.AppGraph
import com.leekleak.trafficlight.charts.ExtraGraph
import com.leekleak.trafficlight.database.AppUsage
import com.leekleak.trafficlight.database.DataPlan
import com.leekleak.trafficlight.database.DataPlanSnapshot
import com.leekleak.trafficlight.ui.settings.InfoCard
import kotlinx.coroutines.launch
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

private val DataPlansUiState.showShizukuHint: Boolean
    get() = BuildConfig.SHIZUKU && !shizukuTracking

@Composable
fun DataPlans(
    uiState: DataPlansUiState,
    selectDataPlan: (DataPlan?) -> Unit,
    grantShizuku: () -> Unit,
    goToPlanConfig: (DataPlan) -> Unit,
    refresh: () -> Unit,
) {
    LifecycleResumeEffect(Unit) {
        refresh()
        onPauseOrDispose {}
    }

    val isWidescreen = currentWindowAdaptiveInfoV2()
        .windowSizeClass
        .isWidthAtLeastBreakpoint(600)

    HazeScaffold(
        title = stringResource(R.string.data_plans),
        scrollState = null,
        backAction = BackAction.None,
        extraPadding = LocalNavigationBarPadding.current,
    ) { paddingValues ->
        if (isWidescreen) {
            DataPlansTwoPane(
                paddingValues = paddingValues,
                uiState = uiState,
                selectDataPlan = selectDataPlan,
                grantShizuku = grantShizuku,
                goToPlanConfig = goToPlanConfig,
            )
        } else {
            DataPlansSinglePane(
                paddingValues = paddingValues,
                uiState = uiState,
                selectDataPlan = selectDataPlan,
                grantShizuku = grantShizuku,
                goToPlanConfig = goToPlanConfig,
            )
        }
    }
}

@Composable
private fun DataPlansSinglePane(
    paddingValues: PaddingValues,
    uiState: DataPlansUiState,
    selectDataPlan: (DataPlan?) -> Unit,
    grantShizuku: () -> Unit,
    goToPlanConfig: (DataPlan) -> Unit,
) {
    val layoutDirection = LocalLayoutDirection.current
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Spacer(Modifier.height(paddingValues.calculateTopPadding()))
        DataPlanPager(
            startPadding = paddingValues.calculateStartPadding(layoutDirection) + 8.dp,
            endPadding = paddingValues.calculateEndPadding(layoutDirection) + 8.dp,
            uiState = uiState,
            selectDataPlan = selectDataPlan,
            grantShizuku = grantShizuku
        )
        DataPlanInsights(
            bottomPadding = paddingValues.calculateBottomPadding(),
            uiState = uiState,
            onConfigurePlan = goToPlanConfig
        )
    }
}

@Composable
private fun DataPlansTwoPane(
    paddingValues: PaddingValues,
    uiState: DataPlansUiState,
    selectDataPlan: (DataPlan?) -> Unit,
    grantShizuku: () -> Unit,
    goToPlanConfig: (DataPlan) -> Unit,
) {
    val layoutDirection = LocalLayoutDirection.current
    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(
                start = paddingValues.calculateStartPadding(layoutDirection),
                end = paddingValues.calculateEndPadding(layoutDirection),
            ),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight(),
            contentPadding = PaddingValues(
                top = paddingValues.calculateTopPadding(),
                bottom = paddingValues.calculateBottomPadding(),
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(uiState.activePlans, key = { it.hashedSubscriberID }) { plan ->
                val planSnapshot = uiState.planSnapshots[plan]
                Box(Modifier.animateItem()) {
                    DataPlanCard(plan, planSnapshot, plan == uiState.selectedPlan) { selectDataPlan(plan) }
                }
            }
            if (uiState.showShizukuHint) {
                item(key = "shizuku_hint") {
                    ShizukuHintCard(
                        onGrant = grantShizuku,
                        shizukuRunning = uiState.shizukuRunning,
                        modifier = Modifier.animateItem()
                    )
                }
            }
        }
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
        ) {
            Spacer(Modifier.height(paddingValues.calculateTopPadding()))
            DataPlanInsights(
                uiState = uiState,
                modifier = Modifier
                    .weight(1f)
                    .padding(bottom = paddingValues.calculateBottomPadding()),
                shape = MaterialTheme.shapes.large,
                onConfigurePlan = goToPlanConfig
            )
        }
    }
}

@Composable
private fun DataPlanPager(
    startPadding: Dp,
    endPadding: Dp,
    uiState: DataPlansUiState,
    selectDataPlan: (DataPlan?) -> Unit,
    grantShizuku: () -> Unit,
) {
    val scope = rememberCoroutineScope()

    val activePlans = uiState.activePlans

    val pagerState = rememberPagerState(pageCount = {
        activePlans.size + if (uiState.showShizukuHint) 1 else 0
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
        contentPadding = PaddingValues(start = startPadding, end = endPadding),
        pageSpacing = 12.dp,
        snapPosition = SnapPosition.Center,
        pageSize = PageSize.Fill
    ) { page ->
        if (page < activePlans.size) {
            val plan = activePlans[page]
            DataPlanCard(plan, uiState.planSnapshots[plan], page == pagerState.currentPage) {
                scope.launch { pagerState.animateScrollToPage(page) }
            }
        } else {
            ShizukuHintCard(
                onGrant = grantShizuku,
                shizukuRunning = uiState.shizukuRunning
            )
        }
    }
}

@Composable
private fun ShizukuHintCard(
    onGrant: () -> Unit,
    shizukuRunning: Boolean,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .height(200.dp)
            .border(1.5.dp, colorScheme.secondary, MaterialTheme.shapes.medium)
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            modifier = Modifier.size(32.dp),
            painter = painterResource(R.drawable.sim_card),
            contentDescription = null,
            tint = colorScheme.secondary
        )
        Text(
            text = stringResource(R.string.track_multiple_sim_cards),
            color = colorScheme.secondary,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        Text(
            text = stringResource(R.string.shizuku_permission_required),
            color = colorScheme.secondary,
            textAlign = TextAlign.Center
        )
        if (!shizukuRunning) {
            Text(
                text = stringResource(R.string.shizuku_not_running),
                color = colorScheme.secondary,
                textAlign = TextAlign.Center
            )
        }
        Button(
            colors = ButtonDefaults.buttonColors(
                containerColor = colorScheme.secondary,
                contentColor = colorScheme.onSecondary
            ),
            shape = MaterialTheme.shapes.large,
            enabled = shizukuRunning,
            onClick = onGrant
        ) {
            Text(stringResource(R.string.grant))
        }
    }
}

@Composable
private fun DataPlanInsights(
    modifier: Modifier = Modifier,
    bottomPadding: Dp = 16.dp,
    uiState: DataPlansUiState,
    onConfigurePlan: (DataPlan) -> Unit,
    shape: Shape = shelfShape
) {
    val contentPadding = PaddingValues(
        start = 16.dp,
        top = 0.dp,
        end = 16.dp,
        bottom = bottomPadding
    )
    val listState = rememberLazyListState()

    LazyColumn(
        modifier = modifier
            .clip(shape)
            .background(colorScheme.surfaceContainer)
            .fillMaxSize(),
        contentPadding = contentPadding,
        verticalArrangement = Arrangement.spacedBy(6.dp),
        state = listState
    ) {
        item{}
        val plan = uiState.selectedPlan
        val snapshot = uiState.planSnapshots[plan]
        if (plan?.configured == false) configurePlanItem(plan, onConfigurePlan)
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
            uiState.topApps?.let { topApps(it) }
            if (plan.mainDataSize.byteValue > 0) budgetInsights(uiState)
            uiState.activityUsage?.let { activity(it, plan) }
        }
        if (plan?.configured == true) configurePlanItem(plan, onConfigurePlan)
    }
}

private fun LazyListScope.configurePlanItem(
    plan: DataPlan,
    onConfigurePlan: (DataPlan) -> Unit
) {
    item(key = "configure") {
        val haptic = LocalHapticFeedback.current
        FilledTonalButton(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.large,
            onClick = {
                haptic.performHapticFeedback(HapticFeedbackType.ContextClick)
                onConfigurePlan(plan)
            }
        ) {
            Icon(painterResource(R.drawable.edit), contentDescription = null)
            Text(
                modifier = Modifier.padding(start = 8.dp),
                text = stringResource(R.string.configure_plan)
            )
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

private fun LazyListScope.activity(days: List<ActivityDay>, plan: DataPlan) {
    item(key = "daily_activity") {
        Column(Modifier.animateItem()) {
            CategoryTitleText(stringResource(R.string.activity))
            val metric = LocalSizeMetric.current
            val font = remember { googleSans(weight = 700f) }
            var selectedEpochDay by rememberSaveable(plan.hashedSubscriberID) {
                mutableStateOf<Long?>(null)
            }

            val lastEpochDay = days.lastOrNull()?.date?.toEpochDay()
            LaunchedEffect(days) {
                val selected = selectedEpochDay
                if (selected == null || days.none { it.date.toEpochDay() == selected }) {
                    selectedEpochDay = lastEpochDay
                }
            }

            val selectedDay = days.firstOrNull { it.date.toEpochDay() == selectedEpochDay }

            Column(Modifier.card(colorScheme.surface)) {
                ActivityGraph(
                    data = days,
                    periodStart = plan.getStartDate().toLocalDate().takeIf { plan.configured },
                    selectedDate = selectedDay?.date,
                    onSelectDate = { selectedEpochDay = it.toEpochDay() },
                )

                selectedDay?.let { day ->
                    val date = remember(day.date) {
                        day.date.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM))
                    }
                    val usage = DataSize(day.usage).toString(metric = metric)
                    Row(
                        modifier = Modifier.padding(8.dp).fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)
                    ) {
                        Text(
                            modifier = Modifier
                                .background(colorScheme.primary, MaterialTheme.shapes.small)
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                            text = date,
                            fontFamily = font,
                            color = colorScheme.onPrimary,
                        )
                        Text(
                            modifier = Modifier
                                .background(colorScheme.secondary, MaterialTheme.shapes.small)
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                            text = usage,
                            fontFamily = font,
                            color = colorScheme.onSecondary,
                        )
                    }
                }
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