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
data object DataPlansKey : NavKey

@Serializable
data object HistoryKey : NavKey

@Serializable
data object SettingsKey : NavKey

@Serializable
data object UsagePermissionRequestKey : NavKey

val mainScreens = listOf(OverviewKey, DataPlansKey, HistoryKey)

@Serializable
data class PlanConfigKey(val dataPlan: DataPlan) : NavKey

/**
 * Settings
 */
@Serializable
data object NotificationSettingsKey : NavKey
@Serializable
data object LibraryLicenseScreen : NavKey