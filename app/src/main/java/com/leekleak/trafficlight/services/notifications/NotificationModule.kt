package com.leekleak.trafficlight.services.notifications

import com.leekleak.trafficlight.database.DataPlan
import com.leekleak.trafficlight.database.TrafficSnapshotManager
import kotlinx.coroutines.CoroutineScope
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module
import org.koin.plugin.module.dsl.factory

val notificationModule = module {
    factory<TrafficSnapshotManager>()

    factory<SpeedNotification>()
    factory { (scope: CoroutineScope, id: Int, dataPlan: DataPlan) ->
        PlanNotification(
            serviceScope = scope,
            context = androidContext(),
            notificationId = id,
            dataPlan = dataPlan,
            networkUsageManager = get(),
            notificationManager = get(),
            appPreferenceRepo = get(),
        )
    }
}