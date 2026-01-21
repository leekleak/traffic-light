package com.leekleak.trafficlight.model

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
        val data = Parcel.obtain()
        val reply = Parcel.obtain()
        try {
            data.writeInterfaceToken("com.android.internal.telephony.ISub")
            data.writeString("com.android.shell")

            val binder = ShizukuBinderWrapper(getSystemService("isub"))

            // TODO: Get transaction code procedurally
            binder.transact(5, data, reply, 0)
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
        val data = Parcel.obtain()
        val reply = Parcel.obtain()
        try {
            data.writeInterfaceToken("com.android.internal.telephony.IPhoneSubInfo")
            data.writeInt(subscriptionId)
            data.writeString("com.android.shell")

            val binder = ShizukuBinderWrapper(getSystemService("iphonesubinfo"))

            // TODO: Get transaction code procedurally
            binder.transact(10, data, reply, 0) // TRANSACTION_getSubscriberIdForSubscriber = 10;
            Timber.e("Exception:%s", reply.readException())
            val string = reply.readString()!!
            Timber.e("SubsrciberID:%s", string)
            return string
        } catch (e: Exception) {
            Timber.e(e)
            return null
        } finally {
            data.recycle()
            reply.recycle()
        }
    }



}