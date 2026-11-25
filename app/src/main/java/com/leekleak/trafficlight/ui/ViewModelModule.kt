package com.leekleak.trafficlight.ui

import com.leekleak.trafficlight.ui.history.HistoryVM
import com.leekleak.trafficlight.ui.overview.OverviewVM
import com.leekleak.trafficlight.ui.plans.DataPlanConfigVM
import com.leekleak.trafficlight.ui.plans.DataPlansVM
import com.leekleak.trafficlight.ui.permissions.UsagePermissionRequestVM
import com.leekleak.trafficlight.ui.settings.SettingsVM
import com.leekleak.trafficlight.ui.tools.notification.NotificationSettingsScreenVM
import com.leekleak.trafficlight.ui.tools.notification.NotificationScreenVM
import com.leekleak.trafficlight.ui.tools.overlay.OverlayScreenVM
import org.koin.dsl.module
import org.koin.plugin.module.dsl.viewModel

val viewModelModule = module {
    viewModel<OverviewVM>()
    viewModel<DataPlansVM>()
    viewModel<HistoryVM>()
    viewModel<DataPlanConfigVM>()
    viewModel<NotificationScreenVM>()
    viewModel<NotificationSettingsScreenVM>()
    viewModel<OverlayScreenVM>()
    viewModel<SettingsVM>()
    viewModel<UsagePermissionRequestVM>()
}