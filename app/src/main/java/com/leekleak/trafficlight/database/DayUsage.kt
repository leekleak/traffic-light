package com.leekleak.trafficlight.database

import android.net.TrafficStats
import com.leekleak.trafficlight.model.PreferenceRepo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import timber.log.Timber
import java.io.File
import java.time.LocalDate

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

data class HourData(
    var upload: Long = 0,
    var download: Long = 0,
    var wifi: Long = 0,
    var cellular: Long = 0
) {
    val total: Long
        get() = upload + download

    fun toHourUsage(timestamp: Long = 0): HourUsage {
        return HourUsage(timestamp,
            wifi,
            cellular
        )
    }

    fun add(other: HourData) {
        upload += other.upload
        download += other.download
        wifi += other.wifi
        cellular += other.cellular
    }
}

data class TrafficSnapshot (
    var lastDown: Long = 0,
    var lastUp: Long = 0,
    var lastMobile: Long = 0,
    var lastWifi: Long = 0,

    var currentDown: Long = 0,
    var currentUp: Long = 0,
    var currentMobile: Long = 0,
    var currentWifi: Long = 0,
) : KoinComponent {
    private val preferenceRepo: PreferenceRepo by inject()
    private var useFallback: Boolean = TrafficStats.getTotalTxBytes() == TrafficStats.UNSUPPORTED.toLong()

    init {
        CoroutineScope(Dispatchers.IO).launch {
            preferenceRepo.forceFallback.collect {
                useFallback = if (it) true
                else TrafficStats.getTotalTxBytes() == TrafficStats.UNSUPPORTED.toLong()
            }
        }
    }
    val totalSpeed: Long
        get() = upSpeed + downSpeed

    val upSpeed: Long
        get() = currentUp - lastUp

    val downSpeed: Long
        get() = currentDown - lastDown

    val mobileSpeed: Long
        get() = currentMobile - lastMobile

    val wifiSpeed: Long
        get() = currentWifi - lastWifi

    fun setCurrentAsLast() {
        lastDown = currentDown
        lastUp = currentUp
        lastMobile = currentMobile
        lastWifi = currentWifi
    }

    fun isCurrentSameAsLast(): Boolean {
        return lastDown == currentDown &&
            lastUp == currentUp &&
            lastMobile == currentMobile &&
            lastWifi == currentWifi
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
        currentDown = TrafficStats.getTotalRxBytes()
        currentUp = TrafficStats.getTotalTxBytes()
        currentMobile = TrafficStats.getMobileRxBytes() + TrafficStats.getMobileTxBytes()
        currentWifi = currentUp + currentDown - currentMobile

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
        currentMobile = mobileUp + mobileDown
        currentWifi = wifiUp + wifiDown
    }

    fun speedToHourData(): HourData = HourData(upSpeed, downSpeed, wifiSpeed, mobileSpeed)

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
