package com.leekleak.trafficlight.ui.navigation

import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.leekleak.core.ui.navigation.LocalNavigator
import com.leekleak.trafficlight.ui.history.History
import com.leekleak.trafficlight.ui.overview.Overview
import com.leekleak.trafficlight.ui.plans.DataPlanConfig
import com.leekleak.trafficlight.ui.plans.DataPlanConfigVM
import com.leekleak.trafficlight.ui.plans.DataPlans
import com.leekleak.trafficlight.ui.plans.DataPlansVM
import com.leekleak.trafficlight.ui.settings.LibraryLicenseScreen
import com.leekleak.trafficlight.ui.settings.NotificationSettingsScreen
import com.leekleak.trafficlight.ui.settings.OverlaySettingsScreen
import com.leekleak.trafficlight.ui.settings.Settings
import com.leekleak.trafficlight.ui.settings.UsagePermissionRequest
import com.leekleak.trafficlight.ui.tools.Tools
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.annotation.KoinExperimentalAPI
import org.koin.core.parameter.parametersOf
import org.koin.dsl.module
import org.koin.dsl.navigation3.navigation

@OptIn(KoinExperimentalAPI::class)
val navigationModule = module {

    navigation<OverviewKey> { Overview(LocalNavigator.current, get()) }
    navigation<DataPlansKey> {
        val viewModel: DataPlansVM = koinViewModel<DataPlansVM>()
        val navigator = LocalNavigator.current
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
    navigation<ToolsKey> { Tools(LocalNavigator.current) }
    navigation<SettingsKey> { Settings(LocalNavigator.current, get(), get(), get()) }
    navigation<UsagePermissionRequestKey> { UsagePermissionRequest(LocalNavigator.current, get()) }
    navigation<PlanConfigKey> { key ->
        val viewModel: DataPlanConfigVM = koinViewModel(key = key.dataPlan.hashedSubscriberID) { parametersOf(key.dataPlan) }
        DataPlanConfig(LocalNavigator.current, viewModel)
    }
    navigation<NotificationSettingsKey> { NotificationSettingsScreen(LocalNavigator.current, get(), get(), get()) }
    navigation<OverlaySettingsKey> { OverlaySettingsScreen(LocalNavigator.current, get(), get()) }
    navigation<LibraryLicenseScreen> { LibraryLicenseScreen(LocalNavigator.current) }
}
