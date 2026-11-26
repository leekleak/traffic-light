package com.leekleak.core.ui.theme

import android.content.Context
import android.content.res.Configuration
import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.MaterialTheme.shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.leekleak.core.R

enum class Theme {
    AutoMaterial,
    LightMaterial,
    DarkMaterial,
    Auto,
    Light,
    Dark;

    val isMaterial: Boolean
        get() = this == AutoMaterial || this == LightMaterial || this == DarkMaterial

    @Composable
    fun getColors(): ColorScheme {
        val context = LocalContext.current
        return getColors(context, isSystemInDarkTheme())
    }

    fun getColors(context: Context, darkTheme: Boolean): ColorScheme {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            return when (this) {
                AutoMaterial -> if (darkTheme) darkColorScheme() else lightColorScheme()
                LightMaterial -> lightColorScheme()
                DarkMaterial -> darkColorScheme()
                Auto -> if (darkTheme) darkScheme else lightScheme
                Light -> lightScheme
                Dark -> darkScheme
            }
        }

        return when (this) {
            AutoMaterial -> if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
            LightMaterial -> dynamicLightColorScheme(context)
            DarkMaterial -> dynamicDarkColorScheme(context)
            Auto -> if (darkTheme) darkScheme else lightScheme
            Light -> lightScheme
            Dark -> darkScheme
        }
    }

    @Composable
    fun getName(): String {
        return when (this) {
            AutoMaterial, Auto -> stringResource(R.string.auto)
            LightMaterial, Light -> stringResource(R.string.light)
            DarkMaterial, Dark -> stringResource(R.string.dark)
        }
    }

    @Composable
    fun isDark(): Boolean = isDark(isSystemInDarkTheme())

    fun isDark(context: Context): Boolean {
        val systemDarkTheme =
            (context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) ==
                Configuration.UI_MODE_NIGHT_YES
        return isDark(systemDarkTheme)
    }

    private fun isDark(systemDarkTheme: Boolean): Boolean {
        return (
            systemDarkTheme && this == AutoMaterial ||
            systemDarkTheme && this == Auto ||
            this == DarkMaterial ||
            this == Dark
        )
    }
}


@Composable
fun Modifier.card(backgroundColor: Color = colorScheme.surfaceContainer): Modifier {
    return this
        .clip(MaterialTheme.shapes.medium)
        .background(backgroundColor)
}

inline val shelfShape: RoundedCornerShape
    @Composable get() = RoundedCornerShape(
        topStart = shapes.large.topStart,
        topEnd = shapes.large.topEnd,
        bottomEnd = CornerSize(0.dp),
        bottomStart = CornerSize(0.dp)
    )

inline val variableRoundedStart: RoundedCornerShape
    @Composable get() = RoundedCornerShape(
        topStart = shapes.medium.topStart,
        topEnd = shapes.small.topEnd,
        bottomEnd = shapes.small.bottomEnd,
        bottomStart = shapes.medium.bottomStart
    )

inline val variableRoundedEnd: RoundedCornerShape
    @Composable get() = RoundedCornerShape(
        topStart = shapes.small.topStart,
        topEnd = shapes.medium.topEnd,
        bottomEnd = shapes.medium.bottomEnd,
        bottomStart = shapes.small.bottomStart
    )

internal val lightScheme = lightColorScheme(
    primary = PrimaryLight,
    onPrimary = OnPrimaryLight,
    primaryContainer = PrimaryContainerLight,
    onPrimaryContainer = OnPrimaryContainerLight,
    inversePrimary = InversePrimaryLight,
    secondary = SecondaryLight,
    onSecondary = OnSecondaryLight,
    secondaryContainer = SecondaryContainerLight,
    onSecondaryContainer = OnSecondaryContainerLight,
    tertiary = TertiaryLight,
    onTertiary = OnTertiaryLight,
    tertiaryContainer = TertiaryContainerLight,
    onTertiaryContainer = OnTertiaryContainerLight,
    background = BackgroundLight,
    onBackground = OnBackgroundLight,
    surface = SurfaceLight,
    onSurface = OnSurfaceLight,
    surfaceVariant = SurfaceVariantLight,
    onSurfaceVariant = OnSurfaceVariantLight,
    surfaceTint = SurfaceTintLight,
    inverseSurface = InverseSurfaceLight,
    inverseOnSurface = InverseOnSurfaceLight,
    error = ErrorLight,
    onError = OnErrorLight,
    errorContainer = ErrorContainerLight,
    onErrorContainer = OnErrorContainerLight,
    outline = OutlineLight,
    outlineVariant = OutlineVariantLight,
    scrim = ScrimLight,
    surfaceBright = SurfaceBrightLight,
    surfaceContainer = SurfaceContainerLight,
    surfaceContainerHigh = SurfaceContainerHighLight,
    surfaceContainerHighest = SurfaceContainerHighestLight,
    surfaceContainerLow = SurfaceContainerLowLight,
    surfaceContainerLowest = SurfaceContainerLowestLight,
    surfaceDim = SurfaceDimLight,
    primaryFixed = PrimaryFixed,
    primaryFixedDim = PrimaryFixedDim,
    onPrimaryFixed = OnPrimaryFixed,
    onPrimaryFixedVariant = OnPrimaryFixedVariant,
    secondaryFixed = SecondaryFixed,
    secondaryFixedDim = SecondaryFixedDim,
    onSecondaryFixed = OnSecondaryFixed,
    onSecondaryFixedVariant = OnSecondaryFixedVariant,
    tertiaryFixed = TertiaryFixed,
    tertiaryFixedDim = TertiaryFixedDim,
    onTertiaryFixed = OnTertiaryFixed,
    onTertiaryFixedVariant = OnTertiaryFixedVariant,
)

internal val darkScheme = darkColorScheme(
    primary = PrimaryDark,
    onPrimary = OnPrimaryDark,
    primaryContainer = PrimaryContainerDark,
    onPrimaryContainer = OnPrimaryContainerDark,
    inversePrimary = InversePrimaryDark,
    secondary = SecondaryDark,
    onSecondary = OnSecondaryDark,
    secondaryContainer = SecondaryContainerDark,
    onSecondaryContainer = OnSecondaryContainerDark,
    tertiary = TertiaryDark,
    onTertiary = OnTertiaryDark,
    tertiaryContainer = TertiaryContainerDark,
    onTertiaryContainer = OnTertiaryContainerDark,
    background = BackgroundDark,
    onBackground = OnBackgroundDark,
    surface = SurfaceDark,
    onSurface = OnSurfaceDark,
    surfaceVariant = SurfaceVariantDark,
    onSurfaceVariant = OnSurfaceVariantDark,
    surfaceTint = SurfaceTintDark,
    inverseSurface = InverseSurfaceDark,
    inverseOnSurface = InverseOnSurfaceDark,
    error = ErrorDark,
    onError = OnErrorDark,
    errorContainer = ErrorContainerDark,
    onErrorContainer = OnErrorContainerDark,
    outline = OutlineDark,
    outlineVariant = OutlineVariantDark,
    scrim = ScrimDark,
    surfaceBright = SurfaceBrightDark,
    surfaceContainer = SurfaceContainerDark,
    surfaceContainerHigh = SurfaceContainerHighDark,
    surfaceContainerHighest = SurfaceContainerHighestDark,
    surfaceContainerLow = SurfaceContainerLowDark,
    surfaceContainerLowest = SurfaceContainerLowestDark,
    surfaceDim = SurfaceDimDark,
    primaryFixed = PrimaryFixed,
    primaryFixedDim = PrimaryFixedDim,
    onPrimaryFixed = OnPrimaryFixed,
    onPrimaryFixedVariant = OnPrimaryFixedVariant,
    secondaryFixed = SecondaryFixed,
    secondaryFixedDim = SecondaryFixedDim,
    onSecondaryFixed = OnSecondaryFixed,
    onSecondaryFixedVariant = OnSecondaryFixedVariant,
    tertiaryFixed = TertiaryFixed,
    tertiaryFixedDim = TertiaryFixedDim,
    onTertiaryFixed = OnTertiaryFixed,
    onTertiaryFixedVariant = OnTertiaryFixedVariant,
)
