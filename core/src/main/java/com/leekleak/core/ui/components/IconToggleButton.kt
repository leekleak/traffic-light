package com.leekleak.core.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ButtonGroupScope
import androidx.compose.material3.IconButtonColors
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

fun ButtonGroupScope.iconToggleButton(
    text: String? = null,
    showBadge: Boolean = false,
    weight: Float? = null,
    enabled: Boolean = true,
    selected: Boolean,
    onSelect: () -> Unit,
    selectedColors: IconButtonColors? = null,
    unselectedColors: IconButtonColors? = null,
    icon: @Composable (() -> Unit)? = null
) {
    customItem(
        buttonGroupContent = {
            val haptic = LocalHapticFeedback.current
            val source = remember { MutableInteractionSource() }
            val press by source.collectIsPressedAsState()

            // Values grabbed from ShapeTokens
            val cornerRadius by animateDpAsState(if (press || selected) 8.dp else 16.dp)

            val containerColor by animateColorAsState(targetValue =
                if (selected) selectedColors?.containerColor ?: colorScheme.primaryContainer
                else unselectedColors?.containerColor ?: colorScheme.surfaceContainerHigh
            )
            val contentColor by animateColorAsState(targetValue =
                if (selected) selectedColors?.contentColor ?: colorScheme.onPrimaryContainer
                else unselectedColors?.contentColor ?: colorScheme.onSurfaceVariant
            )
            TextButton(
                modifier = Modifier.let { mod -> weight?.let { mod.weight(it) } ?: mod }.animateWidth(source),
                enabled = enabled,
                colors = ButtonDefaults.buttonColors(
                    containerColor = containerColor,
                    contentColor = contentColor
                ),
                shape = RoundedCornerShape(cornerRadius),
                interactionSource = source,
                onClick = {
                    onSelect()
                    haptic.performHapticFeedback(HapticFeedbackType.ToggleOn)
                }
            ) {
                @Composable
                fun content() {
                    Row {
                        icon?.let { it() }
                        text?.let {
                            Text(
                                modifier = if (icon!= null) Modifier.padding(start = 8.dp) else Modifier,
                                text = it,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
                if (showBadge) {
                    BadgedBox({ Badge() }) {
                        content()
                    }
                } else content()
            }
        },
        menuContent = {}
    )
}
