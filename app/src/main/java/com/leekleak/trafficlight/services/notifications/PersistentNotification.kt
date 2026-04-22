package com.leekleak.trafficlight.services.notifications

import androidx.lifecycle.LifecycleService

interface PersistentNotification {
    fun start()
    fun cancel()
    fun startForeground(service: LifecycleService)
    fun screenStateChange(on: Boolean)
    fun getId(): Int
}