package com.leekleak.trafficlight.charts

import android.graphics.Paint
import androidx.compose.animation.core.Animatable
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.drawText
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.leekleak.trafficlight.util.DataSize
import com.leekleak.trafficlight.util.NetworkType
import com.leekleak.trafficlight.util.SizeFormatter
import com.leekleak.trafficlight.util.getName
import java.time.LocalDate
import java.time.format.TextStyle

internal data class ScrollableBarGraphMetrics(
    val gridHeight: Float,
    val gridWidth: Float,
    val xItemSpacing: Float,
    val yAxisData: List<Pair<Double, Double>>,
    val xAxisData: List<LocalDate>,
    val rectList: List<Bar>
)

internal class ScrollableBarGraphHelper(
    private val scope: DrawScope,
    private val yAxisData: List<Pair<Double, Double>>,
    private val xAxisData: List<LocalDate>,
    private val stretch: List<Animatable<Float, *>>,
    private val xOffset: Int = 0,
    private val maximum: Animatable<Float, *>,
    val onBarVisibilityChanged: (i: Int, visible: Boolean) -> Unit,
    val onMaximumChange: (maximum: Long) -> Unit
) {
    private var sizeFormatter = SizeFormatter()
    internal val metrics = scope.buildMetrics()

    private fun DrawScope.textPaint(color: Color): Paint {
        return Paint().apply {
            this.color = color.toArgb()
            alpha = 255/2
            textAlign = Paint.Align.CENTER
            textSize = 12.sp.toPx()
        }
    }
    private fun DrawScope.buildMetrics(): ScrollableBarGraphMetrics {
        val yAxisPadding: Dp = 36.dp
        val paddingBottom: Dp = 20.dp

        val gridHeight = size.height - paddingBottom.toPx()
        val gridWidth = size.width - yAxisPadding.toPx()

        val rectList = mutableListOf<Bar>()
        val xItemSpacing = 30.dp.toPx()

        val visibleIndices = mutableListOf<Int>()

        for (i in 0 until yAxisData.size) {
            val x1 = xItemSpacing * i + xOffset
            val x2 = x1 + xItemSpacing
            val error = 32.dp.toPx()

            if (x1 in (-error)..(size.width+error) || x2 in (-error)..(size.width+error)) {
                visibleIndices.add(i)
                if (stretch[i].value == 0f) onBarVisibilityChanged(i, true)
            } else if (stretch[i].value == 1f) onBarVisibilityChanged(i, false)
        }

        val absMaxY = visibleIndices.maxOf { yAxisData[it].first + yAxisData[it].second }.toLong()
        if (maximum.targetValue.toLong() != absMaxY && absMaxY != 0L) { onMaximumChange(absMaxY) }

        val verticalStep = maximum.value / gridHeight

        rectList.clear()
        for (i in 0 until yAxisData.size) {
            val padding = 0.5.dp.toPx()
            val x = xItemSpacing * i + xOffset
            val yOffset1 = yAxisData[i].first.toFloat() / verticalStep
            val yOffset2 = yAxisData[i].second.toFloat() / verticalStep

            val barStretch = stretch[i].value
            val height1 = if (-yOffset1 * barStretch < -3) -yOffset1 * barStretch else 0f
            val height2 = if (-yOffset2 * barStretch < -3) -yOffset2 * barStretch else 0f

            if (height1 != 0f) {
                rectList.add(
                    Bar(
                        rect = Rect(
                            top = gridHeight + height1 + padding,
                            left = x + padding,
                            right = x + xItemSpacing - padding,
                            bottom = gridHeight - padding
                        ),
                        type = NetworkType.Cellular
                    )
                )
            }
            if (height2 != 0f) {
                rectList.add(
                    Bar(
                        rect = Rect(
                            top = gridHeight + height1 + height2,
                            left = x + padding,
                            right = x + xItemSpacing - padding,
                            bottom = gridHeight + height1 - padding
                        ),
                        type = NetworkType.Wifi
                    )
                )
            }
        }

        return ScrollableBarGraphMetrics(
            gridHeight = gridHeight,
            gridWidth = gridWidth,
            xItemSpacing = xItemSpacing,
            yAxisData = yAxisData,
            xAxisData = xAxisData,
            rectList = rectList,
        )
    }

    /**
     * Drawing Grid lines behind the graph on x and y axis
     */
    internal fun drawGrid(color: Color, background: Color, textMeasurer: TextMeasurer) {
        scope.run {
            val padding = 24.dp.toPx()
            drawLine(
                start = Offset(-padding, 0f),
                end = Offset(metrics.gridWidth, 0f),
                color = color,
                alpha = 0.5f,
                strokeWidth = 1.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), -xOffset.toFloat())
            )

            drawLine(
                start = Offset(0f + xOffset, metrics.gridHeight),
                end = Offset(metrics.xItemSpacing * yAxisData.size + xOffset, metrics.gridHeight),
                color = color,
                alpha = 0.5f,
                strokeWidth = 1.dp.toPx(),
            )

            for (i in 0 until yAxisData.size) {
                val x = metrics.xItemSpacing * i + xOffset
                val yStart = metrics.gridHeight + if (i % 3 == 0) 12 else 6
                val yEnd = metrics.gridHeight - if (i % 3 == 0) 12 else 6
                drawLine(
                    start = Offset(x, yStart),
                    end = Offset(x, yEnd),
                    color = color,
                    alpha = 0.5f,
                    strokeWidth = 1.dp.toPx(),
                )
            }

            drawTextLabelsOverXAndYAxis(color, background, textMeasurer)

            val brush1 = Brush.horizontalGradient(listOf(background, Color.Transparent), -padding, 0f)
            drawRect(brush1, Offset(-padding, -padding), Size(padding, size.height + 2 * padding))

            val brush2 = Brush.horizontalGradient(listOf(Color.Transparent, background), size.width, size.width + padding)
            drawRect(brush2, Offset(size.width, -padding), Size(padding, size.height + 2 * padding))

            //Drawing text labels over the y- axis
            val dataSize = DataSize(maximum.value)
            drawContext.canvas.nativeCanvas.drawText(
                sizeFormatter.format(dataSize.getComparisonValue().getBitValue(), 0, false),
                metrics.gridWidth + 4.sp.toPx(),
                0f + 4.sp.toPx(),
                textPaint(color).apply { textAlign = Paint.Align.LEFT }
            )
        }
    }

    internal fun drawTextLabelsOverXAndYAxis(color: Color, background: Color, textMeasurer: TextMeasurer) {
        scope.run {
            val monthPadding = 4.dp.toPx().toLong()
            for (i in 0 until yAxisData.size) {
                val xTopLabel = metrics.xItemSpacing * i
                val xBottomLabel = metrics.xItemSpacing * (i + 0.5f)
                drawContext.canvas.nativeCanvas.drawText(
                    xAxisData[i].dayOfMonth.toString(),
                    xBottomLabel + xOffset,
                    size.height,
                    textPaint(color)
                )
                if (xAxisData[i].dayOfMonth == 1 || i == 0) {
                    val text = xAxisData[i].month.getName(TextStyle.FULL_STANDALONE)
                    val result = textMeasurer.measure(text)
                    val yOffset = (-result.size.height).toFloat() + 8.dp.toPx()

                    val backgroundOffset = Offset(xTopLabel + xOffset - monthPadding, yOffset)
                    val backgroundSize = Size(result.size.width + 2 * monthPadding.toFloat(), result.size.height.toFloat())
                    val backgroundBrush = Brush.verticalGradient(
                        0.6f to background, 1f to Color.Transparent,
                        startY = yOffset,
                        endY = yOffset + backgroundSize.height
                    )
                    drawRect(backgroundBrush, backgroundOffset, backgroundSize)

                    val overlapRatio = -(xTopLabel + xOffset - metrics.gridWidth) / result.size.width
                    val textOffset = Offset(xTopLabel + xOffset, yOffset)
                    if (overlapRatio in 0f..1f) {
                        val colorArray = mutableListOf(0f to color)
                        colorArray.add(overlapRatio - 0.1f to color)
                        colorArray.add(overlapRatio to background)
                        val textBrush = Brush.horizontalGradient(
                            0f to color, overlapRatio - 0.1f to color, overlapRatio to background,
                            startX = 0f,
                            endX = result.size.width.toFloat(),
                        )
                        drawText(result, textBrush, textOffset)
                    }
                    else {
                        val color = if (overlapRatio > 1f) color else background
                        drawText(result, color, textOffset)
                    }
                }
            }

            val xPos = metrics.xItemSpacing * yAxisData.size + xOffset

            drawLine(
                start = Offset(xPos, metrics.gridHeight + 12),
                end = Offset(xPos, metrics.gridHeight - 12),
                color = color,
                alpha = 0.5f,
                strokeWidth = 1.dp.toPx(),
            )
        }
    }
    internal fun drawBars(cornerRadius: CornerRadius, color1: Color, color2: Color, widths: List<Animatable<Float, *>>) {
        scope.run {
            metrics.rectList.forEachIndexed { i, bar ->
                val path = Path().apply {
                    addRoundRect(
                        RoundRect(
                            rect = bar.rect.copy(
                                left = bar.rect.left + widths[i].value,
                                right = bar.rect.right - widths[i].value,
                            ),
                            cornerRadius = cornerRadius
                        )
                    )
                }
                drawPath(path, if (bar.type == NetworkType.Wifi) color1 else color2)
            }
        }
    }
}