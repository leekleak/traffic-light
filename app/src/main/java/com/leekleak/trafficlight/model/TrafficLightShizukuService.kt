package com.leekleak.trafficlight.model

import android.util.Log
import com.leekleak.trafficlight.ITrafficLightShizukuService
import rikka.shizuku.SystemServiceHelper
import kotlin.system.exitProcess

class TrafficLightShizukuService : ITrafficLightShizukuService.Stub() {
    override fun destroy() {
        exitProcess(0)
    }

    override fun getSubscriberIDTransaction(): Int {
        val code = SystemServiceHelper.getTransactionCode($$"com.android.internal.telephony.IPhoneSubInfo$Stub", "getSubscriberIdForSubscriber")
        Log.e("SubID", code.toString())
        return code ?: 0
    }

    override fun getActiveSubscriptionInfoList(): Int {
        val code = SystemServiceHelper.getTransactionCode($$"com.android.internal.telephony.ISub$Stub", "getActiveSubscriptionInfoList")
        Log.e("SubInfo", code.toString())
        return code ?: 0
    }
}
