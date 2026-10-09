package com.leekleak.trafficlight.ui.navigation

import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.leekleak.trafficlight.model.PermissionManager
import com.leekleak.trafficlight.ui.history.History
import com.leekleak.trafficlight.ui.iperf.IperfScreen
import com.leekleak.trafficlight.ui.overview.Overview
import com.leekleak.trafficlight.ui.plans.DataPlanConfig
import com.leekleak.trafficlight.ui.plans.DataPlanConfigVM
import com.leekleak.trafficlight.ui.plans.DataPlans
import com.leekleak.trafficlight.ui.plans.DataPlansVM
import com.leekleak.trafficlight.ui.settings.LibraryLicenseScreen
import com.leekleak.trafficlight.ui.settings.NotificationSettingsScreen
import com.leekleak.trafficlight.ui.settings.Settings
import com.leekleak.trafficlight.ui.settings.UsagePermissionRequest
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.annotation.KoinExperimentalAPI
import org.koin.core.parameter.parametersOf
import org.koin.dsl.module
import org.koin.dsl.navigation3.navigation

@OptIn(KoinExperimentalAPI::class)
val navigationModule = module {

    single {
        val permissionManager: PermissionManager = get()
        permissionManager.update()
        val destination = if (permissionManager.usagePermission.value) OverviewKey else UsagePermissionRequestKey
        Navigator(startDestination = destination)
    }
    navigation<OverviewKey> { Overview(get(), get()) }
    navigation<DataPlansKey> {
        val viewModel: DataPlansVM = koinViewModel<DataPlansVM>()
        val navigator: Navigator = get()
        val uiState by viewModel.uiState.collectAsStateWithLifecycle()
        DataPlans(
            uiState = uiState,
            selectDataPlan = viewModel::selectDataPlan,
            disableShizukuHint = viewModel::disableShizukuHint,
            goToPlanConfig = { plan -> navigator.goTo(PlanConfigKey(plan)) },
            refresh = viewModel::refresh
        )
    }
    navigation<HistoryKey> { History(get(), get()) }
    navigation<IperfScreenKey> { IperfScreen(get()) }
    navigation<SettingsKey> { Settings(get(), get(), get(), get(), get()) }
    navigation<UsagePermissionRequestKey> { UsagePermissionRequest(get(), get()) }
    navigation<PlanConfigKey> { key ->
        val viewModel: DataPlanConfigVM = koinViewModel(key = key.dataPlan.hashedSubscriberID) { parametersOf(key.dataPlan) }
        DataPlanConfig(get(), viewModel)
    }
    navigation<NotificationSettingsKey> { NotificationSettingsScreen(get(), get(), get()) }
    navigation<LibraryLicenseScreen> { LibraryLicenseScreen(get()) }
}