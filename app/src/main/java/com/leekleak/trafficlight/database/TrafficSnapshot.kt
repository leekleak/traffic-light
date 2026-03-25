package com.leekleak.trafficlight.database

import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.TrafficStats
import android.os.Build
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import timber.log.Timber
import java.io.File
import java.time.LocalDate
import kotlin.math.max

data class DayUsage(
    val date: LocalDate = LocalDate.now(),
    val usages: Map<DataType, Long> = mapOf()
)

data class AppUsage(
    val usage: DayUsage,
    val uid: Int,
    val name: String,
    val packageName: String,
    val drawableResource: Int? = null
)

class TrafficSnapshot (
    val scope: CoroutineScope,
    var lastDown: Long = 0,
    var lastUp: Long = 0,
    var currentDown: Long = 0,
    var currentUp: Long = 0,
) : KoinComponent {
    private val preferenceRepo: PreferenceRepo by inject()
    private val connectivityManager: ConnectivityManager by inject()
    private var useFallback: Boolean = TrafficStats.getTotalTxBytes() == TrafficStats.UNSUPPORTED.toLong()
    private var altVpnWorkaround: Boolean = false

    init {
        combine(preferenceRepo.forceFallback, preferenceRepo.altVpn) { force, alt ->
            useFallback = force || TrafficStats.getTotalTxBytes() == TrafficStats.UNSUPPORTED.toLong()
            altVpnWorkaround = alt
        }.launchIn(scope)
    }
    val totalSpeed: Long
        get() = upSpeed + downSpeed

    val upSpeed: Long
        get() = currentUp - lastUp

    val downSpeed: Long
        get() = currentDown - lastDown

    fun setCurrentAsLast() {
        lastDown = currentDown
        lastUp = currentUp
    }

    fun updateSnapshot() {
        useFallback.let {
            if (it) {
                try {
                    fallbackUpdateSnapshot()
                } catch (e: Exception) {
                    Timber.e("Fallback unsupported: $e")
                    scope.launch { preferenceRepo.setForceFallback(false) }
                    useFallback = false
                }
            } else {
                regularUpdateSnapshot()
            }
        }
    }

    private fun regularUpdateSnapshot() {
        val capabilities = connectivityManager.getNetworkCapabilities(connectivityManager.activeNetwork)
        val runVpnWorkaround = capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_VPN) ?: false &&
                               Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

        if (runVpnWorkaround) {
            if (altVpnWorkaround) { // Less accurate, but fixes split tunneling setups
                currentDown = max(currentDown, TrafficStats.getTotalRxBytes() - TrafficStats.getRxBytes("tun0"))
                currentUp = max(currentUp, TrafficStats.getTotalTxBytes() - TrafficStats.getTxBytes("tun0"))
            } else {
                currentDown = TrafficStats.getRxBytes("tun0")
                currentUp = TrafficStats.getTxBytes("tun0")
            }
        } else {
            currentDown = TrafficStats.getTotalRxBytes()
            currentUp = TrafficStats.getTotalTxBytes()
        }

        /**
         * Fun fact: when switching networks the api sometimes fucks up the values as per
         * https://issuetracker.google.com/issues/37009612
         * Yes. That bug report is from 2014.
         * Yes. It still happens on my Android 16 device.
         */
    }

    private fun fallbackUpdateSnapshot() {
        val mobileUp = mobileTxFile.readLongOrZero()
        val mobileDown = mobileRxFile.readLongOrZero()
        val wifiUp = wifiTxFile.readLongOrZero() + ethTxFile.readLongOrZero()
        val wifiDown = wifiRxFile.readLongOrZero() + ethRxFile.readLongOrZero()
        currentUp = mobileUp + wifiUp
        currentDown = mobileDown + wifiDown
    }

    fun isCurrentSameAsLast(): Boolean = lastDown == currentDown && lastUp == currentUp

    private fun File.readLongOrZero() = if (canRead()) readText().trim().toLong() else 0L

    companion object {
        private val mobileRxFile: File by lazy { File("/sys/class/net/rmnet0/statistics/rx_bytes") }
        private val mobileTxFile: File by lazy { File("/sys/class/net/rmnet0/statistics/tx_bytes") }
        private val wifiRxFile: File by lazy { File("/sys/class/net/wlan0/statistics/rx_bytes") }
        private val wifiTxFile: File by lazy { File("/sys/class/net/wlan0/statistics/tx_bytes") }
        private val ethRxFile: File by lazy { File("/sys/class/net/eth0/statistics/rx_bytes") }
        private val ethTxFile: File by lazy { File("/sys/class/net/eth0/statistics/tx_bytes") }
        fun doesFallbackWork(): Boolean = mobileRxFile.canRead() || wifiRxFile.canRead() || ethRxFile.canRead()
    }
}
