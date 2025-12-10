package com.leekleak.trafficlight.ui.settings

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.toPath
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.leekleak.trafficlight.R
import com.leekleak.trafficlight.charts.GraphTheme
import com.leekleak.trafficlight.ui.theme.Theme
import com.leekleak.trafficlight.ui.theme.card
import com.leekleak.trafficlight.util.CategoryTitleSmallText
import com.leekleak.trafficlight.util.px
import kotlinx.coroutines.launch

@Composable
fun Preference(
    title: String,
    summary: String? = null,
    icon: Painter? = null,
    onClick: () -> Unit = {},
    controls: @Composable (() -> Unit)? = null,
    enabled: Boolean = true,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .card()
            .clickable(enabled = enabled, onClick = onClick)
            .padding(
                start = if (icon != null) 8.dp else 16.dp,
                end = 16.dp,
            )
            .alpha(if (enabled) 1f else 0.38f),
    ) {
        if (icon != null) {
            Box(
                modifier = Modifier
                    .width(56.dp)
                    .padding(end = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = icon,
                    contentDescription = null,
                )
            }
        } else {
            Box(modifier = Modifier.size(0.dp))
        }
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(vertical = 16.dp),
        ) {
            ProvideTextStyle(value = MaterialTheme.typography.titleMedium) {
                Text(text = title)
            }
            if (summary != null) {
                CompositionLocalProvider(
                    LocalTextStyle provides MaterialTheme.typography.bodyMedium,
                    LocalContentColor provides colorScheme.onSurface,
                ) {
                    Text(text = summary)
                }
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
fun SwitchPreference(
    title: String,
    icon: Painter? = null,
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
        title = title,
        icon = icon,
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

@Composable
fun ThemePreferenceContainer(currentTheme: Theme, material: Boolean, onThemeChanged: (Theme) -> Unit) {
    val themeLight = if (material) Theme.LightMaterial else Theme.Light
    val themeDark = if (material) Theme.DarkMaterial else Theme.Dark
    val themeAuto = if (material) Theme.AutoMaterial else Theme.Auto
    Column {
        CategoryTitleSmallText(if (material) stringResource(R.string.material_theme) else stringResource(R.string.default_theme))
        Column(
            modifier = Modifier
                .clip(MaterialTheme.shapes.large)
                .width(IntrinsicSize.Max)
                .background(colorScheme.surface)
                .padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ThemePreference(
                    themeLight,
                    themeLight == currentTheme
                ) { onThemeChanged(themeLight) }
                ThemePreference(themeDark, themeDark == currentTheme) { onThemeChanged(themeDark) }
            }
            ThemeAutoPreference(themeAuto, themeAuto == currentTheme) { onThemeChanged(themeAuto) }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ThemePreference(theme: Theme, enabled: Boolean, onClick: () -> Unit) {
    val scheme = theme.getColors()
    val radiusSmall = 12.dp.px
    val radiusBig = 38.dp.px
    val cornerRadius = remember { Animatable(radiusSmall) }
    val rotation = remember { Animatable(0f) }

    val shape1 = GraphTheme.wifiShape().toPath()
    val shape2 = GraphTheme.cellularShape().toPath()
    val iconScaleSmall = 42.dp.px
    val iconScaleBig = 48.dp.px
    val iconScale = remember { Animatable(iconScaleSmall) }

    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current

    LaunchedEffect(enabled) {
        if (enabled) {
            launch { cornerRadius.animateTo(radiusBig) }
            launch { rotation.animateTo(-10f) }
            launch { iconScale.animateTo(iconScaleBig) }
        } else {
            launch { cornerRadius.animateTo(radiusSmall) }
            launch { rotation.animateTo(0f) }
            launch { iconScale.animateTo(iconScaleSmall) }
        }
    }
    Column (
        Modifier
            .card()
            .clickable(onClick = {
                scope.launch { haptic.performHapticFeedback(HapticFeedbackType.ToggleOn) }
                onClick()
            })
            .background(if (enabled) colorScheme.surfaceVariant else colorScheme.surfaceContainer)
            .padding(4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .padding(4.dp)
                .size(120.dp, 70.dp)
                .drawBehind {
                    rotate(-rotation.value) {
                        drawRoundRect(
                            color = scheme.background,
                            cornerRadius = CornerRadius(cornerRadius.value)
                        )
                    }
                    val x = size.width / 7f
                    val y = size.height / 2f
                    translate(x * 2, y) {
                        rotate(rotation.value * 2f) {
                            translate(-rotation.value, rotation.value) {
                                scale(iconScale.value, Offset(0.5f, 0.5f)) {
                                    drawPath(shape1, scheme.primary)
                                }
                            }
                        }
                    }
                    translate(x * 5, y) {
                        rotate(-rotation.value * 2f) {
                            translate(rotation.value, -rotation.value) {
                                scale(iconScale.value, Offset(0.5f, 0.5f)) {
                                    drawPath(shape2, scheme.tertiary)
                                }
                            }
                        }
                    }
                }
                .padding(12.dp)
                .fillMaxWidth(),
        )
        Row (
            Modifier.padding(vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                painter = painterResource(if (theme.isDark()) R.drawable.dark else R.drawable.light),
                contentDescription = null
            )
            Text(
                text = theme.getName(),
                style = MaterialTheme.typography.titleMedium,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ThemeAutoPreference(theme: Theme, enabled: Boolean, onClick: () -> Unit) {
    val scheme = theme.getColors()
    val radiusSmall = 12.dp.px
    val radiusBig = 38.dp.px
    val cornerRadius = remember { Animatable(radiusSmall) }
    val rotation = remember { Animatable(-15f) }

    val shape1 = GraphTheme.wifiShape().toPath()
    val shape2 = GraphTheme.cellularShape().toPath()
    val iconScaleSmall = 42.dp.px
    val iconScaleBig = 48.dp.px
    val iconScale = remember { Animatable(iconScaleSmall) }

    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current

    LaunchedEffect(enabled) {
        if (enabled) {
            launch { cornerRadius.animateTo(radiusBig) }
            launch { rotation.animateTo(-5f) }
            launch { iconScale.animateTo(iconScaleBig) }
        } else {
            launch { cornerRadius.animateTo(radiusSmall) }
            launch { rotation.animateTo(-15f) }
            launch { iconScale.animateTo(iconScaleSmall) }
        }
    }
    Box(
        modifier = Modifier
            .card()
            .clickable(onClick = {
                scope.launch { haptic.performHapticFeedback(HapticFeedbackType.ToggleOn) }
                onClick()
            })
            .background(if (enabled) colorScheme.surfaceVariant else colorScheme.surfaceContainer)
            .padding(4.dp)
            .drawBehind {
                val x = size.width / 7f
                val y = size.height / 2f
                translate(x * 1, y) {
                    rotate(rotation.value * 2f) {
                        scale(iconScale.value, Offset(0.5f, 0.5f)) {
                            drawPath(shape1, scheme.primary)
                        }
                    }
                }
                translate(x * 6, y) {
                    rotate(-rotation.value * 2f) {
                        scale(iconScale.value, Offset(0.5f, 0.5f)) {
                            drawPath(shape2, scheme.tertiary)
                        }
                    }
                }
            }
            .padding(12.dp)
            .fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(2.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                painter = painterResource(R.drawable.magic),
                contentDescription = null,
            )
            Text(
                text = theme.getName(),
                style = MaterialTheme.typography.titleMedium,
            )
        }
    }
}

@Composable
fun PermissionCard(
    title: String,
    description: String,
    icon: Painter,
    onClick: () -> Unit,
    extraButton: @Composable (() -> Unit)? = null
) {
    Row (modifier = Modifier
        .card()
        .padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier.weight(1f),
            horizontalAlignment = Alignment.End
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(icon, "Icon")
                Text(modifier = Modifier.fillMaxWidth(), fontWeight = FontWeight.Bold, text = title)
            }
            Text(modifier = Modifier.fillMaxWidth(), text = description)
        }
        extraButton?.invoke()
        FloatingActionButton (
            onClick = onClick
        ) {
            Icon(
                painterResource(R.drawable.grant),
                contentDescription = stringResource(R.string.grant),
            )
        }
    }
}