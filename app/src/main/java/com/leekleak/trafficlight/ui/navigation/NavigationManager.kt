package com.leekleak.trafficlight.ui.navigation

import android.annotation.SuppressLint
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FloatingToolbarDefaults
import androidx.compose.material3.HorizontalFloatingToolbar
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.ui.NavDisplay
import com.leekleak.trafficlight.R
import com.leekleak.trafficlight.ui.history.History
import com.leekleak.trafficlight.ui.overview.Overview
import com.leekleak.trafficlight.ui.overview.PlanConfig
import com.leekleak.trafficlight.ui.settings.NotificationSettings
import com.leekleak.trafficlight.ui.settings.Settings
import com.leekleak.trafficlight.ui.settings.UsagePermissionRequest
import com.leekleak.trafficlight.ui.theme.navBarShadow
import com.leekleak.trafficlight.util.TOP_BAR_HEIGHT
import com.leekleak.trafficlight.util.WideScreenWrapper
import org.koin.compose.koinInject
import org.koin.core.annotation.KoinExperimentalAPI

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@OptIn(ExperimentalMaterial3ExpressiveApi::class, KoinExperimentalAPI::class)
@Composable
fun NavigationManager() {
    val navigator: Navigator = koinInject()
    val backStack = navigator.backStack

    var showBottomBar by remember { mutableStateOf(false) }

    LaunchedEffect(navigator.backStack.last()) {
        showBottomBar = mainScreens.contains(navigator.backStack.last())
    }

    val toolbarOffset =
        FloatingToolbarDefaults.ContainerSize +
        FloatingToolbarDefaults.ScreenOffset

    val topPadding = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val bottomPadding = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val paddingValues =
        PaddingValues(
            start = 16.dp,
            end = 16.dp,
            top = topPadding + TOP_BAR_HEIGHT,
            bottom = bottomPadding + if (showBottomBar) toolbarOffset else 8.dp
        )

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(8.dp),
                contentAlignment = Alignment.Center
            ) {
                AnimatedVisibility(
                    visible = showBottomBar,
                    enter = slideInVertically {it} + fadeIn(),
                    exit = slideOutVertically {it} + fadeOut()
                ) {
                    HorizontalFloatingToolbar(
                        modifier = Modifier.navBarShadow(),
                        expanded = true,
                        content = {
                            NavigationButton(backStack, Overview, stringResource(R.string.overview), R.drawable.overview)
                            NavigationButton(backStack, History, stringResource(R.string.history), R.drawable.history)
                        },
                    )
                }
            }
        }
    ) {
        WideScreenWrapper {
            NavDisplay(
                backStack = navigator.backStack,
                onBack = { navigator.goBack() },
                entryProvider = entryProvider {
                    entry<Blank> { Box(modifier = Modifier.fillMaxSize())}
                    entry<Overview> { Overview(paddingValues) }
                    entry<History> { History(paddingValues) }
                    entry<Settings> { Settings(paddingValues) }

                    entry<UsagePermissionRequest> { UsagePermissionRequest(paddingValues) }
                    entry<PlanConfig> { PlanConfig(it.subscriberId) }
                    entry<NotificationSettings> { NotificationSettings(paddingValues) }
                },
                transitionSpec = {
                    if (backStack.size == 1) fadeIn() togetherWith fadeOut()
                    else {
                        slideInHorizontally { it } togetherWith
                        slideOutHorizontally { -it / 2 } + scaleOut(targetScale = 0.7f) + fadeOut()
                    }
                },
                popTransitionSpec = {
                    slideInHorizontally { -it / 2 } + scaleIn(initialScale = 0.7f) + fadeIn() togetherWith
                    slideOutHorizontally { it }
                },
                predictivePopTransitionSpec = {
                    slideInHorizontally { -it/2 } + scaleIn(initialScale = 0.7f) + fadeIn() togetherWith
                    slideOutHorizontally { it }
                }
            )
        }
    }
}


@Composable
fun NavigationButton(backstack: SnapshotStateList<NavKey>, route: NavKey, name: String, icon: Int) {
    val haptic = LocalHapticFeedback.current
    Button (
        colors =
            if (backstack.last() == route){
                ButtonDefaults.filledTonalButtonColors()
            } else {
                ButtonDefaults.textButtonColors()
            },
        onClick = {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            backstack.clear()
            backstack.add(route)
        }
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Icon(
                painter = painterResource(icon),
                contentDescription = route.toString()
            )
            AnimatedVisibility(backstack.last() == route) {
                Text(text = name)
            }
        }
    }
}