package com.leekleak.trafficlight.services.notifications

import com.leekleak.trafficlight.database.DataPlan
import kotlinx.coroutines.CoroutineScope
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val notificationModule = module {
    factory { (scope: CoroutineScope, id: Int) ->
        SpeedNotification(
            serviceScope = scope,
            context = androidContext(),
            notificationId = id,
            networkUsageManager = get(),
            notificationManager = get(),
            connectivityManager = get(),
            appPreferenceRepo = get()
        )
    }
    factory { (scope: CoroutineScope, id: Int, dataPlan: DataPlan) ->
        PlanNotification(
            serviceScope = scope,
            context = androidContext(),
            notificationId = id,
            dataPlan = dataPlan,
            networkUsageManager = get(),
            notificationManager = get(),
        )
    }
}