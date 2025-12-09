package com.leekleak.trafficlight.services

import org.koin.dsl.module

val permissionManagerModule = module { single{ PermissionManager(get()) } }