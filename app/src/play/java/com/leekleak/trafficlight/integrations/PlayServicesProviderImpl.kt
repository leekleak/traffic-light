package com.leekleak.trafficlight.integrations

import android.app.Activity
import com.leekleak.play_integration.AppReviewManager

class PlayServicesProviderImpl(
    private val appReviewManager: AppReviewManager,
): PlayServicesProvider {

    override suspend fun onAppLaunch(activity: Activity) {
        appReviewManager.onAppLaunch(activity)
    }
}
