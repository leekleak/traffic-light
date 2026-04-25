package com.leekleak.trafficlight.ui.navigation

import androidx.navigation3.runtime.NavKey
import com.leekleak.trafficlight.database.DataPlan
import kotlinx.serialization.Serializable

/**
 * Main screens
 */
@Serializable
data object Blank : NavKey
@Serializable
data object Overview : NavKey
@Serializable
data object History : NavKey
@Serializable
data object Settings : NavKey
@Serializable
data object UsagePermissionRequest : NavKey

val mainScreens = listOf(Blank, Overview, History)

/**
 * Settings
 */
@Serializable
data class PlanConfig(val dataPlan: DataPlan) : NavKey

/**
 * Settings
 */
@Serializable
data object NotificationSettings : NavKey
