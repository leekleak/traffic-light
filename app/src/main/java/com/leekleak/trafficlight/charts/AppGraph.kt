package com.leekleak.trafficlight.charts

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.leekleak.core.R
import com.leekleak.core.ui.theme.googleSans
import com.leekleak.core.ui.theme.googleSansEmphasized
import com.leekleak.core.util.DataSize
import com.leekleak.core.util.formatted
import com.leekleak.core.util.toSp
import com.leekleak.trafficlight.database.AppUsage

private val BarHeight = 40.dp
private val BarSpacing = 8.dp
private const val MaxApps = 3

@Composable
fun AppGraph(
    data: List<AppUsage>,
    alternateColor: Boolean = false,
) {
    BoxWithConstraints(
        Modifier.fillMaxWidth().padding(8.dp)
    ) {
        if (data.isEmpty()) {
            val font = remember { googleSansEmphasized() }
            val height = BarHeight * MaxApps + BarSpacing * (MaxApps - 1)
            Box(Modifier.fillMaxWidth().height(height)) {
                Text(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .align(Alignment.Center),
                    text = stringResource(R.string.no_usage_detected),
                    textAlign = TextAlign.Center,
                    color = colorScheme.onSurfaceVariant,
                    fontFamily = font
                )
            }
            return@BoxWithConstraints
        }
        val maxBarWidth = maxWidth - 52.dp
        val maxUsage = data.firstOrNull()?.usage?.totalUsage?.toFloat() ?: return@BoxWithConstraints
        val density = LocalDensity.current
        val textMeasurer = rememberTextMeasurer()
        Column(verticalArrangement = Arrangement.spacedBy(BarSpacing)) {
            data.forEachIndexed { index, usage ->
                val barWidth = maxBarWidth * (usage.usage.totalUsage.toFloat() / maxUsage)
                val text = DataSize(usage.usage.totalUsage).formatted().replace(" ", "")
                val font = remember(index) { googleSans(weight = 600f - 100f * index, width = 60f) }
                val labelInsideBar = remember(text, barWidth, font) {
                    val style = TextStyle(fontFamily = font, fontSize = with(density) { 42.dp.toSp() })
                    textMeasurer.measure(text, style).size.width <= with(density) { (barWidth - 8.dp).toPx() }
                }

                Row (horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(
                        Modifier
                            .clip(MaterialTheme.shapes.medium)
                            .background(if (alternateColor) colorScheme.tertiary else colorScheme.primary)
                            .width(barWidth)
                            .height(BarHeight)
                    ) {
                        if (labelInsideBar) {
                            val color = if (alternateColor) colorScheme.onTertiary else colorScheme.onPrimary
                            Text(font, color, text)
                        }
                    }
                    usage.app.GetIcon(Modifier.size(40.dp))
                    if (!labelInsideBar) {
                        Box(Modifier.height(BarHeight)) {
                            Text(font, colorScheme.onBackground, text)
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
            .padding(start = 6.dp)
            .height(38.dp)
            .align(Alignment.CenterStart)
            .wrapContentHeight(unbounded = true),
        fontFamily = font,
        color = onPrimaryColor,
        text = text,
        fontSize = 42.dp.toSp,
        softWrap = false
    )
}
