package com.leekleak.trafficlight.ui.navigation

import com.leekleak.trafficlight.model.PermissionManager
import org.koin.dsl.module

val navigationModule = module {

    single {
        val permissionManager: PermissionManager = get()
        permissionManager.update()
        val destination = if (permissionManager.usagePermissionFlow.value) Overview else UsagePermissionRequest
        Navigator(startDestination = destination)
    }
}