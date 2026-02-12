package com.leekleak.trafficlight.model

import android.content.ComponentName
import android.content.ServiceConnection
import android.os.IBinder
import android.os.Parcel
import android.telephony.SubscriptionInfo
import com.leekleak.trafficlight.BuildConfig
import com.leekleak.trafficlight.ITrafficLightShizukuService
import com.leekleak.trafficlight.services.PermissionManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import rikka.shizuku.Shizuku
import rikka.shizuku.ShizukuBinderWrapper
import rikka.shizuku.SystemServiceHelper.getSystemService
import timber.log.Timber


class ShizukuDataManager : KoinComponent {
    val preferenceRepo: PreferenceRepo by inject()
    val permissionManager: PermissionManager by inject()
    var enabled = false

    private var binderMine: ITrafficLightShizukuService? = null

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(componentName: ComponentName, binder: IBinder?) {
            if (binder != null && binder.pingBinder()) {
                binderMine = ITrafficLightShizukuService.Stub.asInterface(binder)
            }
        }

        override fun onServiceDisconnected(componentName: ComponentName) {
            binderMine = null
        }
    }

    private val serviceArgs = Shizuku.UserServiceArgs(
        ComponentName(BuildConfig.APPLICATION_ID, TrafficLightShizukuService::class.java.name))
        .processNameSuffix("traffic_light_shizuku_service")
        .debuggable(BuildConfig.DEBUG)
        .version(BuildConfig.VERSION_CODE)

    init {
        CoroutineScope(Dispatchers.IO).launch {
            permissionManager.shizukuPermissionFlow.collect {
                enabled = it
                if (enabled) {
                    Shizuku.unbindUserService(serviceArgs, connection, true) // Force update service
                    Shizuku.bindUserService(serviceArgs, connection)
                }
            }
        }
    }

    suspend fun getSubscriptionInfos(): List<SubscriptionInfo> {
        if (!enabled) return emptyList()
        val data = Parcel.obtain()
        val reply = Parcel.obtain()
        try {
            data.writeInterfaceToken("com.android.internal.telephony.ISub")
            data.writeString("com.android.shell")

            val binder = ShizukuBinderWrapper(getSystemService("isub"))

            while (binderMine == null) delay(10)
            val code = binderMine!!.subscriptionInfoListTransaction

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

    suspend fun getSubscriberID(subscriptionId: Int): String? {
        if (!enabled) return null
        val data = Parcel.obtain()
        val reply = Parcel.obtain()
        try {
            data.writeInterfaceToken("com.android.internal.telephony.IPhoneSubInfo")
            data.writeInt(subscriptionId)
            data.writeString("com.android.shell")

            val binder = ShizukuBinderWrapper(getSystemService("iphonesubinfo"))

            while (binderMine == null) delay(10)
            val code = binderMine!!.subscriberIDTransaction

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