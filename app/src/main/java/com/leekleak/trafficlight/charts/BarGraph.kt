package com.leekleak.trafficlight.charts

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableIntState
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import com.leekleak.trafficlight.charts.model.BarData
import com.leekleak.trafficlight.ui.theme.LocalSizeMetric
import com.leekleak.trafficlight.util.px
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds


@Composable
fun BarGraph(
    data: List<BarData>,
    onClick: (i: Int) -> Unit = {}
) {
        BarGraphImpl(
            xAxisData = data.map { it.x },
            yAxisData = data.map { Pair(it.y1, it.y2) },
            onClick = onClick
        )
}

@Composable
private fun BarGraphImpl(
    xAxisData: List<String>,
    yAxisData: List<Pair<Long, Long>>,
    onClick: (i: Int) -> Unit
) {
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current

    val primaryColor = GraphTheme.primaryColor
    val secondaryColor = GraphTheme.secondaryColor
    val gridColor = GraphTheme.gridColor
    val cornerRadius = GraphTheme.cornerRadius
    val legendSize = 32.dp.px

    val wifiLegendStrength = remember { mutableIntStateOf(5) }
    val cellularLegendStrength = remember { mutableIntStateOf(5) }

    val wifiAnimation = remember { Animatable(0f) }
    val cellularAnimation = remember { Animatable(0f) }
    val barAnimationSqueeze = remember(yAxisData.size) { List(yAxisData.size * 2) { Animatable(0f) } }
    val barAnimation = remember(yAxisData.size) { List(yAxisData.size) { Animatable(0f) } }
    LaunchedEffect(yAxisData) {
        for (i in barAnimation.indices) {
            launch {
                if (yAxisData[i].second + yAxisData[i].first != 0L) {
                    delay(100.milliseconds)
                    barAnimation[i].animateTo(1f)
                }
            }
        }
    }

    var wifiOffset: Offset = Offset.Zero
    var cellularOffset: Offset = Offset.Zero
    val barOffset = remember { mutableListOf<Bar>() }

    val metric = LocalSizeMetric.current

    suspend fun legendAnimator(clickOffset: Offset, legendOffset: Offset, animation: Animatable<Float, *>, legendStrength: MutableIntState) {
        if (
            (clickOffset - legendOffset).x in (0f..legendSize) &&
            (clickOffset - legendOffset).y in (0f..legendSize) &&
            legendStrength.intValue != 0
        ) {
            legendStrength.intValue -= 1
            haptic.performHapticFeedback(
                if (legendStrength.intValue == 0) HapticFeedbackType.LongPress
                else HapticFeedbackType.ContextClick
            )
            animation.animateTo(
                targetValue = if (animation.targetValue == 15f) 0f else 15f,
                animationSpec = tween(150)
            )
        }
    }

    fun CoroutineScope.barAnimator(clickOffset: Offset, bar: Bar, i: Int, animation: Animatable<Float, *>) {
        if (
            (clickOffset - bar.rect.topLeft).x in (0f..bar.rect.size.width) &&
            (clickOffset - bar.rect.topLeft).y in (0f..bar.rect.size.height)
        ) {
            haptic.performHapticFeedback(HapticFeedbackType.ContextClick)
            launch {
                onClick(i)
                animation.animateTo(
                    targetValue = 8f,
                    animationSpec = tween(150)
                )
                animation.animateTo(
                    targetValue = 0f,
                    animationSpec = tween(150)
                )
            }
        }
    }

    Canvas(
        modifier = Modifier
            .padding(top = 24.dp, bottom = 14.dp, start = 20.dp, end = 20.dp)
            .height(170.dp)
            .fillMaxWidth()
            .pointerInput(true) {
                detectTapGestures { offset ->
                    scope.launch {
                        legendAnimator(offset, wifiOffset, wifiAnimation, wifiLegendStrength)
                        legendAnimator(offset, cellularOffset, cellularAnimation, cellularLegendStrength)
                        for (i in barOffset.indices) {
                            barAnimator(offset, barOffset[i], i, barAnimationSqueeze[i])
                        }
                    }
                }
            }
    ) {
        val barGraphHelper = BarGraphHelper(
            scope = this,
            yAxisData = yAxisData,
            xAxisData = xAxisData,
            stretch = barAnimation,
            metric = metric
        )

        barOffset.clear()
        barOffset.addAll(barGraphHelper.metrics.rectList)
        barGraphHelper.metrics.rectList
        wifiOffset = barGraphHelper.metrics.wifiIconOffset
        cellularOffset = barGraphHelper.metrics.cellularIconOffset

        barGraphHelper.drawGrid(gridColor)

        barGraphHelper.drawTextLabelsOverXAndYAxis(gridColor)
        barGraphHelper.drawBars(cornerRadius, primaryColor, secondaryColor, barAnimationSqueeze)
    }
}

