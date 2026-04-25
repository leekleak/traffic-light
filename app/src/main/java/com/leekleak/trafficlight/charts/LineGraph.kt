package com.leekleak.trafficlight.charts

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.leekleak.trafficlight.ui.theme.historyItemFont
import com.leekleak.trafficlight.util.DataSize
import com.leekleak.trafficlight.util.px
import kotlin.math.max
import kotlin.math.min

@Composable
fun LineGraph(
    maximum: Long,
    data: Pair<Long?, Long?>,
) {
    val fontSize = 20.sp
    val textPadding = 4.dp.px

    val primaryColor = GraphTheme.primaryColor
    val secondaryColor = GraphTheme.secondaryColor
    val onPrimaryColor = GraphTheme.onPrimaryColor
    val onSecondaryColor = GraphTheme.onSecondaryColor
    val onBackgroundColor = GraphTheme.onBackgroundColor

    val textMeasurer = rememberTextMeasurer()
    val font = remember { historyItemFont() }

    val safeMax = max(maximum, 1).toFloat()

    Canvas(modifier = Modifier
        .fillMaxWidth()
        .height(32.dp)
        .clip(MaterialTheme.shapes.extraSmall)
    ) {
        /**
         * Bars
         */
        data.first?.let { data1 ->
            val width1 = data1.toFloat() / safeMax * size.width
            if (data.first != 0L) {
                drawRect(
                    color = primaryColor,
                    size = Size(width1 - min(width1, 0.5.dp.toPx()), size.height)
                )
            }
        }
        data.second?.let { data2 ->
            val width2 = data2.toFloat() / safeMax * size.width
            if (data.second != 0L) {
                drawRect(
                    color = secondaryColor,
                    topLeft = Offset(size.width - width2 + 0.5.dp.toPx(), 0f),
                    size = Size(width2 - 0.5.dp.toPx() , size.height)
                )
            }
        }

        val paddingRatio = textPadding / size.width
        val point1 = (data.first?.toFloat() ?: 0f) / safeMax - 0.5.dp.toPx() / size.width
        val point2 = (safeMax - (data.second?.toFloat() ?: 0f)) / safeMax - 0.5.dp.toPx() / size.width
        /**
         * Wifi text
         */
        data.first?.let { data1 ->
            val textMeasure1 = textMeasurer.measure(
                DataSize(data1).toString(extraPrecision = true),
                TextStyle(
                    fontFamily = font,
                    fontSize = fontSize,
                )
            )
            val brush1 = Brush.horizontalGradient(
                0f to onPrimaryColor,
                point1 - paddingRatio to onPrimaryColor,
                point1 - paddingRatio to onBackgroundColor,
                point2 - paddingRatio to onBackgroundColor,
                point2 - paddingRatio to onSecondaryColor,
                startX = 0f,
                endX = size.width
            )
            drawText(
                topLeft = Offset(textPadding, (size.height - textMeasure1.size.height) / 2),
                textLayoutResult = textMeasure1,
                brush = brush1,
            )
        }

        /**
         * Mobile text
         */
        data.second?.let { data2 ->
            val textMeasure2 = textMeasurer.measure(
                DataSize(data2).toString(extraPrecision = true),
                TextStyle(
                    fontFamily = font,
                    fontSize = fontSize,
                )
            )
            val brush2 = Brush.horizontalGradient(
                0f to onPrimaryColor,
                point1 + paddingRatio to onPrimaryColor,
                point1 + paddingRatio to onBackgroundColor,
                point2 + paddingRatio to onBackgroundColor,
                point2 + paddingRatio to onSecondaryColor,
                startX = textMeasure2.size.width - size.width,
                endX = textMeasure2.size.width.toFloat()
            )
            drawText(
                topLeft = Offset(
                    size.width - textMeasure2.size.width - textPadding,
                    (size.height - textMeasure2.size.height) / 2
                ),
                textLayoutResult = textMeasure2,
                brush = brush2,
            )
        }
    }
}
