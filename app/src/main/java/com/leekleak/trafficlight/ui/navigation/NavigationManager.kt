package com.leekleak.trafficlight.ui.navigation

import android.annotation.SuppressLint
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FloatingToolbarDefaults
import androidx.compose.material3.HorizontalFloatingToolbar
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import com.leekleak.trafficlight.R
import com.leekleak.trafficlight.database.UsageMode
import com.leekleak.trafficlight.ui.history.History
import com.leekleak.trafficlight.ui.overview.Overview
import com.leekleak.trafficlight.ui.settings.NotificationSettings
import com.leekleak.trafficlight.ui.settings.Settings
import com.leekleak.trafficlight.ui.theme.navBarShadow
import com.leekleak.trafficlight.util.WideScreenWrapper
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable

/**
 * Main screens
 */
@Serializable
data object Overview : NavKey
@Serializable
data object History : NavKey
@Serializable
data object Settings : NavKey

val mainScreens = listOf(Overview, History, Settings)

/**
 * Settings
 */
@Serializable
data object NotificationSettings : NavKey

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun NavigationManager() {
    val viewModel: NavigationManagerVM = viewModel()
    val usageMode by viewModel.hourlyUsageRepo.usageModeFlow().collectAsState(UsageMode.Unlimited)

    val backStack = rememberNavBackStack(Settings)
    var showBottomBar by remember { mutableStateOf(false) }

    LaunchedEffect(backStack.last()) {
        showBottomBar = mainScreens.contains(backStack.last())
    }

    LaunchedEffect(usageMode) {
        backStack.clear()
        if (usageMode != UsageMode.Unlimited) backStack.add(Settings) else backStack.add(Overview)
    }

    val toolbarVisible = usageMode == UsageMode.Unlimited && showBottomBar
    val toolbarOffset =
        FloatingToolbarDefaults.ContainerSize +
        FloatingToolbarDefaults.ScreenOffset

    val topPadding = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val bottomPadding = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    val paddingValues =
        PaddingValues(
            start = 16.dp,
            end = 16.dp,
            top = topPadding,
            bottom = bottomPadding + if (toolbarVisible) toolbarOffset else 8.dp
        )

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            Box(
                modifier = Modifier.fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(8.dp),
                contentAlignment = Alignment.Center
            ) {
                AnimatedVisibility(
                    visible = toolbarVisible,
                    enter = slideInVertically {it} + fadeIn(),
                    exit = slideOutVertically {it} + fadeOut()
                ) {
                    HorizontalFloatingToolbar(
                        modifier = Modifier.navBarShadow(),
                        expanded = true,
                        content = {
                            NavigationButton(backStack, Overview, R.drawable.overview)
                            NavigationButton(backStack, History, R.drawable.history)
                            NavigationButton(backStack, Settings, R.drawable.settings)
                        },
                    )
                }
            }
        }
    ) {
        WideScreenWrapper {
            NavDisplay(
                backStack = backStack,
                onBack = { backStack.removeLastOrNull() },
                entryProvider = entryProvider {
                    entry<Overview> { Overview(paddingValues) }
                    entry<History> { History(paddingValues) }
                    entry<Settings> { Settings(paddingValues, backStack) }
                    entry<NotificationSettings> { NotificationSettings(paddingValues) }
                },
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                popTransitionSpec = { fadeIn() togetherWith fadeOut() },
                predictivePopTransitionSpec = { fadeIn() togetherWith fadeOut() },
            )
        }
    }
}


@Composable
fun NavigationButton(backstack: NavBackStack<NavKey>, route: NavKey, icon: Int) {
    val haptic = LocalHapticFeedback.current
    val animation = remember { Animatable(1f) }
    val scope = rememberCoroutineScope()
    IconButton(
        modifier = Modifier.scale(animation.value),
        colors =
            if (backstack.last() == route){
                IconButtonDefaults.filledIconButtonColors()
            } else {
                IconButtonDefaults.iconButtonColors()
            },
        onClick = {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            backstack.clear()
            backstack.add(route)
            scope.launch {
                animation.snapTo(0.9f)
                animation.animateTo(1f)
            }
        }
    ) {
        Icon(
            painter = painterResource(icon),
            contentDescription = null
        )
    }
}