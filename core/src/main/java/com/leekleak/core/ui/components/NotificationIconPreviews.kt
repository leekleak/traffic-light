package com.leekleak.core.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.leekleak.core.R
import com.leekleak.core.ui.theme.googleSans
import com.leekleak.core.util.NotificationIconRenderer
import com.leekleak.core.util.toSp

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

@Composable
fun LiveNotificationPreview(
    modifier: Modifier = Modifier,
    text: String = "3.9 MB/s",
) {
    val font = remember { googleSans() }
    Row(
        modifier = modifier
            .padding(top = 12.dp) // off-center as the visual centered is pulled down by the help icon
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceContainerHighest)
            .padding(horizontal = 24.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            modifier = Modifier
                .padding(end = 6.dp)
                .size(42.dp),
            painter = painterResource(R.drawable.mobiledata_arrows),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = text,
            fontFamily = font,
            fontSize = 26.dp.toSp,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}
