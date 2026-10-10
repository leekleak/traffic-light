package com.leekleak.trafficlight.ui

import com.leekleak.trafficlight.ui.history.HistoryVM
import com.leekleak.trafficlight.ui.iperf.IperfClientVM
import com.leekleak.trafficlight.ui.iperf.IperfScreenVM
import com.leekleak.trafficlight.ui.overview.OverviewVM
import com.leekleak.trafficlight.ui.plans.DataPlanConfigVM
import com.leekleak.trafficlight.ui.plans.DataPlansVM
import com.leekleak.trafficlight.ui.settings.SettingsVM
import org.koin.dsl.module
import org.koin.plugin.module.dsl.viewModel

val viewModelModule = module {
    viewModel<OverviewVM>()
    viewModel<DataPlansVM>()
    viewModel<HistoryVM>()
    viewModel<IperfScreenVM>()
    viewModel<IperfClientVM>()
    viewModel<DataPlanConfigVM>()
    viewModel<SettingsVM>()
}