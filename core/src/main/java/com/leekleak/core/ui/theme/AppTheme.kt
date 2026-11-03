package com.leekleak.core.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

val LocalSizeMetric = compositionLocalOf { false }
val LocalSpeedMetric = compositionLocalOf { false }
val LocalBlurEnabled = compositionLocalOf { true }

@Composable
fun AppTheme(
    theme: Theme = Theme.AutoMaterial,
    speedMetric: Boolean = true,
    sizeMetric: Boolean = false,
    blurEnabled: Boolean = true,
    content: @Composable () -> Unit,
) {
    val isDark = theme.isDark()

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as android.app.Activity).window
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !isDark
        }
    }

    CompositionLocalProvider(
        LocalSpeedMetric provides speedMetric,
        LocalSizeMetric provides sizeMetric,
        LocalBlurEnabled provides blurEnabled
    ) {
        MaterialTheme(theme.getColors()) { content() }
    }
}
