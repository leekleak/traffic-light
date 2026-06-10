package com.leekleak.trafficlight.ui.overview

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ButtonGroup
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialShapes.Companion.Cookie12Sided
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfoV2
import androidx.compose.material3.toPath
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.graphics.copy
import androidx.compose.ui.graphics.drawscope.translate
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
import com.leekleak.trafficlight.R
import com.leekleak.trafficlight.charts.AppGraph
import com.leekleak.trafficlight.charts.BarGraph
import com.leekleak.trafficlight.database.DataType
import com.leekleak.trafficlight.database.UsageQuery
import com.leekleak.trafficlight.integrations.Ad
import com.leekleak.trafficlight.integrations.AdType
import com.leekleak.trafficlight.ui.navigation.Navigator
import com.leekleak.trafficlight.ui.navigation.SettingsKey
import com.leekleak.trafficlight.ui.theme.card
import com.leekleak.trafficlight.ui.theme.googleSans
import com.leekleak.trafficlight.util.CategoryTitleText
import com.leekleak.trafficlight.util.DataSize
import com.leekleak.trafficlight.util.EqualHeightRow
import com.leekleak.trafficlight.util.MiniCard
import com.leekleak.trafficlight.util.MiniCardState
import com.leekleak.trafficlight.util.PageTitle
import com.leekleak.trafficlight.util.TrendCard
import com.leekleak.trafficlight.util.iconToggleButton
import com.leekleak.trafficlight.util.px
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.rememberHazeState
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject


@Composable
fun Overview(
    paddingValues: PaddingValues,
) {
    val viewModel: OverviewVM = koinViewModel()
    val navigator: Navigator = koinInject()

    val windowSizeClass = currentWindowAdaptiveInfoV2().windowSizeClass

    val hazeState = rememberHazeState()
    val scrollState = rememberScrollState()

    LifecycleResumeEffect(Unit) {
        viewModel.refresh()
        onPauseOrDispose {}
    }

    val paddingSide = paddingValues.calculateLeftPadding(LayoutDirection.Ltr)
    val paddingTop = paddingValues.calculateTopPadding()
    val paddingBottom = paddingValues.calculateBottomPadding()

    Column(
        modifier = Modifier
            .background(colorScheme.surface)
            .fillMaxSize()
            .hazeSource(hazeState)
            .padding(horizontal = paddingSide)
            .verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(Modifier.height(paddingTop - 8.dp))
        if (windowSizeClass.isWidthAtLeastBreakpoint(400)) {
            EqualHeightRow (
                modifier = Modifier
                    .padding(horizontal = 16.dp),
                first = {
                    Column (Modifier
                        .weight(1f)
                        .fillMaxHeight(), verticalArrangement = Arrangement.SpaceBetween) {
                        HeroItems(scrollState)
                    }
                },
                second = {
                    Column (Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        OverviewItems()
                    }
                },
                spacing = 16.dp
            )
        } else {
            HeroItems(scrollState)
            OverviewItems()
        }
        Box(Modifier.height(paddingBottom - 8.dp))
    }
    PageTitle(false, hazeState, stringResource(R.string.today)) {
        IconButton(
            modifier = Modifier.align(Alignment.CenterEnd),
            onClick = { navigator.goTo(SettingsKey) }
        ) {
            Icon(
                painterResource(R.drawable.settings),
                contentDescription = stringResource(R.string.settings)
            )
        }
    }
}

@Composable
private fun HeroItems(scrollState: ScrollState) {
    OverviewHero(scrollState)
    Row(
        modifier = Modifier.height(IntrinsicSize.Min),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        PredictionCard()
        val viewModel: OverviewVM = koinViewModel()
        val trend by viewModel.trend.collectAsState()
        TrendCard(trend)
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun OverviewHero(scrollState: ScrollState) {
    val viewModel: OverviewVM = koinViewModel()
    val haptic = LocalHapticFeedback.current

    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val query by viewModel.query.collectAsState()

    val offset by animateFloatAsState(if (pressed) 132.dp.px else 116.dp.px)

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

    val scheme = colorScheme
    val shape1 = Cookie12Sided.toPath()
    val shapeScale = 336.dp.px
    val iconScale = remember { Animatable(shapeScale) }

    val infiniteTransition = rememberInfiniteTransition()
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(50000, easing = LinearEasing)
        )
    )

    val shape1Transformed = remember(iconScale.value, rotation) {
        val matrix = Matrix().apply {
            rotateZ(rotation)
            scale(iconScale.value, iconScale.value)
            translate(-0.5f, -0.5f)
        }
        shape1.copy().apply { transform(matrix) }
    }
    val shape2Transformed = remember(iconScale.value, rotation) {
        val matrix = Matrix().apply {
            rotateZ(-rotation + 360f / 24)
            scale(iconScale.value, iconScale.value)
            translate(-0.5f, -0.5f)
        }
        shape1.copy().apply { transform(matrix) }
    }
    val glowColor by animateColorAsState(targetValue =
        if (query.dataType == DataType.Mobile) colorScheme.primaryContainer
        else colorScheme.tertiaryContainer
    )
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = { }
            ),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    translationY = scrollState.value * 0.4f
                }
                .drawWithContent {
                    val a = size.width / 2 - offset
                    val b = size.width / 2 + offset

                    drawCircle(
                        Brush.radialGradient(
                            listOf(
                                glowColor,
                                Color.Transparent
                            )
                        )
                    )
                    translate(a, b) { drawPath(shape1Transformed, scheme.surface) }
                    translate(b, a) { drawPath(shape2Transformed, scheme.surface) }
                }
        )
        Column(modifier = Modifier.align(Alignment.Center)) {
            val todayUsage by viewModel.todayUsage.collectAsState()
            val string = DataSize(todayUsage).toStringParts(extraPrecision = true)

            val width by animateFloatAsState(
                targetValue = if (pressed) 60f else 30f,
                animationSpec = spring()
            )
            val weight by animateFloatAsState(if (pressed) 800f else 400f, spring())
            val fontFamily1 = remember(weight, width) { googleSans(weight = weight, width = width, roundness = 100f) }
            val fontFamily2 = remember(weight, width) { googleSans(weight = weight + 200f, width = width + 70f, roundness = 50f) }

            AnimatedContent(string to (query.dataType == DataType.Wifi)) { (text, isWifi) ->
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
        }

        val wifiToggledColors = IconButtonDefaults.iconButtonColors(
            containerColor = colorScheme.tertiaryContainer,
            contentColor = colorScheme.onTertiaryContainer
        )

        ButtonGroup(
            modifier = Modifier.align(Alignment.BottomCenter),
            overflowIndicator = {},
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            iconToggleButton(
                text = null,
                selected = query.dataType == DataType.Mobile,
                onSelect = {viewModel.query.value = UsageQuery(DataType.Mobile) }
            ) {
                Icon(
                    painter = painterResource(R.drawable.cellular),
                    contentDescription = stringResource(R.string.cellular)
                )
            }
            iconToggleButton(
                text = null,
                selected = query.dataType == DataType.Wifi,
                toggledColors = wifiToggledColors,
                onSelect = {viewModel.query.value = UsageQuery(DataType.Wifi) }
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
private fun RowScope.PredictionCard() {
    val viewModel: OverviewVM = koinViewModel()
    val prediction by viewModel.prediction.collectAsState()
    val string = DataSize(prediction).toStringParts(extraPrecision = true)

    MiniCard(
        state = MiniCardState.NEUTRAL,
        icon = painterResource(R.drawable.query_stats),
        title = stringResource(R.string.prediction),
        tooltipText = stringResource(R.string.prediction_tooltip),
        description = buildAnnotatedString {
            withStyle(style = SpanStyle(fontSize = 24.sp)) {
                append("${string.first}${string.second}")
            }
            withStyle(style = SpanStyle(fontSize = 20.sp)) {
                append(string.third)
            }
        }
    )
}

@Composable
fun OverviewItems() {
    val viewModel: OverviewVM = koinViewModel()
    val data by viewModel.weekUsage.collectAsState()
    val topAppsList by viewModel.topApps.collectAsState()
    val query by viewModel.query.collectAsState()
    CategoryTitleText(stringResource(R.string.top_apps))
    Box(
        modifier = Modifier
            .card()
            .padding(6.dp)
    ) {
        AnimatedContent(
            targetState = topAppsList to (query.dataType == DataType.Wifi)
        ) { (list, isWifi) ->
            AppGraph(list, isWifi)
        }
    }
    Ad(AdType.NativeBanner)
    if (data.isNotEmpty()) {
        CategoryTitleText(stringResource(R.string.this_week))
        Box(
            modifier = Modifier
                .card()
                .padding(6.dp)
        ) {
            BarGraph(
                data = data,
                centerLabels = true
            )
        }
    }
}
