package com.leekleak.core.ui.navigation

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.FloatingToolbarDefaults
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.unit.dp

val BOTTOM_NAVBAR_PADDING = FloatingToolbarDefaults.ContainerSize + FloatingToolbarDefaults.ScreenOffset - 8.dp

val SIDE_NAVBAR_WIDTH = 96.dp
val SIDE_NAVBAR_PADDING = SIDE_NAVBAR_WIDTH + 16.dp

val LocalNavigationBarPadding = compositionLocalOf { PaddingValues(bottom = BOTTOM_NAVBAR_PADDING) }
