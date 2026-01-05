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
import androidx.compose.ui.graphics.asComposePath
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.drawText
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.graphics.shapes.RoundedPolygon
import androidx.graphics.shapes.toPath
import com.leekleak.trafficlight.charts.model.ScrollableBarData
import com.leekleak.trafficlight.util.DataSize
import com.leekleak.trafficlight.util.NetworkType
import com.leekleak.trafficlight.util.SizeFormatter
import com.leekleak.trafficlight.util.getName
import java.time.format.TextStyle

internal data class ScrollableBarGraphMetrics(
    val gridHeight: Float,
    val gridWidth: Float,
    val data: List<ScrollableBarData>,
    val rectList: List<Bar>,
    val monthList: List<MonthObject>
)

internal data class MonthObject (
    val name: String,
    var xOffset: Float,
    val visible: Int // -1 off to the left, 0 visible, 1 off to the right
)

internal class ScrollableBarGraphHelper(
    private val scope: DrawScope,
    private val data: List<ScrollableBarData>,
    private val stretch: List<Animatable<Float, *>>,
    private val xOffset: Int = 0,
    private val xItemSpacing: Float = 30f,
    private val maximum: Animatable<Float, *>,
    private val selectorOffset: Float = -1f,
    private val gridColor: Color,
    private val backgroundColor: Color,
    private val onBackgroundColor: Color,
    private val primaryColor: Color,
    private val secondaryColor: Color,
    private val onBarVisibilityChanged: (i: Int, visible: Boolean) -> Unit,
    private val onMaximumChange: (maximum: Long) -> Unit,
) {
    private var sizeFormatter = SizeFormatter()
    private val visibleIndices = mutableListOf<Int>()
    internal val metrics = scope.buildMetrics()

    private fun DrawScope.textPaint(color: Color): Paint {
        return Paint().apply {
            this.color = color.toArgb()
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

        val monthList = mutableListOf<MonthObject>()

        for (i in 0 until data.size) {
            val x1 = xItemSpacing * i + xOffset
            val x2 = x1 + xItemSpacing
            val error = 32.dp.toPx()

            if (data[i].x.dayOfMonth == 1 || i == 0) {
                monthList.add(
                    MonthObject(
                        data[i].x.month.getName(TextStyle.FULL_STANDALONE),
                        x1,
                        if (x1 <= 0) -1 else if (x1 >= size.width) 1 else 0
                    )
                )
            }
            if (x2 >= -error && x1 <= size.width+error) {
                visibleIndices.add(i)
                if (stretch[i].value == 0f) onBarVisibilityChanged(i, true)
            } else if (stretch[i].value == 1f) {
                onBarVisibilityChanged(i, false)
            }
        }

        val absMaxY = visibleIndices.maxOf { data[it].y1 + data[it].y2 }.toLong()
        if (maximum.targetValue.toLong() != absMaxY && absMaxY != 0L) { onMaximumChange(absMaxY) }

        val verticalStep = maximum.value / gridHeight

        rectList.clear()

        val roundedPolygon = RoundedPolygon(3, 12.dp.toPx())
        translate(selectorOffset + xItemSpacing / 2, size.height + 16.dp.toPx()) {
            rotate(-90f, Offset.Zero) {
                drawPath(roundedPolygon.toPath().asComposePath(), color = primaryColor)
            }
        }

        for (i in visibleIndices) {
            val padding = 0.5.dp.toPx()
            val x = xItemSpacing * i + xOffset
            val yOffset1 = data[i].y1.toFloat() / verticalStep
            val yOffset2 = data[i].y2.toFloat() / verticalStep

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
                            top = gridHeight + height1 + height2 + 2 * padding,
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
            data,
            rectList = rectList,
            monthList = monthList
        )
    }

    /**
     * Drawing Grid lines behind the graph on x and y axis
     */
    internal fun drawGrid(textMeasurer: TextMeasurer) {
        scope.run {
            drawLine(
                start = Offset(0f, 0f),
                end = Offset(metrics.gridWidth, 0f),
                color = gridColor,
                strokeWidth = 1.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), -xOffset.toFloat())
            )

            drawLine(
                start = Offset(0f + xOffset, metrics.gridHeight),
                end = Offset(xItemSpacing * data.size + xOffset, metrics.gridHeight),
                color = gridColor,
                strokeWidth = 1.dp.toPx(),
            )

            for (i in 0 until data.size) {
                val x = xItemSpacing * i + xOffset
                val yStart = metrics.gridHeight + if (i % 3 == 0) 12 else 6
                val yEnd = metrics.gridHeight - if (i % 3 == 0) 12 else 6
                drawLine(
                    start = Offset(x, yStart),
                    end = Offset(x, yEnd),
                    color = gridColor,
                    strokeWidth = 1.dp.toPx(),
                )
            }

            drawTextLabelsOverXAndYAxis(gridColor, backgroundColor, textMeasurer)

            //Drawing text labels over the y- axis
            val dataSize = DataSize(maximum.value)
            drawContext.canvas.nativeCanvas.drawText(
                sizeFormatter.format(dataSize.getComparisonValue().getBitValue(), 0, false),
                metrics.gridWidth + 4.sp.toPx(),
                0f + 4.sp.toPx(),
                textPaint(gridColor).apply { textAlign = Paint.Align.LEFT }
            )
        }
    }

    internal fun drawTextLabelsOverXAndYAxis(color: Color, background: Color, textMeasurer: TextMeasurer) {
        scope.run {
            val monthPadding = 4.dp.toPx().toLong()
            for (i in visibleIndices) {
                val xBottomLabel = xItemSpacing * (i + 0.5f)
                if (data[i].x.dayOfWeek.value == 1) {
                    drawRoundRect(
                        color = onBackgroundColor,
                        topLeft = Offset(xItemSpacing * i + xOffset, size.height-12.sp.toPx()),
                        size = Size(xItemSpacing, 16.sp.toPx()),
                        cornerRadius = CornerRadius(8.dp.toPx())
                    )
                    drawContext.canvas.nativeCanvas.drawText(
                        data[i].x.dayOfMonth.toString(),
                        xBottomLabel + xOffset,
                        size.height,
                        textPaint(background)
                    )
                } else {
                    drawContext.canvas.nativeCanvas.drawText(
                        data[i].x.dayOfMonth.toString(),
                        xBottomLabel + xOffset,
                        size.height,
                        textPaint(color)
                    )
                }
            }

            var lastVisibility = 1
            var lastOffset: Float = Float.MAX_VALUE
            for (i in metrics.monthList.size-1 downTo 0) {
                val text = metrics.monthList[i].name
                val result = textMeasurer.measure(text)
                val yOffset = (-result.size.height).toFloat() + 8.dp.toPx()

                val snap = lastVisibility != -1 && metrics.monthList[i].visible == -1
                var xOffset = if (snap && lastOffset != 0f) 0f else metrics.monthList[i].xOffset

                val diffVsLast = xOffset + result.size.width.toFloat() - lastOffset
                if (diffVsLast >= 0) xOffset -= diffVsLast

                lastVisibility = metrics.monthList[i].visible
                lastOffset = xOffset - 6 * monthPadding

                drawLine(
                    start = Offset(xOffset - monthPadding, 0f),
                    end = Offset(xOffset + monthPadding + result.size.width, 0f),
                    color = background,
                    alpha = 1f,
                    strokeWidth = 1.5.dp.toPx(),
                )

                val overlapRatio = -(xOffset - metrics.gridWidth) / result.size.width
                val textOffset = Offset(xOffset, yOffset)
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

            val xPos = xItemSpacing * data.size + xOffset

            drawLine(
                start = Offset(xPos, metrics.gridHeight + 12),
                end = Offset(xPos, metrics.gridHeight - 12),
                color = color,
                alpha = 0.5f,
                strokeWidth = 1.dp.toPx(),
            )
        }
    }
    internal fun drawBars(
        cornerRadius: CornerRadius,
        widths: List<Animatable<Float, *>>
    ) {
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
                drawPath(
                    path = path,
                    color = when(bar.type) {
                        NetworkType.Wifi -> primaryColor
                        NetworkType.Cellular -> secondaryColor
                    }
                )
            }
        }
    }
}