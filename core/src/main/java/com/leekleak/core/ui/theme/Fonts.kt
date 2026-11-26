package com.leekleak.core.ui.theme

import androidx.annotation.FloatRange
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import com.leekleak.core.R

fun googleSans(
    @FloatRange(200.0, 1000.0) weight: Float = 400f,
    @FloatRange(25.0, 130.0) width: Float = 100f,
    @FloatRange(0.0, 100.0) roundness: Float = 0f
): FontFamily {
    return FontFamily(
        Font(
            R.font.google_sans_flex,
            variationSettings = FontVariation.Settings(
                FontVariation.Setting("wght", weight),
                FontVariation.Setting("wdth", width),
                FontVariation.Setting("ROND", roundness)
            )
        ),
    )
}

fun carrierFont(): FontFamily {
    return FontFamily(
        Font(
            R.font.google_sans_flex_carrier,
            weight = FontWeight.SemiBold,
        ),
    )
}

fun googleSansEmphasized(): FontFamily = googleSans(weight = 750f, width = 105f, roundness = 30f)
fun googleSansPlan(): FontFamily = googleSans(weight = 650f, width = 95f, roundness = 30f)
fun historyItemFont() = googleSans(weight = 800f)
