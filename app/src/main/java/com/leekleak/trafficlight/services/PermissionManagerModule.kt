package com.leekleak.trafficlight.services

import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val permissionManagerModule = module {
    single{ PermissionManager(androidContext(), get()) }
}