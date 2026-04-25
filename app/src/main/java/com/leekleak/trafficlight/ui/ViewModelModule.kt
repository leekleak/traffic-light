package com.leekleak.trafficlight.ui

import com.leekleak.trafficlight.ui.history.HistoryVM
import com.leekleak.trafficlight.ui.overview.OverviewVM
import com.leekleak.trafficlight.ui.settings.SettingsVM
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val viewModelModule = module {
    viewModel { OverviewVM(get()) }
    viewModel { HistoryVM(get(), get(), get()) }
    viewModel { SettingsVM(get()) }
}