package com.leekleak.trafficlight.ui.navigation

import org.koin.dsl.module

val navigationModule = module {

    single {
        Navigator(startDestination = Blank)
    }
}