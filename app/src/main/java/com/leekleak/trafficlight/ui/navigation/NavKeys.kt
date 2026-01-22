package com.leekleak.trafficlight.ui.navigation

import androidx.navigation3.runtime.NavKey
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

val mainScreens = listOf(Blank, Overview, History, Settings)

/**
 * Settings
 */
@Serializable
data class PlanConfig(val subscriberId: String) : NavKey

/**
 * Settings
 */
@Serializable
data object NotificationSettings : NavKey
