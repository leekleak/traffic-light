package com.leekleak.play_integration

import android.app.Activity
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.google.android.play.core.review.testing.FakeReviewManager
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class AppReviewManagerTest {

    private lateinit var context: Context
    private lateinit var fakeReviewManager: FakeReviewManager
    private lateinit var preferenceRepo: PlayPreferenceRepo
    private lateinit var appReviewManager: AppReviewManager
    private lateinit var activity: Activity

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        fakeReviewManager = FakeReviewManager(context)
        preferenceRepo = mockk(relaxed = true)
        appReviewManager = AppReviewManager(fakeReviewManager, preferenceRepo)
        activity = Robolectric.buildActivity(Activity::class.java).setup().get()
    }

    @Test
    fun `onAppLaunch - when stamp is -1, does nothing`() = runTest {
        every { preferenceRepo.reviewPromptStamp } returns flowOf(-1L)

        appReviewManager.onAppLaunch(activity)

        coVerify(exactly = 0) { preferenceRepo.setReviewPromptStamp(any()) }
    }

    @Test
    fun `onAppLaunch - when stamp is 0, sets stamp to 5 days later`() = runTest {
        every { preferenceRepo.reviewPromptStamp } returns flowOf(0L)

        appReviewManager.onAppLaunch(activity)

        coVerify { preferenceRepo.setReviewPromptStamp(match { it > System.currentTimeMillis() }) }
    }

    @Test
    fun `onAppLaunch - when stamp is in future, does nothing`() = runTest {
        val futureStamp = System.currentTimeMillis() + 100000
        every { preferenceRepo.reviewPromptStamp } returns flowOf(futureStamp)

        appReviewManager.onAppLaunch(activity)

        coVerify(exactly = 0) { preferenceRepo.setReviewPromptStamp(any()) }
    }

    @Test
    fun `onAppLaunch - when stamp is in past, launches review and sets stamp to -1`() = runTest {
        val pastStamp = System.currentTimeMillis() - 100000
        every { preferenceRepo.reviewPromptStamp } returns flowOf(pastStamp)

        // runTest automatically skips delay(10s)
        appReviewManager.onAppLaunch(activity)

        coVerify { preferenceRepo.setReviewPromptStamp(-1) }
    }
}
