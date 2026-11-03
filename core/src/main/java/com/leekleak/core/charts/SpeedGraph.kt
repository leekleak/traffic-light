package com.leekleak.core.charts

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.unit.dp

@Composable
fun SpeedGraph(
    modifier: Modifier = Modifier,
    data: List<Float>,
    width: Int = 19,
    running: Boolean
) {
    val graphColor by animateColorAsState(if (running) colorScheme.primary else colorScheme.surfaceContainerHigh)
    Canvas(modifier = modifier
        .fillMaxWidth()
        .heightIn(min = 128.dp)
    ) {
        val path = Path().apply {
            val activeData = data.takeLast(width + 1)
            val maximum = activeData.maxOrNull() ?: return@apply
            val normalizedData = activeData.map { it * size.height / maximum }

            val points = normalizedData.indices.map { i ->
                Offset(i.toFloat() * size.width / width, size.height - normalizedData[i])
            }

            moveTo(0f, size.height)
            lineTo(points.first().x, points.first().y)

            for (i in 0 until points.size - 1) {
                val p0 = points[i]
                val p1 = points[i + 1]
                val midX = (p0.x + p1.x) / 2f
                val midY = (p0.y + p1.y) / 2f
                quadraticTo(p0.x, p0.y, midX, midY)
            }

            lineTo(points.last().x, points.last().y)

            lineTo((points.size - 1) * size.width / width, size.height)
            close()
        }
        drawPath(path = path, Brush.verticalGradient(listOf(graphColor, Color.Transparent)))
    }
}
