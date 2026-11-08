package com.leekleak.trafficlight.ui.navigation

import android.annotation.SuppressLint
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FloatingToolbarDefaults
import androidx.compose.material3.HorizontalFloatingToolbar
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalFloatingToolbar
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfoV2
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.ui.NavDisplay
import com.leekleak.core.R
import com.leekleak.core.ui.navigation.BOTTOM_NAVBAR_PADDING
import com.leekleak.core.ui.navigation.LocalNavigationBarPadding
import com.leekleak.core.ui.navigation.Navigator
import com.leekleak.core.ui.navigation.SIDE_NAVBAR_PADDING
import com.leekleak.core.ui.navigation.SIDE_NAVBAR_WIDTH
import com.leekleak.core.ui.theme.LocalBlurEnabled
import dev.chrisbanes.haze.HazeInput
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.blur.HazeBlurStyle
import dev.chrisbanes.haze.blur.HazeColorEffect
import dev.chrisbanes.haze.blur.hazeBlur
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.rememberHazeState
import org.koin.compose.navigation3.koinEntryProvider
import org.koin.core.annotation.KoinExperimentalAPI

private const val WIDESCREEN_BREAKPOINT = 600

private val PillOutlineWidth = 1.dp

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@OptIn(KoinExperimentalAPI::class)
@Composable
fun NavigationManager(navigator: Navigator) {
    val currentEntry = navigator.backStack.lastOrNull()

    var showBottomBar by remember { mutableStateOf(mainScreens.contains(currentEntry)) }

    LaunchedEffect(currentEntry) {
        showBottomBar = mainScreens.contains(currentEntry)
    }

    val isWidescreen = currentWindowAdaptiveInfoV2()
        .windowSizeClass
        .isWidthAtLeastBreakpoint(WIDESCREEN_BREAKPOINT)

    val navigationBarHaze = rememberHazeState()

    CompositionLocalProvider(
        LocalNavigationBarPadding provides remember(isWidescreen) {
            if (isWidescreen) {
                PaddingValues(end = SIDE_NAVBAR_PADDING)
            } else {
                PaddingValues(bottom = BOTTOM_NAVBAR_PADDING)
            }
        }
    ) {
        Scaffold(
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            bottomBar = {
                if (!isWidescreen) {
                    BottomNavigationBar(navigator, showBottomBar, navigationBarHaze)
                }
            }
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                NavDisplay(
                    backStack = navigator.backStack,
                    modifier = Modifier.hazeSource(navigationBarHaze),
                    onBack = { navigator.goBack() },
                    entryProvider = koinEntryProvider<Any>(),
                    entryDecorators = listOf(
                        rememberViewModelStoreNavEntryDecorator()
                    ),
                    transitionSpec = {
                        if (navigator.backStack.size == 1) fadeIn(tween()) togetherWith fadeOut(tween())
                        else {
                            slideInHorizontally { it } togetherWith
                            slideOutHorizontally { -it / 2 } + scaleOut(targetScale = 0.7f) + fadeOut()
                        }
                    },
                    popTransitionSpec = {
                        slideInHorizontally { -it / 2 } + scaleIn(initialScale = 0.7f) + fadeIn(tween()) togetherWith
                        slideOutHorizontally { it }
                    },
                    predictivePopTransitionSpec = {
                        slideInHorizontally { -it/2 } + scaleIn(initialScale = 0.7f) + fadeIn(tween()) togetherWith
                        slideOutHorizontally { it }
                    }
                )
                SideNavigationRail(
                    navigator = navigator,
                    visible = isWidescreen && showBottomBar,
                    hazeState = navigationBarHaze,
                    modifier = Modifier.align(Alignment.BottomEnd),
                )
            }
        }
    }
}

@Composable
private fun BottomNavigationBar(
    navigator: Navigator,
    visible: Boolean,
    hazeState: HazeState,
) {
    val shape = CircleShape

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(vertical = 8.dp, horizontal = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        AnimatedVisibility(
            visible = visible,
            enter = slideInVertically { it } + fadeIn(tween()) + scaleIn(tween(), 0.7f),
            exit = slideOutVertically { it } + fadeOut(tween()) + scaleOut(tween(), 0.7f)
        ) {
            HorizontalFloatingToolbar(
                modifier = Modifier
                    .height(64.dp)
                    .pillGlass(hazeState, shape)
                    .border(PillOutlineWidth, navBarBorderColor(), shape),
                expanded = true,
                colors = FloatingToolbarDefaults.standardFloatingToolbarColors().copy(
                    toolbarContainerColor = Color.Transparent,
                ),
                shape = shape,
                contentPadding = PaddingValues(6.dp),
                content = {
                    navigationItems().forEach { item ->
                        NavigationButton(
                            modifier = Modifier.weight(1f),
                            navigator = navigator,
                            route = item.route,
                            name = item.name,
                            icon = item.icon,
                            vertical = false,
                        )
                    }
                },
            )
        }
    }
}

@Composable
private fun Modifier.pillGlass(hazeState: HazeState, shape: Shape): Modifier {
    val screen = MaterialTheme.colorScheme.background
    val tint = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
    val solid = tint.compositeOver(screen)

    if (!LocalBlurEnabled.current) return clip(shape).background(solid, shape)

    return clip(shape).hazeBlur(
        HazeInput.Sources(hazeState),
        remember(tint, screen) {
            HazeBlurStyle {
                backgroundColor(screen)
                colorEffects(listOf(HazeColorEffect.tint(solid.copy(alpha = 0.8f))))
                fallbackColorEffect(HazeColorEffect.tint(solid))
            }
        },
    )
}

@Composable
private fun navBarBorderColor(): Color {
    return MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
}

@Composable
private fun SideNavigationRail(
    navigator: Navigator,
    visible: Boolean,
    hazeState: HazeState,
    modifier: Modifier = Modifier,
) {
    val shape = MaterialTheme.shapes.extraLarge

    Box(
        modifier = modifier
            .navigationBarsPadding()
            .padding(end = 8.dp, bottom = 8.dp)
    ) {
        AnimatedVisibility(
            visible = visible,
            enter = slideInHorizontally { it } + fadeIn(tween()) + scaleIn(tween(), 0.7f),
            exit = slideOutHorizontally { it } + fadeOut(tween()) + scaleOut(tween(), 0.7f)
        ) {
            VerticalFloatingToolbar(
                modifier = Modifier
                    .width(SIDE_NAVBAR_WIDTH)
                    .pillGlass(hazeState, shape)
                    .border(PillOutlineWidth, navBarBorderColor(), shape),
                expanded = true,
                colors = FloatingToolbarDefaults.standardFloatingToolbarColors().copy(
                    toolbarContainerColor = Color.Transparent,
                ),
                shape = shape,
                content = {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        navigationItems().forEach { item ->
                            NavigationButton(
                                navigator = navigator,
                                route = item.route,
                                name = item.name,
                                icon = item.icon,
                                vertical = true,
                            )
                        }
                    }
                },
            )
        }
    }
}

private class NavigationItemSpec(
    val route: NavKey,
    val name: String,
    val icon: Int,
)

@Composable
private fun navigationItems(): List<NavigationItemSpec> = listOf(
    NavigationItemSpec(OverviewKey, stringResource(R.string.overview), R.drawable.overview),
    NavigationItemSpec(DataPlansKey, stringResource(R.string.plans), R.drawable.sim_card),
    NavigationItemSpec(HistoryKey, stringResource(R.string.history), R.drawable.history),
    NavigationItemSpec(ToolsKey, stringResource(R.string.tools), R.drawable.handyman),
)

@Composable
fun NavigationButton(
    modifier: Modifier = Modifier,
    navigator: Navigator,
    route: NavKey,
    name: String,
    icon: Int,
    vertical: Boolean = false,
) {
    val selected = navigator.current == route
    val haptic = LocalHapticFeedback.current

    Button(
        modifier = modifier
            .then(if (vertical) Modifier.fillMaxWidth() else Modifier)
            .height(52.dp),
        shape = CircleShape,
        colors = ButtonDefaults.buttonColors(
            containerColor = if (selected) {
                MaterialTheme.colorScheme.primaryContainer.copy(
                    alpha = 0.72f
                )
            } else {
                Color.Transparent
            },
            contentColor = if (selected) {
                MaterialTheme.colorScheme.onPrimaryContainer
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
        ),
        border = if (selected) {
            BorderStroke(
                PillOutlineWidth,
                MaterialTheme.colorScheme.primary.copy(alpha = 0.45f),
            )
        } else {
            null
        },
        onClick = {
            haptic.performHapticFeedback(HapticFeedbackType.Confirm)
            navigator.setTo(route)
        },
        contentPadding = PaddingValues(horizontal = 4.dp),
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Icon(
                painter = painterResource(icon),
                contentDescription = null,
                modifier = Modifier.size(22.dp),
            )
            Text(
                text = name,
                style = MaterialTheme.typography.labelSmall,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
