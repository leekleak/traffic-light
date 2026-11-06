package com.leekleak.trafficlight.model

import coil3.ImageLoader
import coil3.request.crossfade
import com.leekleak.trafficlight.database.AppDatabase
import com.leekleak.trafficlight.ui.plans.DataPlanLogic
import org.koin.dsl.module
import org.koin.plugin.module.dsl.single

val managerModule = module {
    single<AppManager>()
    single { AppIconFetcher.Factory(get()) }
    single {
        ImageLoader.Builder(get())
            .components {
                add(get<AppIconFetcher.Factory>())
            }
            .crossfade(true)
            .build()
    }

    single<PermissionManager>()
    single<NetworkUsageManager>()
    single {
        DataPlanManager(
            dao = get<AppDatabase>().dataPlanDao(),
            networkUsageManager = get(),
        )
    }
    single<DataPlanLogic>()
}
