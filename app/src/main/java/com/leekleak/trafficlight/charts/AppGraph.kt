package com.leekleak.trafficlight.charts

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import com.leekleak.trafficlight.database.AppUsage
import com.leekleak.trafficlight.ui.theme.googleSans
import com.leekleak.trafficlight.util.DataSize
import com.leekleak.trafficlight.util.toSp

@Composable
fun AppGraph(
    data: List<AppUsage>,
) {
    val onPrimaryColor = GraphTheme.onPrimaryColor
    val onBackgroundColor = GraphTheme.onBackgroundColor

    BoxWithConstraints(
        Modifier.fillMaxSize().padding(8.dp)
    ) {
        val maxBarWidth = maxWidth - 52.dp
        val maxUsage = data.firstOrNull()?.usage?.totalUsage?.toFloat() ?: return@BoxWithConstraints
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            data.forEachIndexed { index, usage ->
                val barWidth = maxBarWidth * (usage.usage.totalUsage.toFloat() / maxUsage)
                val text = DataSize(usage.usage.totalUsage).toString().replace(" ", "")
                val density = LocalDensity.current
                val textMeasurer = rememberTextMeasurer()
                val fittedFontWidth = remember(text, barWidth) {
                    val targetPx = with(density) { (barWidth - 8.dp).toPx() }

                    fun measureWidth(axisWidth: Float): Float {
                        val style = TextStyle(
                            fontFamily = googleSans(weight = 600f - 200f * index, width = axisWidth),
                            fontSize = with(density) { 56.dp.toSp() }
                        )
                        return textMeasurer.measure(text, style).size.width.toFloat()
                    }

                    if (measureWidth(25f) > targetPx) return@remember 25f

                    var lo = 26f
                    var hi = 125f
                    repeat(8) {
                        val mid = (lo + hi) / 2f
                        if (measureWidth(mid) > targetPx / 2f) hi = mid else lo = mid
                    }
                    lo
                }

                val font = remember(fittedFontWidth) { googleSans(weight = 600f - 200f * index, width = fittedFontWidth) }

                Row (horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(
                        Modifier
                            .clip(MaterialTheme.shapes.medium)
                            .background(GraphTheme.primaryColor)
                            .width(barWidth)
                            .height(40.dp)
                    ) {
                        if (fittedFontWidth != 25f) {
                            Text(font, onPrimaryColor, text)
                        }
                    }
                    usage.app.GetIcon(Modifier.size(40.dp))
                    if (fittedFontWidth == 25f) {
                        Box(Modifier.height(40.dp)) {
                            Text(font, onBackgroundColor, text)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BoxScope.Text(
    font: FontFamily,
    onPrimaryColor: Color,
    text: String
) {
    Text(
        modifier = Modifier
            .padding(start = 4.dp)
            .requiredHeight(68.dp)
            .align(Alignment.CenterStart),
        fontFamily = font,
        color = onPrimaryColor,
        text = text,
        fontSize = 56.dp.toSp,
        softWrap = false
    )
}
