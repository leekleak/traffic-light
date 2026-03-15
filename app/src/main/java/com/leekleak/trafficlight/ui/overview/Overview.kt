package com.leekleak.trafficlight.ui.overview

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialShapes.Companion.Cookie12Sided
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.Text
import androidx.compose.material3.toPath
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.graphics.copy
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import com.leekleak.trafficlight.R
import com.leekleak.trafficlight.charts.BarGraph
import com.leekleak.trafficlight.charts.model.BarData
import com.leekleak.trafficlight.database.DataPlanDao
import com.leekleak.trafficlight.model.NetworkUsageManager
import com.leekleak.trafficlight.ui.navigation.PlanConfig
import com.leekleak.trafficlight.ui.navigation.Settings
import com.leekleak.trafficlight.ui.theme.card
import com.leekleak.trafficlight.ui.theme.jetbrainsMono
import com.leekleak.trafficlight.ui.theme.outfit
import com.leekleak.trafficlight.util.CategoryTitleText
import com.leekleak.trafficlight.util.DataSize
import com.leekleak.trafficlight.util.categoryTitle
import com.leekleak.trafficlight.util.px
import org.koin.compose.koinInject


@Composable
fun Overview(
    paddingValues: PaddingValues,
    backStack: NavBackStack<NavKey>
) {
    val networkUsageManager: NetworkUsageManager = koinInject()
    val dataPlanDao: DataPlanDao = koinInject()

    val weeklyUsage by networkUsageManager.weekUsage().collectAsState(listOf())
    val activePlans by remember { dataPlanDao.getActiveFlow() }.collectAsState(listOf())

    val columnState = rememberLazyListState()
    LazyColumn(
        modifier = Modifier
            .background(colorScheme.surface)
            .fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = paddingValues,
        state = columnState
    ) {
        item {
            Box (Modifier.fillMaxWidth()) {
                CategoryTitleText(stringResource(R.string.today))
                IconButton(
                    modifier = Modifier.align(Alignment.CenterEnd),
                    onClick = { backStack.add(Settings) }
                ) {
                    Icon(
                        painterResource(R.drawable.settings),
                        contentDescription = stringResource(R.string.settings)
                    )
                }
            }
        }
        item {
            OverviewHero()
        }
        item {
            Row (horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PredictionCard()
                TrendCard()
            }
        }
        categoryTitle { stringResource(R.string.data_plans) }

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

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun OverviewHero() {
    val networkUsageManager: NetworkUsageManager = koinInject()

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
            .aspectRatio(1f)
            .fillMaxWidth()
            .clipToBounds()
            .drawBehind {
                val a = size.width / 5
                val b = a * 4

                drawCircle(Brush.radialGradient(listOf(scheme.primaryContainer, Color.Transparent)))
                translate(a, b) { drawPath(shape1Transformed, scheme.surface) }
                translate(b, a) { drawPath(shape2Transformed, scheme.surface) }
            },
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            val todayUsage by remember { networkUsageManager.todayMobileUsage() }.collectAsState(0L)
            val string = DataSize(todayUsage.toDouble()).toStringParts()
            Row {
                Text(
                    text = buildAnnotatedString {
                        withStyle(style = SpanStyle(fontFamily = outfit(700f), fontSize = 48.sp)) {
                            append("${string[0]}.${string[1]}")
                        }
                        withStyle(style = SpanStyle(fontFamily = outfit(700f), fontSize = 32.sp)) {
                            append(string[2])
                        }
                    }
                )
            }
            Text(
                text = stringResource(R.string.mobile_data),
                fontFamily = outfit(),
                fontSize = 20.sp
            )
        }
    }
}

@Composable
private fun RowScope.PredictionCard() {
    val networkUsageManager: NetworkUsageManager = koinInject()
    Column(
        modifier = Modifier
            .card()
            .padding(16.dp)
            .weight(1f),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        val prediction by remember { networkUsageManager.predictUsage() }.collectAsState(0.0)
        val string = DataSize(prediction).toStringParts()

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
                text = string[0] + "." + string[1],
                fontFamily = jetbrainsMono(),
                fontSize = 24.sp
            )
            Text(
                modifier = Modifier.alignByBaseline(),
                text = string[2],
                fontFamily = jetbrainsMono(),
                fontSize = 20.sp
            )
        }
    }
}

@Composable
private fun RowScope.TrendCard() {
    val networkUsageManager: NetworkUsageManager = koinInject()
    val trend by remember { networkUsageManager.getTrend() }.collectAsState(0.0)
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
                text = (if (trend > 0.0) "+" else "") +"${trend.toInt()}%",
                fontFamily = jetbrainsMono(),
                fontSize = 24.sp
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
    categoryTitle { stringResource(label) }
    item {
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
