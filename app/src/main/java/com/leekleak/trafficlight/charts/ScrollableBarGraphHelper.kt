package com.leekleak.trafficlight.charts

import android.graphics.Paint
import androidx.compose.animation.core.Animatable
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.leekleak.trafficlight.util.DataSize
import com.leekleak.trafficlight.util.NetworkType
import com.leekleak.trafficlight.util.SizeFormatter
import kotlin.math.max

internal class ScrollableBarGraphHelper(
    private val scope: DrawScope,
    private val yAxisData: List<Pair<Double, Double>>,
    private val xAxisData: List<String>,
    private val finalGridPoint: String,
    private val stretch: List<Animatable<Float, *>>,
    private val xOffset: Int = 0,
) {
    private var sizeFormatter = SizeFormatter()
    internal val metrics = scope.buildMetrics()

    private fun DrawScope.buildMetrics(): BarGraphMetrics {
        val yAxisPadding: Dp = 36.dp
        val paddingBottom: Dp = 20.dp

        val gridHeight = size.height - paddingBottom.toPx()
        val gridWidth = size.width - yAxisPadding.toPx()

        val rectList = mutableListOf<Bar>()

        val absMaxY = max(DataSize(getAbsoluteMax(yAxisData)).getComparisonValue().getBitValue(), 1024)
        val verticalStep = absMaxY / gridHeight

        val xItemSpacing = 30.dp.toPx()

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

        val offsetLeft = gridWidth + 8.dp.toPx()
        val offsetTop2 = gridHeight - 36.dp.toPx()
        val offsetTop1 = gridHeight - 78.dp.toPx()

        val wifiIconOffset = Offset(offsetLeft, offsetTop1)
        val cellularIconOffset = Offset(offsetLeft, offsetTop2)

        return BarGraphMetrics(
            gridHeight = gridHeight,
            gridWidth = gridWidth,
            xItemSpacing = xItemSpacing,
            yAxisData = yAxisData,
            xAxisData = xAxisData,
            rectList = rectList,
            wifiIconOffset = wifiIconOffset,
            cellularIconOffset = cellularIconOffset
        )
    }

    /**
     * Drawing Grid lines behind the graph on x and y axis
     */
    internal fun drawGrid(color: Color) {
        scope.run {
            drawLine(
                start = Offset(0f, 0f),
                end = Offset(metrics.gridWidth, 0f),
                color = color,
                alpha = 0.5f,
                strokeWidth = 1.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f))
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
        }
    }

    internal fun drawTextLabelsOverXAndYAxis(color: Color, centerLabels: Boolean) {
        scope.run {
            val paint = Paint().apply {
                this.color = color.toArgb()
                alpha = 255/2
                textAlign = Paint.Align.CENTER
                textSize = 12.sp.toPx()
            }
            for (i in 0 until yAxisData.size) {
                val xPos = metrics.xItemSpacing * (i + if (centerLabels) 0.5f else 0f)
                drawContext.canvas.nativeCanvas.drawText(
                    xAxisData[i],
                    xPos + xOffset,
                    size.height,
                    paint
                )
            }

            val xPos = metrics.xItemSpacing * yAxisData.size + xOffset
            drawContext.canvas.nativeCanvas.drawText(finalGridPoint, xPos, size.height, paint)

            drawLine(
                start = Offset(xPos, metrics.gridHeight + 12),
                end = Offset(xPos, metrics.gridHeight - 12),
                color = color,
                alpha = 0.5f,
                strokeWidth = 1.dp.toPx(),
            )

            //Drawing text labels over the y- axis
            val dataSize = DataSize(getAbsoluteMax(yAxisData))

            drawContext.canvas.nativeCanvas.drawText(
                sizeFormatter.format(dataSize.getComparisonValue().getBitValue(), 0, false),
                metrics.gridWidth + 4.sp.toPx(),
                0f + 4.sp.toPx(),
                paint.apply { textAlign = Paint.Align.LEFT }
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
    internal fun getAbsoluteMax(list: List<Pair<Number, Number>>): Float {
        return list.maxOfOrNull {
            it.first.toFloat() + it.second.toFloat()
        } ?: 0F
    }
}