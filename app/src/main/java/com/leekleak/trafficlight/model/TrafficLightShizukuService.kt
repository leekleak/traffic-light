package com.leekleak.trafficlight.model

import android.annotation.SuppressLint
import android.os.Parcel
import android.telephony.SubscriptionInfo
import androidx.core.text.isDigitsOnly
import com.leekleak.trafficlight.ITrafficLightShizukuService
import rikka.shizuku.SystemServiceHelper.getSystemService
import timber.log.Timber
import java.lang.reflect.Field
import kotlin.system.exitProcess

@SuppressLint("LogNotTimber")
class TrafficLightShizukuService : ITrafficLightShizukuService.Stub() {
    override fun destroy() {
        exitProcess(0)
    }

    var subscriberIDTransaction: Int? = null
    override fun getSubscriberIDTransaction(): Int {
        val className = $$"com.android.internal.telephony.IPhoneSubInfo$Stub"
        val methodName = "getSubscriberIdForSubscriber"
        if (subscriberIDTransaction == null) {
            subscriberIDTransaction = getTransactionCode(className, methodName)
        }
        return subscriberIDTransaction ?: 0
    }

    var subscriptionInfoListTransaction: Int? = null
    override fun getSubscriptionInfoListTransaction(): Int {
        val className = $$"com.android.internal.telephony.ISub$Stub"
        val methodName = "getActiveSubscriptionInfoList"
        if (subscriptionInfoListTransaction == null) {
            subscriptionInfoListTransaction = getTransactionCode(className, methodName)
        }
        return subscriptionInfoListTransaction ?: 0
    }

    override fun getSubscriberID(subscriptionId: Int): String? {
        return transactForType(
            systemService = "iphonesubinfo",
            code = getSubscriberIDTransaction(),
            interfaceName = "com.android.internal.telephony.IPhoneSubInfo",
            prepareData = { data -> data.writeInt(subscriptionId) },
            parseReply = { reply -> reply.readString() }
        )
    }

    override fun getSubscriptionInfos(): List<SubscriptionInfo> {
        return transactForType(
            systemService = "isub",
            code = getSubscriptionInfoListTransaction(),
            interfaceName = "com.android.internal.telephony.ISub",
            parseReply = { reply -> reply.createTypedArrayList(SubscriptionInfo.CREATOR) }
        ) ?: listOf()
    }

    fun <T> transactForType(
        systemService: String,
        code: Int,
        interfaceName: String,
        prepareData: (data: Parcel) -> Unit = {},
        parseReply: (reply: Parcel) -> T
    ): T? {
        val data = Parcel.obtain()
        val reply = Parcel.obtain()
        try {
            val binder = getSystemService(systemService)
            data.writeInterfaceToken(interfaceName)
            prepareData(data)
            data.writeString("com.android.shell")

            binder.transact(code, data, reply, 0)
            Timber.e("Exception:%s", reply.readException())
            return parseReply(reply)
        } catch (e: Exception) {
            Timber.e(e)
            return null
        } finally {
            data.recycle()
            reply.recycle()
        }
    }

    fun getTransactionCode(className: String, methodName: String): Int? {
        val fieldName = "TRANSACTION_$methodName"

        try {
            val cls = Class.forName(className)
            var declaredField: Field? = null
            try {
                declaredField = cls.getDeclaredField(fieldName)
            } catch (_: NoSuchFieldException) {
                for (f in cls.declaredFields) {
                    if (f.type != Int::class.javaPrimitiveType) continue

                    val name = f.name
                    if (name.startsWith(fieldName + "_")
                        && name.substring(fieldName.length + 1).isDigitsOnly()
                    ) {
                        declaredField = f
                        break
                    }
                }
            }
            return declaredField?.let {
                declaredField.isAccessible = true
                declaredField.getInt(cls)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }
}
