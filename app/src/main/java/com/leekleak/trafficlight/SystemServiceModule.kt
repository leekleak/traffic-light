package com.leekleak.trafficlight

import android.app.NotificationManager
import android.app.usage.NetworkStatsManager
import android.content.Context
import android.content.Context.CONNECTIVITY_SERVICE
import android.content.Context.NOTIFICATION_SERVICE
import android.net.ConnectivityManager
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val systemServiceModule = module {
    single<NetworkStatsManager> {
        androidContext().getSystemService(Context.NETWORK_STATS_SERVICE) as NetworkStatsManager
    }

    single<ConnectivityManager> {
        androidContext().getSystemService(CONNECTIVITY_SERVICE) as ConnectivityManager
    }

    single<NotificationManager> {
        androidContext().getSystemService(NOTIFICATION_SERVICE) as NotificationManager
    }
}