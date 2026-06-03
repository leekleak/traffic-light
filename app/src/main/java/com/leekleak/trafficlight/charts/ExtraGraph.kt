package com.leekleak.trafficlight.charts

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.leekleak.trafficlight.database.DataPlanExtra
import com.leekleak.trafficlight.ui.theme.googleSans
import com.leekleak.trafficlight.util.DataSize
import com.leekleak.trafficlight.util.DataSizeUnit
import com.leekleak.trafficlight.util.fromTimestamp
import java.text.DecimalFormat
import kotlin.math.max
import kotlin.math.min

@Composable
fun ExtraGraph(
    modifier: Modifier,
    extra: DataPlanExtra,
) {
    val primaryColor = GraphTheme.primaryColor
    val onPrimaryColor = GraphTheme.onPrimaryColor
    val onBackgroundColor = GraphTheme.onBackgroundColor

    val maximum: Long = extra.dataAmount
    val used: Long = extra.dataUsed // TODO: Include volatile data
    val expirationDate = fromTimestamp(extra.expiryStamp)

    val textMeasurer = rememberTextMeasurer()
    val font1 = remember { googleSans(weight = 600f, width = 40f) }
    val font2 = remember { googleSans() }

    val safeMax = remember(maximum) { max(maximum, 1).toFloat() }

    val string = remember(used) { DataSize(used).toStringParts(extraPrecision = true) }
    val formatter = remember { DecimalFormat("0.##") }
    val data = remember(extra) { formatter.format(DataSize(maximum).getAsUnit(DataSizeUnit.GB)) }
    val stringAnnotated by remember { derivedStateOf {
        buildAnnotatedString {
            withStyle(style = SpanStyle(fontFamily = font1, fontSize = 46.sp)) {
                append("${string.first}${string.second}")
            }
            withStyle(style = SpanStyle(fontFamily = font1, fontSize = 36.sp)) {
                appendLine("/${data}GB")
            }
            withStyle(style = SpanStyle(fontFamily = font2, fontSize = 14.sp)) {
                append("Expires on:\n${expirationDate.toLocalDate()}")
            }
        }
    } }

    Canvas(modifier = modifier
        .height(128.dp)
        .clip(MaterialTheme.shapes.medium)
        .background(MaterialTheme.colorScheme.background)
    ) {
        val width = used.toFloat() / safeMax * size.width
        if (used != 0L) {
            drawRect(
                color = primaryColor,
                size = Size(width - min(width, 0.5.dp.toPx()), size.height)
            )
        }

        val textMeasure = textMeasurer.measure(
            stringAnnotated,
            TextStyle(
                textAlign = TextAlign.Center
            )
        )
        val point = used.toFloat() / safeMax - (size.width - textMeasure.size.width) / size.width / 2f
        val brush = Brush.horizontalGradient(
            0f to onPrimaryColor,
            point to onPrimaryColor,
            point to onBackgroundColor,
            1f to onBackgroundColor,
            startX = 0f,
            endX = size.width
        )
        drawText(
            topLeft = Offset((size.width - textMeasure.size.width) / 2, (size.height - textMeasure.size.height) / 2),
            textLayoutResult = textMeasure,
            brush = brush,
        )
    }
}
