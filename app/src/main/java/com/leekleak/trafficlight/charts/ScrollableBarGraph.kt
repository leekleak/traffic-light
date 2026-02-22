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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
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
import com.leekleak.trafficlight.util.DataSize
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.math.sign

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
    val onBackgroundColor = GraphTheme.onBackgroundColor
    val gridColor = GraphTheme.gridColor
    val cornerRadius = GraphTheme.cornerRadius

    val barAnimationSqueeze = remember(data.size) { List(data.size * 2) { Animatable(0f) } }
    val barAnimation = remember(data.size) { List(data.size) { Animatable(0f) } }
    val barOffset = remember { mutableListOf<Bar>() }
    var selectorOffset by remember { mutableFloatStateOf(0f) }
    val selectorOffsetSnapped = remember { Animatable(0f) }

    var canvasWidth by remember { mutableFloatStateOf(1f) }
    val barWidth by remember { derivedStateOf { canvasWidth / 11f } }
    val offset = remember(canvasWidth) { Animatable(-barWidth * data.size + canvasWidth) }

    val selectorGoal = (canvasWidth)/2 - ((canvasWidth)/2) % barWidth

    LaunchedEffect(canvasWidth) {
        selectorOffset = canvasWidth - 1f
        selectorOffsetSnapped.snapTo(selectorOffset - selectorOffset % barWidth)
    }

    var selectorIndex by remember { mutableIntStateOf(0) }
    LaunchedEffect(selectorOffsetSnapped.value, offset.value) {
        val currentSelected = ((selectorOffsetSnapped.value - offset.value)/barWidth).roundToInt()
        if (currentSelected != selectorIndex) {
            selectorIndex = currentSelected
            if (selectorIndex in -1..data.size) {
                haptic.performHapticFeedback(HapticFeedbackType.SegmentFrequentTick)
            }
        }
        onSelect(((selectorOffsetSnapped.targetValue - offset.targetValue)/barWidth).roundToInt())
    }

    LaunchedEffect(selectorOffset) {
        if (selectorOffsetSnapped.targetValue != (selectorOffset - selectorOffset % barWidth)) {
            scope.launch {
                selectorOffsetSnapped.animateTo(
                    targetValue = selectorOffset - selectorOffset % barWidth,
                    initialVelocity = 200f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioLowBouncy,
                        stiffness = Spring.StiffnessMedium
                    )
                )
            }
        }
    }

    fun CoroutineScope.barAnimator(clickOffset: Offset, bar: Bar, i: Int) {
        if (
            (clickOffset - bar.rect.topLeft).x in (0f..bar.rect.size.width) &&
            (clickOffset - bar.rect.topLeft).y in (0f..bar.rect.size.height)
        ) {
            haptic.performHapticFeedback(HapticFeedbackType.ContextClick)
            launch {
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

    val maximum = remember(data) { Animatable(data.maxOf { it.y1 + it.y2 }.toFloat()) }

    val scrollableState = rememberScrollableState { delta ->
        if (offset.value !in canvasWidth - barWidth * data.size..0f) return@rememberScrollableState 0f
        var totalOffset = (offset.value + delta).coerceIn(canvasWidth - barWidth * data.size, 0f)
        var selectorOff = selectorOffset
        if (selectorOffset * delta.sign > selectorGoal * delta.sign) {
            totalOffset = offset.value
            val threshold = (selectorOff - delta) * delta.sign < selectorGoal * delta.sign
            selectorOff = if (!threshold) (selectorOff - delta) else selectorGoal
        } else if (totalOffset != offset.value + delta) {
            totalOffset = offset.value
            selectorOff -= delta
        }

        selectorOffset = selectorOff.coerceIn(0f, canvasWidth - 1f) // A bit of a workaround to ensure the selector doesn't go too far

        scope.launch {
            offset.snapTo(totalOffset)
        }
        return@rememberScrollableState delta
    }

    val snapFlingBehavior = object : FlingBehavior {
        val decaySpec = exponentialDecay<Float>()
        override suspend fun ScrollScope.performFling(initialVelocity: Float): Float {
            val targetValue = if (abs(initialVelocity) < 500) {
                offset.value
            } else {
                decaySpec.calculateTargetValue(
                    initialValue = offset.value,
                    initialVelocity = initialVelocity
                )
            }
            val snappedTarget = ((targetValue / barWidth).roundToInt() * barWidth).coerceIn(-barWidth * data.size + canvasWidth, 0f)

            if (targetValue != offset.value && initialVelocity.sign == (selectorOffset - selectorGoal).sign) {
                selectorOffset = selectorGoal
            } else if (selectorOffset == selectorGoal || snappedTarget != targetValue - targetValue % barWidth) {
                scope.launch {
                    offset.animateTo(
                        targetValue = snappedTarget,
                        initialVelocity = initialVelocity,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioLowBouncy,
                            stiffness = Spring.StiffnessMediumLow
                        )
                    )
                }
            }
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
            xItemSpacing = barWidth,
            maximum = maximum,
            selectorOffset = selectorOffsetSnapped.value,
            gridColor = gridColor,
            backgroundColor = backgroundColor,
            onBackgroundColor = onBackgroundColor,
            primaryColor = primaryColor,
            secondaryColor = secondaryColor,
            onBarVisibilityChanged = { i, visible ->
                if (visible) scope.launch { barAnimation[i].animateTo(1f) }
                else scope.launch { barAnimation[i].snapTo(0f) }
            },
            onMaximumChange = {
                new -> scope.launch {
                    maximum.animateTo(DataSize(new.toDouble()).getComparisonValue().getBitValue().toFloat())
                }
            }
        )

        barOffset.clear()
        barOffset.addAll(barGraphHelper.metrics.rectList)
        barGraphHelper.drawBars(cornerRadius, barAnimationSqueeze)
        barGraphHelper.drawGrid(textMeasurer)
    }
}

