package com.leekleak.trafficlight.iperf

import com.leekleak.iperf.ui.IperfScreen
import com.leekleak.iperf.ui.IperfScreenVM
import com.leekleak.trafficlight.ui.navigation.IperfScreenKey
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.annotation.KoinExperimentalAPI
import org.koin.dsl.module
import org.koin.dsl.navigation3.navigation

@OptIn(KoinExperimentalAPI::class)
val iperfNavigationModule = module {
    navigation<IperfScreenKey> {
        val viewModel: IperfScreenVM = koinViewModel()
        IperfScreen(get(), viewModel)
    }
}
