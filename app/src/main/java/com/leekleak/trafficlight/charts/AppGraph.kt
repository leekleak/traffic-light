package com.leekleak.trafficlight.charts

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.EaseIn
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
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
import com.leekleak.trafficlight.model.DataUID
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

private val BarHeight = 40.dp
private val BarSpacing = 8.dp
private const val MaxApps = 3
private const val LegendTaps = 5
private const val TiltDegrees = 15f

private class FallingIconState {
    private var remaining = LegendTaps
    private var busy = false
    val rotation = Animatable(0f)
    val fall = Animatable(0f)
    val alpha = Animatable(1f)

    suspend fun tap(haptic: HapticFeedback) {
        if (busy || remaining <= 0) return
        remaining -= 1
        haptic.performHapticFeedback(
            if (remaining == 0) HapticFeedbackType.LongPress
            else HapticFeedbackType.ContextClick
        )
        rotation.animateTo(
            targetValue =
                when (rotation.targetValue) {
                    TiltDegrees -> -TiltDegrees
                    else -> TiltDegrees
                },
            animationSpec = tween(150)
        )
        if (remaining > 0) return

        busy = true
        fall.animateTo(1f, tween(250, easing = EaseIn))
        delay(3000L.milliseconds)
        alpha.snapTo(0f)
        fall.snapTo(0f)
        rotation.snapTo(0f)
        alpha.animateTo(1f)
        remaining = LegendTaps
        busy = false
    }
}

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
        val graphHeight = BarHeight * data.size + BarSpacing * (data.size - 1)
        val fallDistancePx = with(density) { (graphHeight + BarHeight).toPx() }
        val iconStates = remember(data) {
            data.associate { it.app.uid to FallingIconState() }
        }
        Column(
            verticalArrangement = Arrangement.spacedBy(BarSpacing)
        ) {
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

                    val haptic = LocalHapticFeedback.current
                    val scope = rememberCoroutineScope()
                    val state = iconStates.getValue(usage.app.uid)
                    Box(
                        Modifier
                            .size(40.dp)
                            .pointerInput(state) {
                                detectTapGestures {
                                    scope.launch { state.tap(haptic) }
                                }
                            }
                            .graphicsLayer {
                                rotationZ = state.rotation.value
                                translationY = state.fall.value * fallDistancePx
                                alpha = state.alpha.value
                            }
                    ) {
                        usage.app.GetIcon(Modifier.fillMaxSize())
                    }
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
private fun FallingAppIcon(
    app: DataUID,
    state: FallingIconState,
    fallDistancePx: Float,
) {
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    Box(
        Modifier
            .size(40.dp)
            .pointerInput(state) {
                detectTapGestures {
                    scope.launch { state.tap(haptic) }
                }
            }
            .graphicsLayer {
                rotationZ = state.rotation.value
                translationY = state.fall.value * fallDistancePx
                alpha = state.alpha.value
            }
    ) {
        app.GetIcon(Modifier.fillMaxSize())
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
