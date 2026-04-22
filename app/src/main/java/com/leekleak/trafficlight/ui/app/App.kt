package com.leekleak.trafficlight.ui.app

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.leekleak.trafficlight.model.PermissionManager
import com.leekleak.trafficlight.services.notifications.NotificationService
import com.leekleak.trafficlight.ui.navigation.NavigationManager
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

@Composable
fun App() {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()

    val permissionManager: PermissionManager = koinInject()

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME || event == Lifecycle.Event.ON_START) {
                permissionManager.update()
                scope.launch {
                    NotificationService.startService(context)
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    NavigationManager()
}