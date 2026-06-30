package com.leekleak.trafficlight.integrations

import android.app.Activity
import android.content.Context
import com.google.android.libraries.ads.mobile.sdk.MobileAds
import com.google.android.libraries.ads.mobile.sdk.initialization.InitializationConfig
import com.leekleak.play_integration.AppReviewManager
import com.leekleak.trafficlight.BuildConfig
import com.leekleak.trafficlight.database.AppPreferenceRepo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class PlayServicesProviderImpl(
    private val context: Context,
    private val appReviewManager: AppReviewManager,
    private val appPreferenceRepo: AppPreferenceRepo,
    private val scope: CoroutineScope,
): PlayServicesProvider {

    init {
        scope.launch {
            try {
                appPreferenceRepo.ads.filter { it }.first()
                MobileAds.initialize(context, InitializationConfig.Builder(BuildConfig.ADMOB_APP_ID).build())
            } catch (e: IllegalStateException) {
                timber.log.Timber.e(e, "AdMob initialization failed: invalid state")
            } catch (e: SecurityException) {
                timber.log.Timber.e(e, "AdMob initialization failed: permission denied")
            }
        }
    }

    override suspend fun onAppLaunch(activity: Activity) {
        appReviewManager.onAppLaunch(activity)
    }
}
