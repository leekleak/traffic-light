package com.leekleak.trafficlight.services

import kotlinx.coroutines.CoroutineScope
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val notificationModule = module {
    factory { (scope: CoroutineScope) ->
        SpeedNotification(scope, androidContext(), get(), get(), get(), get())
    }
}