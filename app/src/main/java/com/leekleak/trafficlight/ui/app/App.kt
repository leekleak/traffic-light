package com.leekleak.trafficlight.ui.app

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.leekleak.trafficlight.services.PermissionManager
import com.leekleak.trafficlight.services.UsageService
import com.leekleak.trafficlight.ui.navigation.NavigationManager
import com.leekleak.trafficlight.ui.permissions.Permissions
import org.koin.compose.koinInject

@Composable
fun App() {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val permissionManager: PermissionManager = koinInject()
    val hasAllPermissions by permissionManager.usagePermissionFlow.collectAsState(false)

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                permissionManager.update()

                UsageService.startService(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    if (hasAllPermissions) {
        NavigationManager()
    } else {
        Permissions()
    }
}