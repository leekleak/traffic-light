package com.leekleak.trafficlight.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.leekleak.core.ui.theme.AppTheme as CoreAppTheme
import com.leekleak.core.ui.theme.Theme
import com.leekleak.trafficlight.database.AppPreferenceRepo

@Composable
fun AppTheme(
    appPreferenceRepo: AppPreferenceRepo,
    content: @Composable () -> Unit
) {
    val theme by appPreferenceRepo.theme.collectAsState(Theme.AutoMaterial)
    val speedMetric by appPreferenceRepo.speedMetric.collectAsState(true)
    val sizeMetric by appPreferenceRepo.sizeMetric.collectAsState(false)
    val blurEnabled by appPreferenceRepo.blur.collectAsStateWithLifecycle(true)

    CoreAppTheme(
        theme = theme,
        speedMetric = speedMetric,
        sizeMetric = sizeMetric,
        blurEnabled = blurEnabled,
        content = content,
    )
}
