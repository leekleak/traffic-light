package com.leekleak.trafficlight.ui.theme

import androidx.annotation.FloatRange
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import com.leekleak.trafficlight.R

@Composable
fun momoTrustDisplayFont(): FontFamily {
    return FontFamily(
        Font(
            R.font.momo_trust_display
        ),
    )
}

@Composable
fun carrierFont(): FontFamily = robotoFlex(-10f,25f,675f)

@Composable
fun doHyeonFont(): FontFamily {
    return FontFamily(
        Font(
            R.font.do_hyeon,
        ),
    )
}

@OptIn(ExperimentalTextApi::class)
@Composable
fun robotoFlex(
    @FloatRange(-10.0, 0.0) slant: Float,
    @FloatRange(25.0, 151.0) width: Float,
    @FloatRange(100.0, 1000.0) weight: Float
): FontFamily {
    return FontFamily(
        Font(
            R.font.roboto_flex,
            variationSettings = FontVariation.Settings(
                FontVariation.Setting("slnt", slant),
                FontVariation.Setting("wdth", width),
                FontVariation.Setting("wght", weight)
            )
        ),
    )
}

@OptIn(ExperimentalTextApi::class)
@Composable
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