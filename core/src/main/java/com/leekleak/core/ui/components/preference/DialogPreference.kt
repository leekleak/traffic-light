package com.leekleak.core.ui.components.preference

import androidx.annotation.DrawableRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.leekleak.core.R
import com.leekleak.core.ui.components.FancyDialog

@Composable
fun DialogPreference(
    modifier: Modifier = Modifier,
    title: String,
    summary: String? = null,
    @DrawableRes icon: Int,
    enabled: Boolean = true,
    content: @Composable (ColumnScope.() -> Unit),
) {
    var expanded by remember { mutableStateOf(false) }
    val haptic = LocalHapticFeedback.current

    Preference(
        modifier = modifier,
        title = title,
        summary = summary,
        icon = icon,
        enabled = enabled,
        onClick = {
            expanded = true
            haptic.performHapticFeedback(HapticFeedbackType.ContextClick)
        },
    )
    if (expanded) {
        FancyDialog(
            onDismissRequest = { expanded = false },
            title = title,
            icon = icon,
            content = {
                content()
            },
        )
    }
}

@Composable
fun <T> DialogItemSelectPreference(
    modifier: Modifier = Modifier,
    title: String,
    summary: String? = null,
    @DrawableRes icon: Int,
    value: T,
    values: List<Pair<T, String>>,
    enabled: Boolean = true,
    onValueChanged: (T) -> Unit
) {
    val currentValueLabel = values.find { it.first == value }?.second ?: value.toString()
    val haptic = LocalHapticFeedback.current
    DialogPreference(
        modifier = modifier,
        title = title,
        summary = summary ?: currentValueLabel,
        icon = icon,
        enabled = enabled,
    ) {
        Column(
            modifier = Modifier
                .padding(vertical = 12.dp)
                .fillMaxWidth()
                .weight(1f, fill = false)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            values.forEach { (itemValue, label) ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(MaterialTheme.shapes.medium)
                        .clickable {
                            onValueChanged(itemValue)
                            haptic.performHapticFeedback(HapticFeedbackType.ContextClick)
                        }
                        .background(if (itemValue == value) colorScheme.primary else colorScheme.background)
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        modifier = Modifier.weight(1f),
                        text = label,
                        style = MaterialTheme.typography.bodyLargeEmphasized,
                        color = if (itemValue == value) colorScheme.onPrimary else colorScheme.onSurface,
                        fontWeight = FontWeight.Bold
                    )
                    AnimatedVisibility(
                        visible = itemValue == value,
                        enter = fadeIn(tween()) + scaleIn(),
                        exit = fadeOut(tween()) + scaleOut()
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.check_circle),
                            contentDescription = stringResource(R.string.selected),
                            tint = colorScheme.onPrimary
                        )
                    }
                }
            }
        }
    }
}
