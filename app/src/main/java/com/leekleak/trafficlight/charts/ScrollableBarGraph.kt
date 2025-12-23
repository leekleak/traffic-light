package com.leekleak.trafficlight.charts

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.calculateTargetValue
import androidx.compose.animation.core.exponentialDecay
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.FlingBehavior
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.ScrollScope
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.rememberScrollableState
import androidx.compose.foundation.gestures.scrollable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import com.leekleak.trafficlight.charts.model.ScrollableBarData
import com.leekleak.trafficlight.util.px
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ScrollableBarGraph(
    data: List<ScrollableBarData>,
    onSelect: (i: Int) -> Unit,
) {
    val scope = rememberCoroutineScope()
    val textMeasurer = rememberTextMeasurer()
    val haptic = LocalHapticFeedback.current

    val primaryColor = GraphTheme.primaryColor
    val secondaryColor = GraphTheme.secondaryColor
    val backgroundColor = GraphTheme.backgroundColor
    val selectorColor = GraphTheme.selectorColor
    val gridColor = GraphTheme.gridColor
    val cornerRadius = GraphTheme.cornerRadius

    val barAnimationSqueeze = remember(data.size) { List(data.size * 2) { Animatable(0f) } }
    val barAnimation = remember(data.size) { List(data.size) { Animatable(0f) } }
    val barOffset = remember { mutableListOf<Bar>() }
    var selected by remember { mutableIntStateOf(-1) }
    var selectorOffset = remember { Animatable(0f) }

    val barWidth = 30.dp.px
    val offset = remember { Animatable(0f) }

    fun CoroutineScope.barAnimator(clickOffset: Offset, bar: Bar, i: Int) {
        if (
            (clickOffset - bar.rect.topLeft).x in (0f..bar.rect.size.width) &&
            (clickOffset - bar.rect.topLeft).y in (0f..bar.rect.size.height)
        ) {
            haptic.performHapticFeedback(HapticFeedbackType.ContextClick)
            selected = ((bar.rect.topLeft.x-offset.value)/barWidth).roundToInt()
            onSelect(selected)
            launch {
                selectorOffset.animateTo(barWidth * selected)
                barAnimationSqueeze[i].animateTo(
                    targetValue = 8f,
                    animationSpec = tween(150)
                )
                barAnimationSqueeze[i].animateTo(
                    targetValue = 0f,
                    animationSpec = tween(150)
                )
            }
        }
    }

    var canvasWidth by remember { mutableFloatStateOf(0f) }
    val maximum = remember(data) { Animatable(data.maxOf { it.y1 + it.y2 }.toFloat()) }

    val scrollableState = rememberScrollableState { delta ->
        val totalOffset = (offset.value + delta).coerceIn(-barWidth * data.size + canvasWidth, 0f)
        scope.launch {
            offset.snapTo(totalOffset)
        }
        delta
    }

    val snapFlingBehavior = object : FlingBehavior {
        val decaySpec = exponentialDecay<Float>()
        override suspend fun ScrollScope.performFling(initialVelocity: Float): Float {
            val targetValue = decaySpec.calculateTargetValue(
                initialValue = offset.value,
                initialVelocity = initialVelocity
            )
            val snappedTarget = (targetValue - targetValue % barWidth).coerceIn(-barWidth * data.size + canvasWidth, 0f)
            offset.animateTo(
                targetValue = snappedTarget,
                initialVelocity = initialVelocity,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioLowBouncy,
                    stiffness = Spring.StiffnessMediumLow
                )
            )
            return 0f
        }
    }

    Canvas(
        modifier = Modifier
            .padding(top = 24.dp, bottom = 14.dp, start = 20.dp, end = 20.dp)
            .height(180.dp)
            .fillMaxWidth()
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    scope.launch {
                        for (i in 0..<barOffset.size) {
                            barAnimator(offset, barOffset[i], i)
                        }
                    }
                }
            }
            .scrollable(scrollableState, Orientation.Horizontal, flingBehavior = snapFlingBehavior)
            .onSizeChanged { size ->
                canvasWidth = size.width.toFloat()
            }
    ) {
        val barGraphHelper = ScrollableBarGraphHelper(
            scope = this,
            data = data,
            stretch = barAnimation,
            xOffset = offset.value.toInt(),
            maximum = maximum,
            selectorOffset = selectorOffset.value,
            onBarVisibilityChanged = { i, visible ->
                if (visible) scope.launch { barAnimation[i].animateTo(1f) }
                else scope.launch { barAnimation[i].snapTo(0f) }
            },
            onMaximumChange = {
                new -> scope.launch { maximum.animateTo(new.toFloat()) }
            }
        )

        barOffset.clear()
        barOffset.addAll(barGraphHelper.metrics.rectList)
        barGraphHelper.metrics.rectList
        barGraphHelper.drawBars(cornerRadius, primaryColor, secondaryColor, selectorColor, barAnimationSqueeze)
        barGraphHelper.drawGrid(gridColor, backgroundColor, textMeasurer)
    }
}

