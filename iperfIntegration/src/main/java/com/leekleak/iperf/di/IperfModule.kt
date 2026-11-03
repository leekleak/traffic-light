package com.leekleak.iperf.di

import android.content.Context
import androidx.room3.Room
import com.leekleak.iperf.IPerf3Provider
import com.leekleak.iperf.data.IPerfEntryDao
import com.leekleak.iperf.data.IPerfEntryDatabase
import com.leekleak.iperf.ui.IperfClientVM
import com.leekleak.iperf.ui.IperfLogScreen
import com.leekleak.iperf.ui.IperfLogScreenKey
import com.leekleak.iperf.ui.IperfLogViewModel
import com.leekleak.iperf.ui.IperfScreenVM
import com.leekleak.iperf.ui.IperfServerVM
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.annotation.KoinExperimentalAPI
import org.koin.dsl.module
import org.koin.dsl.navigation3.navigation
import org.koin.plugin.module.dsl.single
import org.koin.plugin.module.dsl.viewModel

@OptIn(KoinExperimentalAPI::class)
val iperfModule = module {
    single<IPerf3Provider>()

    single<IPerfEntryDatabase> {
        Room.databaseBuilder(
            get<Context>(),
            IPerfEntryDatabase::class.java,
            "iperf_servers"
        )
            .addMigrations()
            .build()
    }
    single<IPerfEntryDao> { get<IPerfEntryDatabase>().iPerfEntryDao() }

    viewModel<IperfScreenVM>()
    viewModel<IperfClientVM>()
    viewModel<IperfServerVM>()

    navigation<IperfLogScreenKey> { key ->
        val viewModel: IperfLogViewModel = if (key.isServer) {
            koinViewModel<IperfServerVM>()
        } else {
            koinViewModel<IperfClientVM>()
        }
        IperfLogScreen(get(), key.events, viewModel)
    }
}
