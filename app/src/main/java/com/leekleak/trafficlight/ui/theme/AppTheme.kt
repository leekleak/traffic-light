package com.leekleak.trafficlight.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.leekleak.core.ui.theme.AppTheme
import com.leekleak.trafficlight.database.AppPreferenceRepo

@Composable
fun AppThemeWrapper(
    appPreferenceRepo: AppPreferenceRepo,
    content: @Composable () -> Unit
) {
    val theme by appPreferenceRepo.theme.collectAsState()
    val speedMetric by appPreferenceRepo.speedMetric.collectAsState(true)
    val sizeMetric by appPreferenceRepo.sizeMetric.collectAsState(false)
    val speedInBits by appPreferenceRepo.speedBits.collectAsState(false)
    val blurEnabled by appPreferenceRepo.blur.collectAsStateWithLifecycle(true)

    AppTheme(
        theme = theme,
        speedMetric = speedMetric,
        sizeMetric = sizeMetric,
        speedInBits = speedInBits,
        blurEnabled = blurEnabled,
        content = content,
    )
}
