package com.leekleak.core.ui.components.preference

import androidx.annotation.DrawableRes
import androidx.compose.material3.Switch
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback

@Composable
fun SwitchPreference(
    modifier: Modifier = Modifier,
    title: String,
    @DrawableRes icon: Int? = null,
    iconContent: (@Composable () -> Unit)? = null,
    summary: String? = null,
    value: Boolean,
    enabled: Boolean = true,
    onValueChanged: (Boolean) -> Unit
) {
    val haptic = LocalHapticFeedback.current

    fun onClick(state: Boolean) {
        val feedback = if (state) HapticFeedbackType.ToggleOn else HapticFeedbackType.ToggleOff
        haptic.performHapticFeedback(feedback)
        onValueChanged(state)
    }
    Preference(
        modifier = modifier,
        title = title,
        icon = icon,
        iconContent = iconContent,
        summary = summary,
        enabled = enabled,
        onClick = {
            onClick(!value)
        },
        controls = {
            Switch(
                enabled = enabled, checked = value, onCheckedChange = {
                    onClick(it)
                },
            )
        },
    )
}
