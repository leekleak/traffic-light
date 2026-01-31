package com.leekleak.trafficlight.charts

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.leekleak.trafficlight.R
import com.leekleak.trafficlight.util.DataSize
import com.leekleak.trafficlight.util.px

@Composable
fun LineGraph(
    maximum: Long,
    data: Pair<Long, Long>,
) {
    val fontSize = 20.sp
    val textPadding = 4.dp.px

    val primaryColor = GraphTheme.primaryColor
    val secondaryColor = GraphTheme.secondaryColor
    val onPrimaryColor = GraphTheme.onPrimaryColor
    val onSecondaryColor = GraphTheme.onSecondaryColor
    val onBackgroundColor = GraphTheme.onBackgroundColor

    val textMeasurer = rememberTextMeasurer()
    val font = momoTrustDisplayFont()

    Canvas(modifier = Modifier
        .fillMaxWidth()
        .height(32.dp)
        .clip(MaterialTheme.shapes.extraSmall)
    ) {
        /**
         * Bars
         */
        val width1 = data.first.toFloat() / maximum * size.width
        val width2 = data.second.toFloat() / maximum * size.width
        drawRect(
            color = primaryColor,
            size = Size(width1, size.height)
        )
        drawRect(
            color = secondaryColor,
            topLeft = Offset(size.width-width2, 0f),
            size = Size(width2, size.height)
        )

        /**
         * Wifi text
         */
        val textMeasure1 = textMeasurer.measure(
            DataSize(data.first.toDouble()).toString(),
            TextStyle(
                fontFamily = font,
                fontSize = fontSize,
            )
        )

        val paddingRatio = textPadding / size.width
        val brush1 = Brush.horizontalGradient(
            0f to onPrimaryColor,
            data.first.toFloat() / maximum - paddingRatio to onPrimaryColor,
            data.first.toFloat() / maximum - paddingRatio to onBackgroundColor,
            (maximum - data.second.toFloat()) / maximum - paddingRatio to onBackgroundColor,
            (maximum - data.second.toFloat()) / maximum - paddingRatio to onSecondaryColor,
            startX = 0f,
            endX = size.width
        )

        drawText(
            topLeft = Offset(textPadding, (size.height - textMeasure1.size.height) / 2),
            textLayoutResult = textMeasure1,
            brush = brush1,
        )

        /**
         * Mobile text
         */
        val textMeasure2 = textMeasurer.measure(
            DataSize(data.second.toDouble()).toString(),
            TextStyle(
                fontFamily = font,
                fontSize = fontSize,
                brush = brush1
            )
        )
        val brush2 = Brush.horizontalGradient(
            0f to onPrimaryColor,
            data.first.toFloat() / maximum + paddingRatio to onPrimaryColor,
            data.first.toFloat() / maximum + paddingRatio to onBackgroundColor,
            (maximum - data.second.toFloat()) / maximum + paddingRatio to onBackgroundColor,
            (maximum - data.second.toFloat()) / maximum + paddingRatio to onSecondaryColor,
            startX = textMeasure2.size.width - size.width,
            endX = textMeasure2.size.width.toFloat()
        )
        drawText(
            topLeft = Offset(
                size.width - textMeasure2.size.width - textPadding,
                (size.height - textMeasure1.size.height) / 2
            ),
            textLayoutResult = textMeasure2,
            brush = brush2,
        )
    }
}

@Composable
fun momoTrustDisplayFont(): FontFamily {
    return FontFamily(
        Font(
            R.font.momo_trust_display
        ),
    )
}
