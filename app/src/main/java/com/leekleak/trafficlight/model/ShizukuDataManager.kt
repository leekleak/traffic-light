package com.leekleak.trafficlight.model

import android.os.Build
import android.os.Parcel
import android.telephony.SubscriptionInfo
import com.leekleak.trafficlight.services.PermissionManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import rikka.shizuku.ShizukuBinderWrapper
import rikka.shizuku.SystemServiceHelper.getSystemService
import timber.log.Timber


class ShizukuDataManager(): KoinComponent {
    val preferenceRepo: PreferenceRepo by inject()
    val permissionManager: PermissionManager by inject()
    var enabled = false

    init {
        CoroutineScope(Dispatchers.IO).launch {
            permissionManager.shizukuPermissionFlow.collect {
                enabled = it
            }
        }
    }

    fun getSubscriptionInfos(): List<SubscriptionInfo> {
        if (!enabled) return emptyList()
        val data = Parcel.obtain()
        val reply = Parcel.obtain()
        try {
            data.writeInterfaceToken("com.android.internal.telephony.ISub")
            data.writeString("com.android.shell")

            val binder = ShizukuBinderWrapper(getSystemService("isub"))

            // Really sucks to hardcode, but the method is private and reflection is hard soooooo deal with it.
            // The codes were obtained by installing all supported Android version emulators and pulling their
            // /system/framework/framework.jar
            // Technically it's possible for the codes to be different to to vendor changes, but
            // my Android 16 Samsung skin matched with Android 16 Google skin, so maybe it's fine.
            val code = when { //TRANSACTION_getActiveSubscriptionInfoList
                Build.VERSION.SDK_INT >= 34 -> 5
                else -> 6
            }

            binder.transact(code, data, reply, 0)
            Timber.e("Exception:%s", reply.readException())
            val info = reply.createTypedArrayList(SubscriptionInfo.CREATOR)
            return info ?: listOf()
        } catch (e: Exception) {
            Timber.e(e)
            return listOf()
        } finally {
            data.recycle()
            reply.recycle()
        }
    }

    fun getSubscriberID(subscriptionId: Int): String? {
        if (!enabled) return null
        val data = Parcel.obtain()
        val reply = Parcel.obtain()
        try {
            data.writeInterfaceToken("com.android.internal.telephony.IPhoneSubInfo")
            data.writeInt(subscriptionId)
            data.writeString("com.android.shell")

            val binder = ShizukuBinderWrapper(getSystemService("iphonesubinfo"))

            // For more info, see getSubscriptionInfos()
            val code = when { // TRANSACTION_getSubscriberIdForSubscriber
                Build.VERSION.SDK_INT >= 30 -> 10
                else -> 8
            }

            binder.transact(code, data, reply, 0)
            reply.readException()
            return reply.readString()!!
        } catch (e: Exception) {
            Timber.e(e)
            return null
        } finally {
            data.recycle()
            reply.recycle()
        }
    }



}