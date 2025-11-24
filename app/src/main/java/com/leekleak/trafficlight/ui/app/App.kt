package com.leekleak.trafficlight.ui.app

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.leekleak.trafficlight.services.UsageService
import com.leekleak.trafficlight.ui.navigation.NavigationManager
import com.leekleak.trafficlight.ui.permissions.Permissions
import com.leekleak.trafficlight.util.hasBackgroundPermission
import com.leekleak.trafficlight.util.hasNotificationPermission
import com.leekleak.trafficlight.util.hasUsageStatsPermission
import kotlinx.coroutines.delay

@Composable
fun App() {
    val viewModel: AppVM = viewModel()

    val notificationPermission = remember { mutableStateOf(false) }
    val backgroundPermission = remember { mutableStateOf(false) }
    val usagePermission = remember { mutableStateOf(false) }

    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                notificationPermission.value = hasNotificationPermission(context)
                backgroundPermission.value = hasBackgroundPermission(context)
                usagePermission.value = hasUsageStatsPermission(context)

                UsageService.startService(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect (Unit) {
        while (true) {
            if (notificationPermission.value && backgroundPermission.value && usagePermission.value) {
                viewModel.updateDB()
            }
            delay(2000)
        }
    }

    if (notificationPermission.value && backgroundPermission.value && usagePermission.value) {
        NavigationManager()
    } else {
        Permissions(notificationPermission.value, backgroundPermission.value, usagePermission.value)
    }
}