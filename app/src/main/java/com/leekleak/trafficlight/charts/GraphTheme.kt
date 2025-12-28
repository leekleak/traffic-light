package com.leekleak.trafficlight.charts

import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialShapes.Companion.Arch
import androidx.compose.material3.MaterialShapes.Companion.Burst
import androidx.compose.material3.MaterialShapes.Companion.Circle
import androidx.compose.material3.MaterialShapes.Companion.Clover4Leaf
import androidx.compose.material3.MaterialShapes.Companion.Clover8Leaf
import androidx.compose.material3.MaterialShapes.Companion.Cookie12Sided
import androidx.compose.material3.MaterialShapes.Companion.Cookie4Sided
import androidx.compose.material3.MaterialShapes.Companion.Cookie6Sided
import androidx.compose.material3.MaterialShapes.Companion.Cookie7Sided
import androidx.compose.material3.MaterialShapes.Companion.Cookie9Sided
import androidx.compose.material3.MaterialShapes.Companion.Flower
import androidx.compose.material3.MaterialShapes.Companion.Pentagon
import androidx.compose.material3.MaterialShapes.Companion.PixelCircle
import androidx.compose.material3.MaterialShapes.Companion.Slanted
import androidx.compose.material3.MaterialShapes.Companion.SoftBurst
import androidx.compose.material3.MaterialShapes.Companion.Square
import androidx.compose.material3.MaterialShapes.Companion.Sunny
import androidx.compose.material3.MaterialShapes.Companion.VerySunny
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.leekleak.trafficlight.R
import com.leekleak.trafficlight.util.px

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
object GraphTheme {
    val wifiShapes = listOf(Circle, Pentagon, VerySunny, Sunny, Cookie6Sided, Cookie7Sided,
        Cookie9Sided, Cookie12Sided, Clover8Leaf, Burst, SoftBurst, Flower)
    val cellularShapes = listOf(Square, Slanted, Arch, Cookie4Sided, Clover4Leaf, PixelCircle)

    val cornerRadius @Composable get() = CornerRadius(12.dp.px)
    val primaryColor @Composable get() = MaterialTheme.colorScheme.primary
    val secondaryColor @Composable get() = MaterialTheme.colorScheme.tertiary
    val onPrimaryColor @Composable get() = MaterialTheme.colorScheme.onPrimary
    val onSecondaryColor @Composable get() = MaterialTheme.colorScheme.onTertiary
    val backgroundColor @Composable get() = MaterialTheme.colorScheme.surface
    val gridColor @Composable get() = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)

    @Composable
    fun wifiShape() = remember { wifiShapes.random() }

    @Composable
    fun cellularShape() = remember { cellularShapes.random() }

    @Composable
    fun wifiIcon() = painterResource(R.drawable.wifi)

    @Composable
    fun cellularIcon() = painterResource(R.drawable.cellular)
}