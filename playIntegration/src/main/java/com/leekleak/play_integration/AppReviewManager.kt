package com.leekleak.play_integration

import android.app.Activity
import android.util.Log
import com.google.android.play.core.ktx.launchReview
import com.google.android.play.core.ktx.requestReview
import com.google.android.play.core.review.ReviewManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlin.coroutines.cancellation.CancellationException
import kotlin.time.Duration.Companion.seconds

class AppReviewManager(
    val reviewManager: ReviewManager,
    val preferenceRepo: PlayPreferenceRepo
) {
    suspend fun onAppLaunch(activity: Activity) {
        val stamp = preferenceRepo.reviewPromptStamp.first()
        val now = System.currentTimeMillis()

        if (stamp == -1L) return
        if (stamp == 0L) {
            val fiveDaysLater = now + (5 * 24 * 60 * 60 * 1000L)
            preferenceRepo.setReviewPromptStamp(fiveDaysLater)
            return
        }
        if (now < stamp) return

        delay(10.seconds) // Give 10 seconds to user to use the app

        try {
            val reviewInfo = reviewManager.requestReview()
            reviewManager.launchReview(activity, reviewInfo)
            preferenceRepo.setReviewPromptStamp(-1)
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Log.e("ReviewManager", "Review flow failed: ${e.message}")
        }
    }
}