package com.leekleak.trafficlight.ui.overview

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.snapping.SnapPosition
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PageSize
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.ButtonGroup
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfoV2
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.leekleak.core.R
import com.leekleak.core.charts.SpeedGraph
import com.leekleak.core.charts.SpeedGraphTag
import com.leekleak.core.ui.components.BackAction
import com.leekleak.core.ui.components.HazeScaffold
import com.leekleak.core.ui.navigation.LocalNavigationBarPadding
import com.leekleak.core.ui.navigation.Navigator
import com.leekleak.core.ui.theme.LocalSizeMetric
import com.leekleak.core.ui.theme.card
import com.leekleak.core.ui.theme.googleSans
import com.leekleak.core.util.CategoryTitleText
import com.leekleak.core.util.DataSize
import com.leekleak.core.util.formattedParts
import com.leekleak.core.util.getName
import com.leekleak.core.util.iconToggleButton
import com.leekleak.core.util.rememberIs24HourFormat
import com.leekleak.core.util.toLocaleHourString
import com.leekleak.trafficlight.charts.AppGraph
import com.leekleak.trafficlight.database.DataType
import com.leekleak.trafficlight.ui.navigation.DataPlansKey
import com.leekleak.trafficlight.ui.navigation.SettingsKey
import com.leekleak.trafficlight.ui.plans.DataPlanCard
import kotlin.math.ceil


@Composable
fun Overview(
    navigator: Navigator,
    viewModel: OverviewVM
) {
    val haptic = LocalHapticFeedback.current
    val windowSizeClass = currentWindowAdaptiveInfoV2().windowSizeClass
    val isWidescreen = windowSizeClass.isWidthAtLeastBreakpoint(600)

    LifecycleResumeEffect(Unit) {
        viewModel.refresh()
        onPauseOrDispose {}
    }

    HazeScaffold(
        title = stringResource(R.string.overview),
        applyPadding = false,
        backAction = BackAction.None,
        extraPadding = LocalNavigationBarPadding.current,
        actions = {
            IconButton(
                onClick = {
                    navigator.goTo(SettingsKey)
                    haptic.performHapticFeedback(HapticFeedbackType.ContextClick)
                }
            ) {
                Icon(painterResource(R.drawable.settings), contentDescription = stringResource(R.string.settings))
            }
        }
    ) { contentPadding ->
        Spacer(Modifier.height(contentPadding.calculateTopPadding()))
        if (isWidescreen) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(start = contentPadding.calculateLeftPadding(LayoutDirection.Ltr)),
                horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.Top,
            ) {
                Column(
                    modifier = Modifier.widthIn(max = 400.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    OverviewHero(viewModel, true)
                    OverviewRangeSelector(viewModel)
                }
                Column(
                    modifier = Modifier.widthIn(max = 400.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    OverviewItems(navigator, viewModel)
                }
            }
        } else {
            OverviewHero(viewModel, false)
            OverviewRangeSelector(viewModel)
            OverviewItems(navigator, viewModel)
        }
        Spacer(Modifier.height(contentPadding.calculateBottomPadding()))
    }
}

@Composable
private fun OverviewHero(viewModel: OverviewVM, isWidescreen: Boolean, ) {
    val haptic = LocalHapticFeedback.current

    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val query by viewModel.query.collectAsStateWithLifecycle()

    LaunchedEffect(interactionSource) {
        interactionSource.interactions.collect { interaction ->
            when (interaction) {
                is PressInteraction.Press -> {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                }
                is PressInteraction.Release, is PressInteraction.Cancel -> {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                }
            }
        }
    }

    val graphUsage by viewModel.graphUsage.collectAsStateWithLifecycle()
    val range by viewModel.range.collectAsStateWithLifecycle()
    val graphValues = remember(graphUsage) { graphUsage.map { it.value } }
    val is24HourFormat by rememberIs24HourFormat()
    val metric = LocalSizeMetric.current
    val graphColor by animateColorAsState(
        if (query.dataType == DataType.Wifi) colorScheme.tertiary else colorScheme.primary
    )

    val pressProgress by animateFloatAsState(
        targetValue = if (pressed) 1f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "pressProgress"
    )

    val labelScale = 0.6f

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .height(312.dp)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = { }
            ),
    ) {
        val spacing = if (graphUsage.size > 1) (maxWidth + 32.dp) / (graphUsage.size - 1) else 0.dp
        val minGap = if (spacing > 0.dp) ceil(72.dp / spacing).toInt().coerceAtLeast(1) else 1
        val tagLabels = remember(graphUsage, range, is24HourFormat, metric, minGap) {
            val values = graphUsage.map { it.value }
            highlightIndices(values, count = 3, minGap = minGap).map { index ->
                val point = graphUsage[index]
                val title = when (range) {
                    OverviewRange.Today -> point.start.toLocalTime().toLocaleHourString(is24HourFormat, short = true)
                    OverviewRange.Week -> point.start.dayOfWeek.getName(java.time.format.TextStyle.FULL)
                    OverviewRange.Month -> "${point.start.month.getName(java.time.format.TextStyle.SHORT)} ${point.start.dayOfMonth.toString().padStart(2, '0')}"
                }
                SpeedGraphTag(index, title, DataSize(point.value.toLong()).toString(metric = metric))
            }
        }
        val tags = tagLabels.mapIndexed { rank, tag ->
            tag.copy(alpha = ((pressProgress - 0.2f - rank * 0.08f) / 0.6f).coerceIn(0f, 1f))
        }

        SpeedGraph(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .requiredWidth(maxWidth)
                .fillMaxHeight(0.8f - 0.2f * pressProgress),
            data = graphValues,
            width = (graphValues.size - 1).coerceAtLeast(1),
            graphColor = graphColor,
            fillAlpha = 0.7f,
            tags = tags,
            sideBorder = isWidescreen,
        )

        Column(
            modifier = Modifier
                .align(BiasAlignment(0f, -pressProgress))
                .graphicsLayer {
                    val scale = 1f + (labelScale - 1f) * pressProgress
                    scaleX = scale
                    scaleY = scale
                    transformOrigin = TransformOrigin(0.5f, 0f)
                }
        ) {
            val usage by viewModel.usage.collectAsStateWithLifecycle()
            val string = DataSize(usage).formattedParts(extraPrecision = true)

            val width = 30f + 30f * pressProgress
            val weight = 400f + 400f * pressProgress
            val fontFamily1 = remember(weight, width) { googleSans(weight = weight, width = width, roundness = 100f) }
            val fontFamily2 = remember(weight, width) { googleSans(weight = weight, width = width + 70f, roundness = 50f) }

            val bundledUsage = remember(string) { string to (query.dataType == DataType.Wifi) }
            AnimatedContent(bundledUsage) { (text, isWifi) ->
                Text(
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center,
                    text = buildAnnotatedString {
                        withStyle(style = SpanStyle(fontFamily = fontFamily1, fontSize = 100.sp)) {
                            append("${text.first}${text.second}")
                        }
                        withStyle(style = SpanStyle(fontFamily = fontFamily1, fontSize = 42.sp)) {
                            appendLine(text.third)
                        }
                        withStyle(style = SpanStyle(fontFamily = fontFamily2, fontSize = 20.sp)) {
                            if (isWifi) {
                                append(stringResource(R.string.wifi))
                            } else {
                                append(stringResource(R.string.mobile_data))
                            }
                        }
                    }
                )
            }
            Spacer(Modifier.height(36.dp))
        }

        val wifiToggledColors = IconButtonDefaults.iconButtonColors(
            containerColor = colorScheme.tertiaryContainer,
            contentColor = colorScheme.onTertiaryContainer
        )

        ButtonGroup(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .graphicsLayer { alpha = 1f - pressProgress },
            overflowIndicator = {},
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            iconToggleButton(
                text = null,
                selected = query.dataType == DataType.Mobile,
                onSelect = { viewModel.setDataType(DataType.Mobile) }
            ) {
                Icon(
                    painter = painterResource(R.drawable.cellular),
                    contentDescription = stringResource(R.string.cellular)
                )
            }
            iconToggleButton(
                text = null,
                selected = query.dataType == DataType.Wifi,
                selectedColors = wifiToggledColors,
                onSelect = { viewModel.setDataType(DataType.Wifi) }
            ) {
                Icon(
                    painter = painterResource(R.drawable.wifi),
                    contentDescription = stringResource(R.string.wifi)
                )
            }
        }
    }
}

@Composable
private fun OverviewRangeSelector(viewModel: OverviewVM) {
    val haptic = LocalHapticFeedback.current
    val range by viewModel.range.collectAsStateWithLifecycle()
    val query by viewModel.query.collectAsStateWithLifecycle()
    val labels = OverviewRange.entries.map { stringResource(it.nameRes) }
    val selectedColors = if (query.dataType == DataType.Wifi) {
        IconButtonDefaults.iconButtonColors(
            containerColor = colorScheme.tertiaryContainer,
            contentColor = colorScheme.onTertiaryContainer
        )
    } else {
        IconButtonDefaults.iconButtonColors(
            containerColor = colorScheme.primaryContainer,
            contentColor = colorScheme.onPrimaryContainer
        )
    }

    ButtonGroup(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        expandedRatio = 0.05f,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        overflowIndicator = {}
    ) {
        OverviewRange.entries.forEachIndexed { index, option ->
            iconToggleButton(
                selected = range == option,
                text = labels[index],
                selectedColors = selectedColors,
                onSelect = {
                    viewModel.setRange(option)
                    haptic.performHapticFeedback(HapticFeedbackType.ToggleOn)
                },
                weight = 1f
            )
        }
    }
}

@Composable
fun OverviewItems(navigator: Navigator, viewModel: OverviewVM) {
    val topAppsList by viewModel.topApps.collectAsStateWithLifecycle()
    val query by viewModel.query.collectAsStateWithLifecycle()
    Column(Modifier.padding(horizontal = 16.dp)) {
        CategoryTitleText(stringResource(R.string.top_apps))
        Box(modifier = Modifier.card().padding(6.dp)) {
            val listWithWifi =
                remember(topAppsList) { topAppsList to (query.dataType == DataType.Wifi) }
            AnimatedContent(
                targetState = listWithWifi
            ) { (list, isWifi) ->
                AppGraph(list, isWifi)
            }
        }
    }
    OverviewPlans(navigator, viewModel)
}

@Composable
private fun OverviewPlans(navigator: Navigator, viewModel: OverviewVM) {
    val plans by viewModel.plans.collectAsStateWithLifecycle()
    val snapshots by viewModel.planSnapshots.collectAsStateWithLifecycle()
    val haptic = LocalHapticFeedback.current
    if (plans.isEmpty()) return

    val pagerState = rememberPagerState { plans.size }

    Box(Modifier.padding(horizontal = 16.dp)) {
        CategoryTitleText(stringResource(R.string.plans))
    }
    HorizontalPager(
        state = pagerState,
        contentPadding = PaddingValues(horizontal = 16.dp),
        pageSpacing = 12.dp,
        snapPosition = SnapPosition.Center,
        pageSize = PageSize.Fill
    ) { page ->
        val plan = plans[page]
        DataPlanCard(
            plan = plan,
            planSnapshot = snapshots[plan],
            onClick = {
                haptic.performHapticFeedback(HapticFeedbackType.ContextClick)
                navigator.setTo(DataPlansKey(plan.hashedSubscriberID))
            }
        )
    }
}
