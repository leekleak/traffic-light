package com.leekleak.trafficlight.ui.navigation

import android.annotation.SuppressLint
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
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
import com.leekleak.trafficlight.BuildConfig
import org.koin.compose.navigation3.koinEntryProvider
import org.koin.core.annotation.KoinExperimentalAPI

private const val WIDESCREEN_BREAKPOINT = 600

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@OptIn(KoinExperimentalAPI::class)
@Composable
fun NavigationManager(navigator: Navigator) {
    val backStack = navigator.backStack

    var showBottomBar by remember { mutableStateOf(false) }
    val currentEntry = navigator.backStack.lastOrNull()

    LaunchedEffect(currentEntry) {
        showBottomBar = mainScreens.contains(currentEntry)
    }

    val entryProvider = koinEntryProvider<Any>()

    val isWidescreen = currentWindowAdaptiveInfoV2()
        .windowSizeClass
        .isWidthAtLeastBreakpoint(WIDESCREEN_BREAKPOINT)

    val navBarPadding = remember(isWidescreen) {
        if (isWidescreen) {
            PaddingValues(end = SIDE_NAVBAR_PADDING)
        } else {
            PaddingValues(bottom = BOTTOM_NAVBAR_PADDING)
        }
    }

    CompositionLocalProvider(LocalNavigationBarPadding provides navBarPadding) {
        Scaffold(
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            bottomBar = {
                if (!isWidescreen) {
                    BottomNavigationBar(navigator, showBottomBar)
                }
            }
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                NavDisplay(
                    backStack = navigator.backStack,
                    onBack = { navigator.goBack() },
                    entryProvider = entryProvider,
                    entryDecorators = listOf(
                        rememberViewModelStoreNavEntryDecorator()
                    ),
                    transitionSpec = {
                        if (backStack.size == 1) fadeIn(tween()) togetherWith fadeOut(tween())
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
                    modifier = Modifier.align(Alignment.BottomEnd),
                )
            }
        }
    }
}

@Composable
private fun BottomNavigationBar(navigator: Navigator, visible: Boolean) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(8.dp),
        contentAlignment = Alignment.Center
    ) {
        AnimatedVisibility(
            visible = visible,
            enter = slideInVertically { it } + fadeIn(tween()),
            exit = slideOutVertically { it } + fadeOut(tween())
        ) {
            HorizontalFloatingToolbar(
                modifier = Modifier.border(
                    width = 1.5.dp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f),
                    MaterialTheme.shapes.extraLargeIncreased
                ),
                expanded = true,
                colors = FloatingToolbarDefaults.standardFloatingToolbarColors().copy(
                    toolbarContainerColor = MaterialTheme.colorScheme.background,
                ),
                content = {
                    NavigationItems(navigator, vertical = false)
                },
            )
        }
    }
}

@Composable
private fun SideNavigationRail(
    navigator: Navigator,
    visible: Boolean,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .navigationBarsPadding()
            .padding(end = 8.dp, bottom = 8.dp)
    ) {
        AnimatedVisibility(
            visible = visible,
            enter = expandHorizontally(expandFrom = Alignment.End) + fadeIn(tween()),
            exit = shrinkHorizontally(shrinkTowards = Alignment.End) + fadeOut(tween()),
        ) {
            VerticalFloatingToolbar(
                modifier = Modifier
                    .width(SIDE_NAVBAR_WIDTH)
                    .border(
                        width = 1.5.dp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f),
                        MaterialTheme.shapes.extraLargeIncreased
                    ),
                expanded = true,
                colors = FloatingToolbarDefaults.standardFloatingToolbarColors().copy(
                    toolbarContainerColor = MaterialTheme.colorScheme.background,
                ),
                content = {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        NavigationItems(navigator, vertical = true)
                    }
                },
            )
        }
    }
}

@Composable
private fun NavigationItems(navigator: Navigator, vertical: Boolean) {
    NavigationButton(navigator, OverviewKey, stringResource(R.string.overview), R.drawable.overview, vertical)
    NavigationButton(navigator, DataPlansKey, stringResource(R.string.plans), R.drawable.sim_card, vertical)
    NavigationButton(navigator, HistoryKey, stringResource(R.string.history), R.drawable.history, vertical)
    if (BuildConfig.IPERF) {
        NavigationButton(navigator, IperfScreenKey, stringResource(R.string.iperf3), R.drawable.speed, vertical)
    }
}

@Composable
fun NavigationButton(
    navigator: Navigator,
    route: NavKey,
    name: String,
    icon: Int,
    vertical: Boolean = false,
) {
    val selected = navigator.current == route
    val horizontalPadding by animateDpAsState(if (selected) 24.dp else 12.dp)
    val verticalPadding by animateDpAsState(if (selected) 12.dp else 8.dp)
    val haptic = LocalHapticFeedback.current

    Button(
        modifier = if (vertical) Modifier.fillMaxWidth() else Modifier,
        colors =
            if (selected) {
                ButtonDefaults.filledTonalButtonColors()
            } else {
                ButtonDefaults.textButtonColors()
            },
        onClick = {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            navigator.setTo(route)
        },
        contentPadding = if (vertical) {
            PaddingValues(horizontal = 4.dp, vertical = verticalPadding)
        } else {
            PaddingValues(horizontal = horizontalPadding, vertical = 8.dp)
        }
    ) {
        if (vertical) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    painter = painterResource(icon),
                    contentDescription = if (selected) null else name
                )
                AnimatedVisibility(
                    visible = selected,
                    enter = expandVertically(expandFrom = Alignment.Top) + fadeIn(tween()),
                    exit = shrinkVertically(shrinkTowards = Alignment.Top) + fadeOut(tween())
                ) {
                    Text(
                        modifier = Modifier.padding(top = 4.dp),
                        style = MaterialTheme.typography.labelMedium,
                        textAlign = TextAlign.Center,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        text = name
                    )
                }
            }
        } else {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    painter = painterResource(icon),
                    contentDescription = if (selected) null else name
                )
                AnimatedVisibility(visible = selected) {
                    Text(
                        modifier = Modifier.padding(start = 4.dp),
                        maxLines = 1,
                        text = name
                    )
                }
            }
        }
    }
}
