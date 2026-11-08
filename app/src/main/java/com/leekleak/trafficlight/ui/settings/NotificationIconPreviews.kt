package com.leekleak.trafficlight.ui.settings

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.leekleak.trafficlight.services.notifications.NotificationIconRenderer

@Composable
fun SpeedIconPreview(
    modifier: Modifier = Modifier.size(24.dp),
    value: String = "3.9",
    unit: String = "MB/s",
) {
    val context = LocalContext.current
    val renderer = remember(context) { NotificationIconRenderer(context) }
    val color = LocalContentColor.current.toArgb()
    Canvas(modifier) {
        drawIntoCanvas { canvas ->
            renderer.draw(canvas.nativeCanvas, size.minDimension, value, unit, color)
        }
    }
}

@Composable
fun SeparateSpeedIconPreview(
    modifier: Modifier = Modifier.size(24.dp),
    top: String = "24M",
    bottom: String = "687K",
) {
    val context = LocalContext.current
    val renderer = remember(context) { NotificationIconRenderer(context) }
    val color = LocalContentColor.current.toArgb()
    Canvas(modifier) {
        drawIntoCanvas { canvas ->
            renderer.drawSeparate(canvas.nativeCanvas, size.minDimension, top, bottom, color)
        }
    }
}
