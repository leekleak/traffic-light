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
import com.leekleak.trafficlight.util.getName
import java.time.LocalDate
import java.time.format.TextStyle
import java.time.temporal.WeekFields
import java.util.Locale

internal data class ScrollableBarGraphMetrics(
    val gridHeight: Float,
    val gridWidth: Float,
    val data: List<ScrollableBarData>,
    val rectList: List<DoubleBar>,
    val monthList: List<MonthObject>
)

data class DoubleBar (
    val rect: Rect,
    val ratio: Float,
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
    private val padding: Float,
    private val onBarVisibilityChanged: (i: Int, visible: Boolean) -> Unit,
    private val onMaximumChange: (maximum: Long) -> Unit,
) {
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

        val rectList = mutableListOf<DoubleBar>()

        val monthList = mutableListOf<MonthObject>()

        for (i in 0 until data.size) {
            val x1 = xItemSpacing * i + xOffset
            val x2 = x1 + xItemSpacing
            val error = 64.dp.toPx()

            if ((data[i].x.dayOfMonth == 1 || i == 0) && data[i].x != LocalDate.MIN) {
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

        val absMaxY = visibleIndices.maxOf { data[it].y1 + data[it].y2 }
        if (maximum.targetValue.toLong() != absMaxY && absMaxY != 0L) { onMaximumChange(absMaxY) }

        val verticalStep = maximum.value / gridHeight

        val roundedPolygon = RoundedPolygon(3, 12.dp.toPx())
        translate(selectorOffset + xItemSpacing / 2, size.height + 16.dp.toPx()) {
            rotate(-90f, Offset.Zero) {
                drawPath(roundedPolygon.toPath().asComposePath(), color = primaryColor)
            }
        }

        for (i in visibleIndices) {
            val x = xItemSpacing * i + xOffset
            val yOffset = data[i].y.toFloat() / verticalStep

            val barStretch = stretch[i].value
            val ratio = data[i].y1.toFloat() / data[i].y.toFloat()

            rectList.add(
                DoubleBar(
                    Rect(
                        top = gridHeight - yOffset * barStretch,
                        left = x + padding,
                        right = x + xItemSpacing - padding,
                        bottom = gridHeight - padding
                    ),
                    ratio = ratio
                )
            )
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
            val dataSize = DataSize(maximum.value.toLong()).toStringParts()
            drawContext.canvas.nativeCanvas.drawText(
                dataSize.first + " " + dataSize.third,
                metrics.gridWidth + 4.sp.toPx(),
                0f + 4.sp.toPx(),
                textPaint(gridColor).apply { textAlign = Paint.Align.LEFT }
            )
        }
    }

    internal fun drawTextLabelsOverXAndYAxis(color: Color, background: Color, textMeasurer: TextMeasurer) {
        scope.run {
            val monthPadding = 4.dp.toPx().toLong()
            val firstDayValue = WeekFields.of(Locale.getDefault()).firstDayOfWeek.value
            for (i in visibleIndices) {
                val xBottomLabel = xItemSpacing * (i + 0.5f)
                if (data[i].x != LocalDate.MIN) {
                    if (data[i].x.dayOfWeek.value == firstDayValue) {
                        drawRoundRect(
                            color = onBackgroundColor,
                            topLeft = Offset(
                                xItemSpacing * i + xOffset,
                                size.height - 12.sp.toPx()
                            ),
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
            }

            var lastVisibility = 1
            var lastOffset: Float = Float.MAX_VALUE
            for (i in metrics.monthList.size-1 downTo 0) {
                val text = metrics.monthList[i].name
                val result = textMeasurer.measure(text)
                val yOffset = (-result.size.height).toFloat() + 8.dp.toPx()

                val snap = lastVisibility != -1 && metrics.monthList[i].visible == -1
                var xOffset = if (snap && lastOffset != 0f) 0f else metrics.monthList[i].xOffset

                val diffVsLast = xOffset + result.size.width.toFloat() - lastOffset + 16.dp.toPx()
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
    internal fun drawBars(cornerRadius: CornerRadius) {
        scope.run {
            val path1 = Path()
            val path2 = Path()
            metrics.rectList.forEach { doubleBar ->
                if (doubleBar.ratio != 0f) {
                    path1.apply {
                        addRoundRect(
                            RoundRect(
                                rect = doubleBar.rect.copy(
                                    top = doubleBar.rect.bottom - doubleBar.rect.height * doubleBar.ratio + padding,
                                ),
                                cornerRadius = cornerRadius
                            )
                        )
                    }
                }
                if (doubleBar.ratio != 1f) {
                    path2.apply {
                        addRoundRect(
                            RoundRect(
                                rect = doubleBar.rect.copy(
                                    bottom = doubleBar.rect.bottom - doubleBar.rect.height * doubleBar.ratio - padding
                                ),
                                cornerRadius = cornerRadius
                            )
                        )
                    }
                }
            }
            drawPath(
                path = path1,
                color = primaryColor
            )
            drawPath(
                path = path2,
                color = secondaryColor
            )
        }
    }
}