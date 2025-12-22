package com.leekleak.trafficlight.charts

import android.os.VibrationEffect
import android.os.Vibrator
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.EaseIn
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.rememberScrollableState
import androidx.compose.foundation.gestures.scrollable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.toPath
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableIntState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.leekleak.trafficlight.charts.model.BarData
import com.leekleak.trafficlight.util.px
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.max


@Composable
fun ScrollableBarGraph(
    data: List<BarData>,
    finalGridPoint: String = "24",
    centerLabels: Boolean = false,
) {
    Box(
        modifier = Modifier
            .clip(MaterialTheme.shapes.medium)
            .background(MaterialTheme.colorScheme.background)
    ) {
        ScrollableBarGraphImpl(
            xAxisData = data.map { it.x },
            yAxisData = data.map { Pair(it.y1, it.y2) },
            finalGridPoint = finalGridPoint,
            centerLabels = centerLabels,
        )
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun ScrollableBarGraphImpl(
    xAxisData: List<String>,
    yAxisData: List<Pair<Double, Double>>,
    finalGridPoint: String,
    centerLabels: Boolean,
) {
    val scope = rememberCoroutineScope()
    val vibrator = LocalContext.current.getSystemService(Vibrator::class.java)
    val vibrationEffectStrong = VibrationEffect.createOneShot(80, 200)
    val vibrationEffectMedium = VibrationEffect.createOneShot(40, 100)
    val vibrationEffectWeak = VibrationEffect.createOneShot(40,50)

    val primaryColor = GraphTheme.primaryColor
    val secondaryColor = GraphTheme.secondaryColor
    val onPrimaryColor = GraphTheme.onPrimaryColor
    val onSecondaryColor = GraphTheme.onSecondaryColor
    val gridColor = GraphTheme.gridColor
    val cornerRadius = GraphTheme.cornerRadius

    val shapeWifi = GraphTheme.wifiShape().toPath()
    val shapeCellular = GraphTheme.cellularShape().toPath()
    val iconWifi = GraphTheme.wifiIcon()
    val iconCellular = GraphTheme.cellularIcon()
    val legendSize = 32.dp.px

    val wifiLegendStrength = remember { mutableIntStateOf(5) }
    val cellularLegendStrength = remember { mutableIntStateOf(5) }

    val wifiLegendOffset = animateFloatAsState(
        if (wifiLegendStrength.intValue > 0) 0f else 120.dp.px,
        tween(250, easing = EaseIn)
    )
    val cellularLegendOffset = animateFloatAsState(
        if (cellularLegendStrength.intValue > 0) 0f else 120.dp.px,
        tween(250, easing = EaseIn)
    )

    val wifiAnimation = remember { Animatable(0f) }
    val cellularAnimation = remember { Animatable(0f) }
    val barAnimationSqueeze = remember(yAxisData.size) { List(yAxisData.size * 2) { Animatable(0f) } }
    val barAnimation = remember(yAxisData.size) { List(yAxisData.size) { Animatable(0f) } }
    LaunchedEffect(yAxisData) {
        for (i in 0..<barAnimation.size) {
            launch(Dispatchers.IO) {
                if (yAxisData[i].second + yAxisData[i].first != 0.0) {
                    delay(100)
                    barAnimation[i].animateTo(1f)
                }
            }
        }
    }

    var wifiOffset: Offset = Offset.Zero
    var cellularOffset: Offset = Offset.Zero
    val barOffset = remember { mutableListOf<Bar>() }

    suspend fun legendAnimator(clickOffset: Offset, legendOffset: Offset, animation: Animatable<Float, *>, legendStrength: MutableIntState) {
        if (
            (clickOffset - legendOffset).x in (0f..legendSize) &&
            (clickOffset - legendOffset).y in (0f..legendSize) &&
            legendStrength.intValue != 0
        ) {
            legendStrength.intValue -= 1
            vibrator.vibrate(
                if (legendStrength.intValue == 0) vibrationEffectStrong
                else vibrationEffectMedium
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
            vibrator.vibrate(vibrationEffectWeak)
            launch {
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

    val barWidth = 30.dp.px
    var canvasWidth by remember { mutableFloatStateOf(0f) }
    val offset = remember { Animatable(0f) }
    val scrollableState = rememberScrollableState { delta ->
        val totalOffset = (offset.value + delta).coerceIn(-barWidth * yAxisData.size + canvasWidth, 0f)
        scope.launch {
            offset.snapTo(totalOffset)
        }
        delta
    }

    Canvas(
        modifier = Modifier
            .padding(top = 24.dp, bottom = 14.dp, start = 20.dp, end = 20.dp)
            .height(170.dp)
            .fillMaxWidth()
            .scrollable(scrollableState, Orientation.Horizontal)
            .pointerInput(true) {
                detectTapGestures { offset ->
                    scope.launch {
                        legendAnimator(offset, wifiOffset, wifiAnimation, wifiLegendStrength)
                        legendAnimator(offset, cellularOffset, cellularAnimation, cellularLegendStrength)
                        for (i in 0..<barOffset.size) {
                            barAnimator(offset, barOffset[i], i, barAnimationSqueeze[i])
                        }
                    }
                }
            }
            .onSizeChanged { size ->
                canvasWidth = size.width.toFloat()
            }
    ) {
        val barGraphHelper = ScrollableBarGraphHelper(
            scope = this,
            yAxisData = yAxisData,
            xAxisData = xAxisData,
            finalGridPoint = finalGridPoint,
            stretch = barAnimation,
            xOffset = offset.value.toInt(),
        )

        barOffset.clear()
        barOffset.addAll(barGraphHelper.metrics.rectList)
        barGraphHelper.metrics.rectList
        wifiOffset = barGraphHelper.metrics.wifiIconOffset
        cellularOffset = barGraphHelper.metrics.cellularIconOffset

        barGraphHelper.drawGrid(gridColor)

        barGraphHelper.drawTextLabelsOverXAndYAxis(gridColor, centerLabels)
        barGraphHelper.drawBars(cornerRadius, primaryColor, secondaryColor, barAnimationSqueeze)
    }
}

