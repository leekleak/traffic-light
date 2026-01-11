package com.leekleak.trafficlight.database

import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.TrafficStats
import android.os.Build
import com.leekleak.trafficlight.model.PreferenceRepo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import timber.log.Timber
import java.io.File
import java.time.LocalDate
import kotlin.math.max

data class DayUsage(
    val date: LocalDate = LocalDate.now(),
    val hours: MutableMap<Long, HourData> = mutableMapOf(),
    var totalWifi: Long = 0,
    var totalCellular: Long = 0,
) {
    fun categorizeUsage() {
        totalWifi = hours.map { it.value.wifi }.sum()
        totalCellular = hours.map { it.value.cellular }.sum()
    }
}

data class AppUsage(
    val usage: DayUsage,
    val uid: Int,
    val name: String,
    val packageName: String,
)

data class HourData(
    var upload: Long = 0,
    var download: Long = 0,
    var wifi: Long = 0,
    var cellular: Long = 0
) {
    val total: Long
        get() = upload + download
}

data class TrafficSnapshot (
    val connectivityManager: ConnectivityManager,
    var lastDown: Long = 0,
    var lastUp: Long = 0,
    var currentDown: Long = 0,
    var currentUp: Long = 0,
) : KoinComponent {
    private val preferenceRepo: PreferenceRepo by inject()
    private var useFallback: Boolean = TrafficStats.getTotalTxBytes() == TrafficStats.UNSUPPORTED.toLong()
    private var altVpnWorkaround: Boolean = false

    init {
        CoroutineScope(Dispatchers.IO).launch {
            preferenceRepo.forceFallback.collect {
                useFallback = if (it) true
                else TrafficStats.getTotalTxBytes() == TrafficStats.UNSUPPORTED.toLong()
            }
        }
        CoroutineScope(Dispatchers.IO).launch {
            preferenceRepo.altVpn.collect { altVpnWorkaround = it }
        }
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
                    preferenceRepo.setForceFallback(false)
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
        val mobileUp = if (mobileTxFile.canRead()) mobileTxFile.readText().trim().toLong() else 0
        val mobileDown = if (mobileRxFile.canRead()) mobileRxFile.readText().trim().toLong() else 0
        val wifiUp = if (wifiTxFile.canRead()) wifiTxFile.readText().trim().toLong() else 0 +
                     if (ethTxFile.canRead()) ethTxFile.readText().trim().toLong() else 0
        val wifiDown = if (wifiRxFile.canRead()) wifiRxFile.readText().trim().toLong() else 0 +
                       if (ethRxFile.canRead()) ethRxFile.readText().trim().toLong() else 0
        currentUp = mobileUp + wifiUp
        currentDown = mobileDown + wifiDown
    }

    fun isCurrentSameAsLast(): Boolean = lastDown == currentDown && lastUp == currentUp

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
