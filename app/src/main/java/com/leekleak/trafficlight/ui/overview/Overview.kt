package com.leekleak.trafficlight.ui.overview

import androidx.activity.compose.LocalActivity
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
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
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
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.runtime.rememberCoroutineScope
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
import com.leekleak.trafficlight.charts.BarGraph
import com.leekleak.trafficlight.database.AppPreferenceRepo
import com.leekleak.trafficlight.database.DataPlanDao
import com.leekleak.trafficlight.ui.navigation.Navigator
import com.leekleak.trafficlight.ui.navigation.PlanConfig
import com.leekleak.trafficlight.ui.navigation.Settings
import com.leekleak.trafficlight.ui.settings.PermissionButton
import com.leekleak.trafficlight.ui.settings.PermissionCard
import com.leekleak.trafficlight.ui.theme.card
import com.leekleak.trafficlight.ui.theme.googleSans
import com.leekleak.trafficlight.util.CategoryTitleText
import com.leekleak.trafficlight.util.DataSize
import com.leekleak.trafficlight.util.EqualHeightRow
import com.leekleak.trafficlight.util.PageTitle
import com.leekleak.trafficlight.util.openLink
import com.leekleak.trafficlight.util.px
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.rememberHazeState
import kotlinx.coroutines.launch
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
                    Column (Modifier.weight(1f).fillMaxHeight(), verticalArrangement = Arrangement.SpaceBetween) {
                        HeroItems(scrollState)
                    }
                },
                second = {
                    Column (Modifier.weight(1f)) {
                        DataPlanItems(navigator)
                        OverviewItems()
                    }
                },
                spacing = 16.dp
            )
        } else {
            HeroItems(scrollState)
            DataPlanItems(navigator)
            OverviewItems()
        }
        Box(Modifier.height(paddingBottom - 8.dp))
    }
    PageTitle(false, hazeState, stringResource(R.string.today)) {
        IconButton(
            modifier = Modifier.align(Alignment.CenterEnd),
            onClick = { navigator.goTo(Settings) }
        ) {
            Icon(
                painterResource(R.drawable.settings),
                contentDescription = stringResource(R.string.settings)
            )
        }
    }
}

@Composable
private fun DataPlanItems(
    navigator: Navigator,
) {
    val dataPlanDao: DataPlanDao = koinInject()
    val appPreferenceRepo: AppPreferenceRepo = koinInject()

    val activity = LocalActivity.current
    val scope = rememberCoroutineScope()

    val activePlans by remember { dataPlanDao.getActivePlansFlow() }.collectAsState(listOf())
    val shizukuHint by remember { appPreferenceRepo.shizukuHint }.collectAsState(false)
    val shizukuTracking by remember { appPreferenceRepo.shizukuTracking }.collectAsState(true)

    CategoryTitleText(stringResource(R.string.data_plans))

    for (i in activePlans) {
        if (i.dataMax != 0L) {
            ConfiguredDataPlan(i) {
                navigator.goTo(PlanConfig(i))
            }
        } else {
            UnconfiguredDataPlan(i) {
                navigator.goTo(PlanConfig(i))
            }
        }
    }

    if (shizukuHint && !shizukuTracking) {
        PermissionCard(
            title = stringResource(R.string.shizuku_hint),
            description = stringResource(R.string.shizuku_hint_description),
            icon = painterResource(R.drawable.warning),
            onHelp = {
                openLink(
                    activity,
                    "https://github.com/leekleak/traffic-light/wiki/Setting-up-Shizuku-for-multi%E2%80%90SIM-tracking"
                )
            },
            actionButton = {
                PermissionButton(
                    icon = painterResource(R.drawable.close),
                    contentDescription = stringResource(R.string.close),
                    onClick = { scope.launch { appPreferenceRepo.setShizukuHint(false) } }
                )
            }
        )
    }
}

@Composable
private fun ColumnScope.HeroItems(scrollState: ScrollState) {
    OverviewHero(scrollState)
    //Spacer(modifier = Modifier.weight(1f))
    Row(
        modifier = Modifier.height(IntrinsicSize.Min),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        PredictionCard()
        TrendCard()
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun OverviewHero(scrollState: ScrollState) {
    val viewModel: OverviewVM = koinViewModel()
    val haptic = LocalHapticFeedback.current

    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()

    val offset by animateFloatAsState(if (pressed) 30.dp.px else 0f)

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
                val a = size.width / 5 - offset
                val b = (a + offset) * 4 + offset

                drawCircle(Brush.radialGradient(listOf(scheme.primaryContainer, Color.Transparent)))
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

            LaunchedEffect(pressed) {
                if (pressed) {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                } else {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                }
            }
            Text(
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
                text = buildAnnotatedString {
                    withStyle(style = SpanStyle(fontFamily = fontFamily1, fontSize = 100.sp)) {
                        append("${string.first}${string.second}")
                    }
                    withStyle(style = SpanStyle(fontFamily = fontFamily1, fontSize = 42.sp)) {
                        appendLine(string.third)
                    }
                    withStyle(style = SpanStyle(fontFamily = fontFamily2, fontSize = 20.sp)) {
                        append(stringResource(R.string.mobile_data))
                    }
                }
            )
        }
    }
}

@Composable
private fun RowScope.PredictionCard() {
    val viewModel: OverviewVM = koinViewModel()
    val fontFamily = remember { googleSans(weight = 600f) }
    Column(
        modifier = Modifier
            .card()
            .padding(16.dp)
            .weight(1f)
            .fillMaxHeight(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        val prediction by viewModel.prediction.collectAsState()
        val string = DataSize(prediction).toStringParts(extraPrecision = true)

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                painterResource(R.drawable.query_stats),
                contentDescription = null
            )
            Text(stringResource(R.string.prediction))
        }
        Row {
            Text(
                modifier = Modifier.alignByBaseline(),
                text = string.first + string.second,
                fontFamily = fontFamily,
                fontSize = 24.sp
            )
            Text(
                modifier = Modifier.alignByBaseline(),
                text = string.third,
                fontFamily = fontFamily,
                fontSize = 20.sp
            )
        }
    }
}

@Composable
private fun RowScope.TrendCard() {
    val viewModel: OverviewVM = koinViewModel()
    val trend by viewModel.trend.collectAsState()
    val fontFamily = remember { googleSans(weight = 600f) }
    Column(
        modifier = Modifier
            .card()
            .then(
                when {
                    trend > 50 -> Modifier.background(colorScheme.errorContainer)
                    trend < -25 -> Modifier.background(colorScheme.primaryContainer)
                    else -> Modifier
                }
            )
            .padding(16.dp)
            .weight(1f),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                painter = when {
                    trend > 50 -> painterResource(R.drawable.trending_up)
                    trend < -25 -> painterResource(R.drawable.trending_down)
                    else -> painterResource(R.drawable.trending_flat)
                },
                contentDescription = null
            )
            Text(stringResource(R.string.trend))
        }
        Row {
            Text(
                modifier = Modifier.alignByBaseline(),
                text = if (trend < 1000)"%+d%%".format(trend.toInt()) else stringResource(R.string.very_big),
                fontFamily = fontFamily,
                fontSize = 24.sp
            )
        }
    }
}

@Composable
fun OverviewItems() {
    val viewModel: OverviewVM = koinViewModel()
    val data by viewModel.weekUsage.collectAsState()
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
