package com.leekleak.trafficlight.ui.theme

import androidx.annotation.FloatRange
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import com.leekleak.trafficlight.R

fun carrierFont(): FontFamily = robotoFlex(-10f,25f,675f)

fun doHyeonFont(): FontFamily {
    return FontFamily(
        Font(
            R.font.do_hyeon,
        ),
    )
}

@OptIn(ExperimentalTextApi::class)
fun robotoFlex(
    @FloatRange(-10.0, 0.0) slant: Float,
    @FloatRange(25.0, 151.0) width: Float,
    @FloatRange(100.0, 1000.0) weight: Float,
    @FloatRange(323.0, 603.0) counterWidth: Float = 468f
): FontFamily {
    return FontFamily(
        Font(
            R.font.roboto_flex,
            variationSettings = FontVariation.Settings(
                FontVariation.Setting("slnt", slant),
                FontVariation.Setting("wdth", width),
                FontVariation.Setting("wght", weight),
                FontVariation.Setting("XTRA", counterWidth)
            )
        ),
    )
}

@OptIn(ExperimentalTextApi::class)
fun jetbrainsMono(
    @FloatRange(100.0, 800.0) weight: Float = 400f
): FontFamily {
    return FontFamily(
        Font(
            R.font.jetbrains_mono,
            variationSettings = FontVariation.Settings(
                FontVariation.Setting("wght", weight)
            )
        ),
    )
}

@OptIn(ExperimentalTextApi::class)
fun outfit(
    @FloatRange(100.0, 900.0) weight: Float = 500f
): FontFamily {
    return FontFamily(
        Font(
            R.font.outfit,
            variationSettings = FontVariation.Settings(
                FontVariation.Setting("wght", weight)
            )
        ),
    )
}

fun historyItemFont() = robotoFlex(0f, 90f, 800f)
fun notificationIconFont() = robotoFlex(0f, 30f, 600f, 450f)