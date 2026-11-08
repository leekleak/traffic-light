package com.leekleak.trafficlight.ui.app

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation3.runtime.rememberNavBackStack
import com.leekleak.core.ui.navigation.LocalNavigator
import com.leekleak.core.ui.navigation.Navigator
import com.leekleak.trafficlight.model.PermissionManager
import com.leekleak.trafficlight.ui.navigation.NavigationManager
import com.leekleak.trafficlight.ui.navigation.OverviewKey
import com.leekleak.trafficlight.ui.navigation.UsagePermissionRequestKey
import androidx.compose.runtime.collectAsState

@Composable
fun App(
    permissionManager: PermissionManager
) {
    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME || event == Lifecycle.Event.ON_START) {
                permissionManager.update()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val startDestination = if (permissionManager.usagePermission.collectAsState().value) OverviewKey else UsagePermissionRequestKey
    val backStack = rememberNavBackStack(startDestination)
    val navigator = remember(backStack) { Navigator(backStack) }

    CompositionLocalProvider(LocalNavigator provides navigator) {
        NavigationManager(navigator)
    }
}
