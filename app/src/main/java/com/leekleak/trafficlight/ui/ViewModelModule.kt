package com.leekleak.trafficlight.ui

import com.leekleak.trafficlight.database.HistoryPreferenceRepo
import com.leekleak.trafficlight.ui.history.HistoryVM
import com.leekleak.trafficlight.ui.overview.OverviewVM
import com.leekleak.trafficlight.ui.settings.SettingsVM
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val viewModelModule = module {
    viewModel {
        val prefs: HistoryPreferenceRepo = get()
        val (listParam, q1, q2) = runBlocking {
            Triple(prefs.listParam.first(), prefs.query1.first(), prefs.query2.first())
        }
        HistoryVM(get(), prefs, listParam, q1, q2)
    }
    viewModel { SettingsVM() }
    viewModel { OverviewVM(get()) }
}