package com.leekleak.trafficlight.ui.navigation

import androidx.navigation3.runtime.NavKey
import com.leekleak.trafficlight.database.DataPlan
import kotlinx.serialization.Serializable

/**
 * Main screens
 */

@Serializable
data object OverviewKey : NavKey

@Serializable
data class DataPlansKey(val selectedPlanHash: String? = null) : NavKey

@Serializable
data object HistoryKey : NavKey

@Serializable
data object ToolsKey : NavKey

@Serializable
data object SettingsKey : NavKey

@Serializable
data object UsagePermissionRequestKey : NavKey

val mainScreens = listOf(OverviewKey, DataPlansKey(), HistoryKey, ToolsKey)

@Serializable
data object IperfScreenKey : NavKey

@Serializable
data class PlanConfigKey(val dataPlan: DataPlan) : NavKey

@Serializable
data object NotificationSettingsKey : NavKey
@Serializable
data object OverlaySettingsKey : NavKey
@Serializable
data object LibraryLicenseScreen : NavKey