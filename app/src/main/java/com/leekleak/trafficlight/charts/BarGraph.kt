package com.leekleak.trafficlight.charts

import android.os.VibrationEffect
import android.os.Vibrator
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.EaseIn
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableIntState
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.leekleak.trafficlight.charts.model.BarData
import com.leekleak.trafficlight.util.px
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch


@Composable
fun BarGraph(
    data: List<BarData>,
    finalGridPoint: String = "24",
    centerLabels: Boolean = false
) {
    Box(
        modifier = Modifier
            .clip(MaterialTheme.shapes.medium)
            .background(MaterialTheme.colorScheme.background)
    ) {
        BarGraphImpl(
            xAxisData = data.map { it.x },
            yAxisData = data.map { Pair(it.y1, it.y2) },
            finalGridPoint = finalGridPoint,
            centerLabels = centerLabels
        )
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun BarGraphImpl(
    xAxisData: List<String>,
    yAxisData: List<Pair<Double, Double>>,
    finalGridPoint: String,
    centerLabels: Boolean
) {
    val scope = rememberCoroutineScope()
    val vibrator = LocalContext.current.getSystemService(Vibrator::class.java)
    val vibrationEffectStrong = VibrationEffect.createOneShot(80, 200)
    val vibrationEffectMedium = VibrationEffect.createOneShot(40, 100)
    val vibrationEffectWeak = VibrationEffect.createOneShot(40,50)

    val backgroundColor = GraphTheme.backgroundColor

    val primaryColor = GraphTheme.primaryColor
    val secondaryColor = GraphTheme.secondaryColor
    val onPrimaryColor = GraphTheme.onPrimaryColor
    val onSecondaryColor = GraphTheme.onSecondaryColor
    val gridColor = GraphTheme.gridColor
    val cornerRadius = GraphTheme.cornerRadius

    val shapeWifi = GraphTheme.wifiShape()
    val shapeCellular = GraphTheme.cellularShape()
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
    val barAnimation = remember { List(yAxisData.size * 2) {Animatable(0f)} }

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

    fun CoroutineScope.barAnimator(clickOffset: Offset, bar: Bar, animation: Animatable<Float, *>) {
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

    Canvas(
        modifier = Modifier
            .background(backgroundColor)
            .padding(top = 24.dp, bottom = 14.dp, start = 20.dp, end = 20.dp)
            .height(170.dp)
            .fillMaxWidth()
            .pointerInput(true) {
                detectTapGestures { offset ->
                    scope.launch {
                        legendAnimator(offset, wifiOffset, wifiAnimation, wifiLegendStrength)
                        legendAnimator(offset, cellularOffset, cellularAnimation, cellularLegendStrength)
                        for (i in 0..<barOffset.size) {
                            barAnimator(offset, barOffset[i], barAnimation[i])
                        }
                    }
                }
            }
    ) {
        val barGraphHelper = BarGraphHelper(
            scope = this,
            yAxisData = yAxisData,
            xAxisData = xAxisData,
            finalGridPoint = finalGridPoint
        )

        barOffset.clear()
        barOffset.addAll(barGraphHelper.metrics.rectList)
        barGraphHelper.metrics.rectList
        wifiOffset = barGraphHelper.metrics.wifiIconOffset
        cellularOffset = barGraphHelper.metrics.cellularIconOffset

        barGraphHelper.drawGrid(gridColor)

        barGraphHelper.drawLegend(
            barGraphHelper.metrics.wifiIconOffset.copy(
                y = barGraphHelper.metrics.wifiIconOffset.y + wifiLegendOffset.value
            ),
            primaryColor,
            shapeWifi,
            iconWifi,
            onPrimaryColor,
            wifiAnimation.value,
        )

        barGraphHelper.drawLegend(
            barGraphHelper.metrics.cellularIconOffset.copy(
                y = barGraphHelper.metrics.cellularIconOffset.y + cellularLegendOffset.value
            ),
            secondaryColor,
            shapeCellular,
            iconCellular,
            onSecondaryColor,
            cellularAnimation.value,
        )

        barGraphHelper.drawTextLabelsOverXAndYAxis(gridColor, centerLabels)
        barGraphHelper.drawBars(cornerRadius, primaryColor, secondaryColor, barAnimation)
    }
}

