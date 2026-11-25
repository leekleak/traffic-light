package com.leekleak.trafficlight.ui.navigation

import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.leekleak.core.ui.navigation.LocalNavigator
import com.leekleak.trafficlight.ui.history.History
import com.leekleak.trafficlight.ui.history.HistoryVM
import com.leekleak.trafficlight.ui.overview.Overview
import com.leekleak.trafficlight.ui.overview.OverviewVM
import com.leekleak.trafficlight.ui.permissions.UsagePermissionRequest
import com.leekleak.trafficlight.ui.permissions.UsagePermissionRequestVM
import com.leekleak.trafficlight.ui.plans.DataPlanConfig
import com.leekleak.trafficlight.ui.plans.DataPlanConfigVM
import com.leekleak.trafficlight.ui.plans.DataPlans
import com.leekleak.trafficlight.ui.plans.DataPlansVM
import com.leekleak.trafficlight.ui.settings.LibraryLicenseScreen
import com.leekleak.trafficlight.ui.settings.Settings
import com.leekleak.trafficlight.ui.settings.SettingsVM
import com.leekleak.trafficlight.ui.tools.Tools
import com.leekleak.trafficlight.ui.tools.notification.NotificationSettingsScreen
import com.leekleak.trafficlight.ui.tools.notification.NotificationSettingsScreenVM
import com.leekleak.trafficlight.ui.tools.notification.NotificationScreen
import com.leekleak.trafficlight.ui.tools.notification.NotificationScreenVM
import com.leekleak.trafficlight.ui.tools.overlay.OverlayScreen
import com.leekleak.trafficlight.ui.tools.overlay.OverlayScreenVM
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.annotation.KoinExperimentalAPI
import org.koin.core.parameter.parametersOf
import org.koin.dsl.module
import org.koin.dsl.navigation3.navigation

@OptIn(KoinExperimentalAPI::class)
val navigationModule = module {

    navigation<OverviewKey> {
        val viewModel: OverviewVM = koinViewModel()
        val uiState by viewModel.uiState
        Overview(
            navigator = LocalNavigator.current,
            uiState = uiState,
            refresh = viewModel::refresh,
            setDataType = viewModel::setDataType,
            setRange = viewModel::setRange,
        )
    }
    navigation<DataPlansKey> { key ->
        val viewModel: DataPlansVM = koinViewModel<DataPlansVM>()
        val navigator = LocalNavigator.current
        val uiState by viewModel.uiState.collectAsStateWithLifecycle()
        DataPlans(
            uiState = uiState,
            initialPlanHash = key.selectedPlanHash,
            selectDataPlan = viewModel::selectDataPlan,
            grantShizuku = viewModel::grantShizuku,
            goToPlanConfig = { plan -> navigator.goTo(PlanConfigKey(plan)) },
            refresh = viewModel::refresh
        )
    }
    navigation<HistoryKey> {
        val viewModel: HistoryVM = koinViewModel<HistoryVM>()
        val uiState by viewModel.uiState
        History(
            uiState = uiState,
            appManager = get(),
            refresh = viewModel::refresh,
            updateDateQuery = viewModel::updateDateQuery,
            updateQuery = viewModel::updateQuery,
            updateQueries = viewModel::updateQueries,
            updateListQuery = viewModel::updateListQuery,
            persistFilters = viewModel::persistFilters,
            resetFilters = viewModel::resetFilters,
            openApp = viewModel::openApp,
            openPackageSettings = viewModel::openPackageSettings,
        )
    }
    navigation<ToolsKey> { Tools(LocalNavigator.current) }
    navigation<SettingsKey> {
        val viewModel: SettingsVM = koinViewModel()
        val uiState by viewModel.uiState
        Settings(
            navigator = LocalNavigator.current,
            uiState = uiState,
            setTheme = viewModel::setTheme,
            setBlur = viewModel::setBlur,
            setSpeedMetric = viewModel::setSpeedMetric,
            setSizeMetric = viewModel::setSizeMetric,
        )
    }
    navigation<UsagePermissionRequestKey> {
        val viewModel: UsagePermissionRequestVM = koinViewModel()
        val uiState by viewModel.uiState
        UsagePermissionRequest(
            navigator = LocalNavigator.current,
            uiState = uiState,
            askUsagePermission = viewModel::askUsagePermission,
            openUsagePermissionHelp = viewModel::openUsagePermissionHelp,
            askBackgroundPermission = viewModel::askBackgroundPermission,
            openBatteryPermissionHelp = viewModel::openBatteryPermissionHelp,
        )
    }
    navigation<PlanConfigKey> { key ->
        val viewModel: DataPlanConfigVM = koinViewModel(key = key.dataPlan.hashedSubscriberID) { parametersOf(key.dataPlan) }
        DataPlanConfig(LocalNavigator.current, viewModel)
    }
    navigation<NotificationSettingsKey> {
        val viewModel: NotificationScreenVM = koinViewModel()
        val uiState by viewModel.uiState
        NotificationScreen(
            navigator = LocalNavigator.current,
            uiState = uiState,
            setNotification = viewModel::setNotification,
            setNotificationType = viewModel::setNotificationType,
            setSpeedBits = viewModel::setSpeedBits,
            setNotificationAccent = viewModel::setNotificationAccent,
        )
    }
    navigation<NotificationAdvancedSettingsKey> {
        val viewModel: NotificationSettingsScreenVM = koinViewModel()
        val uiState by viewModel.uiState
        NotificationSettingsScreen(
            navigator = LocalNavigator.current,
            uiState = uiState,
            setSpeedThreshold = viewModel::setSpeedThreshold,
            setSpeedThresholdKb = viewModel::setSpeedThresholdKb,
            setModeAOD = viewModel::setModeAOD,
            setForceFallback = viewModel::setForceFallback,
        )
    }
    navigation<OverlaySettingsKey> {
        val viewModel: OverlayScreenVM = koinViewModel()
        val uiState by viewModel.uiState
        OverlayScreen(
            navigator = LocalNavigator.current,
            uiState = uiState,
            setOverlay = viewModel::setOverlay,
            onOverlayPermissionChanged = viewModel::onOverlayPermissionChanged,
        )
    }
    navigation<LibraryLicenseScreen> { LibraryLicenseScreen(LocalNavigator.current) }
}
