package com.leekleak.trafficlight.model

import android.annotation.SuppressLint
import androidx.core.text.isDigitsOnly
import com.leekleak.trafficlight.ITrafficLightShizukuService
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
