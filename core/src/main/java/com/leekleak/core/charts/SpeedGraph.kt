package com.leekleak.core.charts

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.leekleak.core.ui.theme.googleSans
import kotlin.math.sqrt

data class SpeedGraphTag(
    val index: Int,
    val title: String,
    val value: String,
    val alpha: Float = 1f,
)

@Composable
fun SpeedGraph(
    modifier: Modifier = Modifier,
    data: List<Float>,
    width: Int = 19,
    graphColor: Color,
    fillAlpha: Float = 1f,
    tags: List<SpeedGraphTag> = emptyList(),
) {
    val valueColor = colorScheme.onSurface
    val textMeasurer = rememberTextMeasurer()
    val titleStyle = remember { TextStyle(fontFamily = googleSans(weight = 600f), fontSize = 11.sp) }
    val valueStyle = remember { TextStyle(fontFamily = googleSans(weight = 800f), fontSize = 13.sp) }
    val tagTexts = tags.map { Triple(it.index, it.title, it.value) }
    val tagLayouts = remember(tagTexts, titleStyle, valueStyle) {
        tags.associate { tag ->
            tag.index to (textMeasurer.measure(tag.title, titleStyle) to textMeasurer.measure(tag.value, valueStyle))
        }
    }

    Canvas(modifier = modifier
        .fillMaxWidth()
        .heightIn(min = 128.dp)
    ) {
        val activeData = data.takeLast(width + 1)
        val maximum = activeData.maxOrNull() ?: return@Canvas
        if (activeData.size < 2 || maximum <= 0f) return@Canvas
        val normalizedData = activeData.map { it * size.height / maximum }

        val points = normalizedData.indices.map { i ->
            Offset(i.toFloat() * size.width / width, size.height - normalizedData[i])
        }
        val tangents = monotoneTangents(points)

        val path = Path().apply {
            moveTo(0f, size.height)
            lineTo(points.first().x, points.first().y)

            for (i in 0 until points.lastIndex) {
                val third = (points[i + 1].x - points[i].x) / 3f
                cubicTo(
                    points[i].x + third,
                    points[i].y + tangents[i] * third,
                    points[i + 1].x - third,
                    points[i + 1].y - tangents[i + 1] * third,
                    points[i + 1].x,
                    points[i + 1].y
                )
            }

            lineTo(points.last().x, size.height)
            close()
        }
        drawPath(
            path = path,
            brush = Brush.verticalGradient(listOf(graphColor.copy(alpha = fillAlpha), Color.Transparent))
        )

        val dotRadius = 3.dp.toPx()
        val labelGap = 7.dp.toPx()
        val lineGap = 1.dp.toPx()
        val inset = 16.dp.toPx()

        tags.forEach { tag ->
            if (tag.alpha <= 0f) return@forEach
            val point = points.getOrNull(tag.index) ?: return@forEach
            val (title, value) = tagLayouts[tag.index] ?: return@forEach

            drawCircle(color = graphColor, radius = dotRadius, center = point, alpha = tag.alpha)

            val halfWidth = maxOf(title.size.width, value.size.width) / 2f
            val minCenter = halfWidth + inset
            val maxCenter = size.width - halfWidth - inset
            val centerX = if (minCenter <= maxCenter) point.x.coerceIn(minCenter, maxCenter) else size.width / 2f
            val labelHeight = title.size.height + lineGap + value.size.height
            val top = (point.y - labelGap - labelHeight).coerceAtMost(size.height - labelHeight - inset)

            drawText(
                textLayoutResult = title,
                color = valueColor,
                topLeft = Offset(centerX - title.size.width / 2f, top),
                alpha = tag.alpha
            )
            drawText(
                textLayoutResult = value,
                color = valueColor,
                topLeft = Offset(centerX - value.size.width / 2f, top + title.size.height + lineGap),
                alpha = tag.alpha
            )
        }
    }
}

private fun monotoneTangents(points: List<Offset>): FloatArray {
    val slopes = FloatArray(points.lastIndex) { i ->
        (points[i + 1].y - points[i].y) / (points[i + 1].x - points[i].x)
    }
    val tangents = FloatArray(points.size)
    tangents[0] = slopes.first()
    tangents[points.lastIndex] = slopes.last()
    for (i in 1 until points.lastIndex) {
        val previous = slopes[i - 1]
        val next = slopes[i]
        tangents[i] = if (previous * next <= 0f) 0f else 2f * previous * next / (previous + next)
    }
    for (i in slopes.indices) {
        val slope = slopes[i]
        if (slope == 0f) {
            tangents[i] = 0f
            tangents[i + 1] = 0f
            continue
        }
        var alpha = tangents[i] / slope
        var beta = tangents[i + 1] / slope
        if (alpha < 0f) {
            tangents[i] = 0f
            alpha = 0f
        }
        if (beta < 0f) {
            tangents[i + 1] = 0f
            beta = 0f
        }
        val magnitude = alpha * alpha + beta * beta
        if (magnitude > 9f) {
            val tau = 3f / sqrt(magnitude)
            tangents[i] = tau * alpha * slope
            tangents[i + 1] = tau * beta * slope
        }
    }
    return tangents
}
