package com.leekleak.play_integration

import com.google.android.play.core.review.ReviewManagerFactory
import org.koin.dsl.module

val playModule = module {
    single { ReviewManagerFactory.create(get()) }
    single { PlayPreferenceRepo(get()) }
    single { AppReviewManager(get(), get()) }
}