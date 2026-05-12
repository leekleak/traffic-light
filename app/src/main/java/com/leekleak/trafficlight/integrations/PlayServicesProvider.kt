package com.leekleak.trafficlight.integrations

import android.app.Activity

interface PlayServicesProvider {
    suspend fun onAppLaunch(activity: Activity)
}