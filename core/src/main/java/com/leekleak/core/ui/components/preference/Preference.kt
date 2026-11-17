package com.leekleak.core.ui.components.preference

import androidx.annotation.DrawableRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.leekleak.core.R
import com.leekleak.core.ui.theme.card

@Composable
fun Preference(
    modifier: Modifier = Modifier,
    title: String,
    summary: String? = null,
    @DrawableRes icon: Int? = null,
    iconContent: (@Composable () -> Unit)? = null, // TODO: currently only used for passing speed notification icons; however they'll be moved out of settings containers in the future and this should be removed then.
    onClick: () -> Unit = {},
    controls: @Composable (() -> Unit)? = null,
    enabled: Boolean = true,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .card()
            .clickable(enabled = enabled, onClick = onClick)
            .padding(
                start = if (icon != null || iconContent != null) 8.dp else 16.dp,
                end = 16.dp,
            )
            .alpha(if (enabled) 1f else 0.38f),
    ) {
        if (icon != null || iconContent != null) {
            Box(
                modifier = Modifier
                    .width(56.dp)
                    .padding(end = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                if (iconContent != null) {
                    iconContent()
                } else {
                    Icon(
                        modifier = Modifier.size(24.dp),
                        painter = painterResource(icon!!),
                        contentDescription = null,
                    )
                }
            }
        }
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(vertical = 16.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium
            )
            if (summary != null) {
                Text(
                    text = summary,
                    style = MaterialTheme.typography.bodyMedium,
                    color = colorScheme.onSurface,
                )
            }
        }
        if (controls != null) {
            Box(
                modifier = Modifier.padding(start = 24.dp)
            ) {
                controls()
            }
        }
    }
}

@Composable
fun NavigatePreference(
    modifier: Modifier = Modifier,
    title: String,
    summary: String? = null,
    @DrawableRes icon: Int? = null,
    onClick: () -> Unit = {},
    enabled: Boolean = true,
    showControl: Boolean = true
) {
    val haptic = LocalHapticFeedback.current
    Preference(
        modifier = modifier,
        title = title,
        summary = summary,
        icon = icon,
        onClick = {
            onClick()
            haptic.performHapticFeedback(HapticFeedbackType.ContextClick)
        },
        enabled = enabled,
        controls =
            if (showControl) {
                {
                    Icon(
                        painter = painterResource(R.drawable.arrow_forward),
                        contentDescription = null,
                    )
                }
            } else {
                null
            }
    )
}

@Composable
fun NavigatePreferenceIcon(
    modifier: Modifier = Modifier,
    icon: Painter,
    contentDescription: String,
    onClick: () -> Unit = {},
    enabled: Boolean = true,
) {
    val haptic = LocalHapticFeedback.current
    Box(
        modifier = modifier
            .padding(vertical = 4.dp)
            .card()
            .clickable(enabled = enabled, onClick = {
                onClick()
                haptic.performHapticFeedback(HapticFeedbackType.ContextClick)
            })
            .padding(18.dp)
            .alpha(if (enabled) 1f else 0.38f),
    ) {
        Icon(
            modifier = Modifier.size(24.dp),
            painter = icon,
            contentDescription = contentDescription,
        )
    }
}
