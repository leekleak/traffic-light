package com.leekleak.trafficlight.integrations

import android.app.Activity
import com.leekleak.play_integration.AppReviewManager
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class PlayServicesProviderImpl: PlayServicesProvider, KoinComponent {
    private val appReviewManager: AppReviewManager by inject()

    override suspend fun onAppLaunch(activity: Activity) = appReviewManager.onAppLaunch(activity)
}