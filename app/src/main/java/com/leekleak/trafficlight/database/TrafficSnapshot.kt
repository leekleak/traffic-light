package com.leekleak.trafficlight.database

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.TrafficStats
import android.os.SystemClock
import com.leekleak.trafficlight.model.DataUID
import com.leekleak.trafficlight.util.toLocaleHourString
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File
import java.time.LocalDate
import java.time.LocalDateTime

data class DayUsage(
    val date: LocalDate = LocalDate.now(),
    val usage1: Long = 0L,
    val usage2: Long = 0L
) {
    val totalUsage: Long
        get() = usage1 + usage2
}

data class AppUsage(
    val app: DataUID,
    val usage: DayUsage,
)

data class HourUsage(
    val start: LocalDateTime,
    val end: LocalDateTime,
    val usage: DayUsage,
) {
    fun toString(context: Context): String {
        return "${start.toLocalTime().toLocaleHourString(context)} - ${end.toLocalTime().toLocaleHourString(context)}"
    }
}

/**
 * Tracks device network throughput for the live speed notification.
 *
 * Measurement model
 * ------------------
 * "Speed" here means the rate of traffic actually crossing the device's physical
 * radios (Wi-Fi / cellular / Ethernet), not the rate observed at any single
 * interface. That distinction matters once a VPN (traditional, WireGuard, or a
 * local/loopback VPN such as AdGuard) is active: a VPN adds a virtual `tun`-style
 * interface that carries a second, parallel view of the same traffic. Summing
 * every interface (what [TrafficStats.getTotalRxBytes] does) therefore counts
 * VPN'd traffic twice - once on the physical interface, once on the tunnel - and
 * inflates the displayed speed, often close to 2x.
 *
 * To avoid that, [regularUpdateSnapshot] sums counters only for interfaces that
 * back a physical, non-VPN network ([physicalInterfaceNames]), discovered from
 * [ConnectivityManager] rather than assumed names like "wlan0" or "tun0". A VPN's
 * underlying Wi-Fi/cellular network is still reported by the platform as its own
 * `Network` (without `TRANSPORT_VPN`) even while the VPN is active, so this
 * naturally excludes tunnel traffic without ever needing to detect or name the
 * VPN interface itself. It also means split tunneling, VPN reconnects, and VPN
 * on/off transitions don't require any special-casing: the physical interfaces
 * being summed don't change just because a VPN attaches to or detaches from them.
 *
 * Interface changes (Wi-Fi <-> mobile <-> Ethernet) *do* change which interfaces
 * are summed. [bytesPerSecond] guards against treating that as a traffic spike by
 * comparing a source key derived from the current interface set: if it differs
 * from the previous sample's, the measurement is rebaselined (reported as 0)
 * instead of diffing two unrelated counters.
 */
class TrafficSnapshot (
    private val scope: CoroutineScope,
    private val appPreferenceRepo: AppPreferenceRepo,
    private val connectivityManager: ConnectivityManager,
) {
    @Volatile private var lastDown: Long = 0
    @Volatile private var lastUp: Long = 0
    @Volatile private var currentDown: Long = 0
    @Volatile private var currentUp: Long = 0
    @Volatile private var lastSourceKey: String = ""
    @Volatile private var currentSourceKey: String = ""
    @Volatile private var lastElapsedRealtimeMs: Long = SystemClock.elapsedRealtime()
    @Volatile private var currentElapsedRealtimeMs: Long = lastElapsedRealtimeMs
    @Volatile private var useFallback: Boolean = TrafficStats.getTotalTxBytes() == TrafficStats.UNSUPPORTED.toLong()

    init {
        appPreferenceRepo.forceFallback
            .onEach { force -> useFallback = force || TrafficStats.getTotalTxBytes() == TrafficStats.UNSUPPORTED.toLong() }
            .launchIn(scope)
    }

    val totalSpeed: Long
        get() = upSpeed + downSpeed

    val upSpeed: Long
        get() = bytesPerSecond(currentUp, lastUp, currentSourceKey, lastSourceKey, currentElapsedRealtimeMs, lastElapsedRealtimeMs)

    val downSpeed: Long
        get() = bytesPerSecond(currentDown, lastDown, currentSourceKey, lastSourceKey, currentElapsedRealtimeMs, lastElapsedRealtimeMs)

    fun setCurrentAsLast() {
        lastDown = currentDown
        lastUp = currentUp
        lastSourceKey = currentSourceKey
        lastElapsedRealtimeMs = currentElapsedRealtimeMs
    }

    suspend fun updateSnapshot() {
        currentElapsedRealtimeMs = SystemClock.elapsedRealtime()
        if (useFallback) {
            try {
                fallbackUpdateSnapshot()
            } catch (e: java.io.IOException) {
                Timber.e(e, "Fallback IO error")
                scope.launch { appPreferenceRepo.setForceFallback(false) }
                useFallback = false
            } catch (e: NumberFormatException) {
                Timber.e(e, "Fallback number format error")
                scope.launch { appPreferenceRepo.setForceFallback(false) }
                useFallback = false
            }
        } else {
            regularUpdateSnapshot()
        }
    }

    private suspend fun regularUpdateSnapshot() = withContext(Dispatchers.IO) {
        val interfaces = physicalInterfaceNames()
        if (interfaces.isEmpty()) {
            // No physical (Wi-Fi/cellular/Ethernet) network is visible to this app right
            // now - can happen transiently during a network handover, or on an unusual
            // setup where only a VPN network is reported. Total counters are the best
            // remaining source, even though they can double-count VPN traffic; there is
            // no physical-only source left to fall back to.
            currentSourceKey = SOURCE_TOTAL
            currentDown = TrafficStats.getTotalRxBytes()
            currentUp = TrafficStats.getTotalTxBytes()
            return@withContext
        }

        currentSourceKey = interfaces.sorted().joinToString(",")
        currentDown = interfaces.sumOf { it.rxBytesOrZero() }
        currentUp = interfaces.sumOf { it.txBytesOrZero() }

        // Fun fact: when switching networks the API sometimes messes up the values as per
        // https://issuetracker.google.com/issues/37009612
        // Yes. That bug report is from 2014.
        // Yes. It still happens on recent Android versions.
        // The source-key rebaseline in bytesPerSecond() covers a network switch changing
        // which interfaces are summed, but not this: a transient bad reading from the
        // platform itself on the interfaces we're already tracking.
    }

    /**
     * Interfaces backing the device's currently connected physical networks
     * (Wi-Fi, cellular, Ethernet), discovered via [ConnectivityManager] instead of
     * assumed names, so this keeps working on devices where those names differ
     * and stays correct across VPN connect/disconnect: a VPN's own tunnel network
     * always reports `TRANSPORT_VPN` and is excluded, while the physical network
     * underneath it keeps being reported on its own, without that transport.
     *
     * `allNetworks` is deprecated in favor of a registered [ConnectivityManager]
     * callback, but this class is a polling snapshot by design (matching the
     * notification's own poll loop), so a callback-based rewrite isn't warranted
     * here; the deprecated call remains fully functional.
     */
    @Suppress("DEPRECATION")
    private fun physicalInterfaceNames(): List<String> =
        connectivityManager.allNetworks
            .mapNotNull { network ->
                val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return@mapNotNull null
                val isPhysical = !capabilities.hasTransport(NetworkCapabilities.TRANSPORT_VPN) &&
                        (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
                                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET))
                if (!isPhysical) return@mapNotNull null
                connectivityManager.getLinkProperties(network)?.interfaceName
            }
            .distinct()

    private fun String.rxBytesOrZero(): Long =
        TrafficStats.getRxBytes(this).let { if (it == TrafficStats.UNSUPPORTED.toLong()) 0L else it }

    private fun String.txBytesOrZero(): Long =
        TrafficStats.getTxBytes(this).let { if (it == TrafficStats.UNSUPPORTED.toLong()) 0L else it }

    private fun fallbackUpdateSnapshot() {
        currentSourceKey = SOURCE_FALLBACK
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
        private const val SOURCE_TOTAL = "total"
        private const val SOURCE_FALLBACK = "fallback"

        private val mobileRxFile: File by lazy { File("/sys/class/net/rmnet0/statistics/rx_bytes") }
        private val mobileTxFile: File by lazy { File("/sys/class/net/rmnet0/statistics/tx_bytes") }
        private val wifiRxFile: File by lazy { File("/sys/class/net/wlan0/statistics/rx_bytes") }
        private val wifiTxFile: File by lazy { File("/sys/class/net/wlan0/statistics/tx_bytes") }
        private val ethRxFile: File by lazy { File("/sys/class/net/eth0/statistics/rx_bytes") }
        private val ethTxFile: File by lazy { File("/sys/class/net/eth0/statistics/tx_bytes") }
        fun doesFallbackWork(): Boolean = mobileRxFile.canRead() || wifiRxFile.canRead() || ethRxFile.canRead()
    }
}

/**
 * Converts two counter samples into an instantaneous bytes-per-second rate.
 *
 * Returns 0 instead of a computed rate when:
 * - there is no previous sample yet ([lastSourceKey] empty on the very first call),
 * - the measurement source changed between samples (interface handover, VPN
 *   attaching/detaching from the physical interface set, fallback mode toggling) -
 *   the two counters aren't comparable, so this rebaselines instead of reporting
 *   whatever gap happens to exist between two unrelated counters,
 * - the counter went backwards (device reboot, a driver resetting its interface
 *   counters, counter wraparound) - this never derives a negative or
 *   overflow-driven speed from that.
 *
 * Elapsed time is measured directly rather than assumed, since the actual gap
 * between samples varies with scheduling delays.
 */
internal fun bytesPerSecond(
    currentBytes: Long,
    lastBytes: Long,
    currentSourceKey: String,
    lastSourceKey: String,
    currentElapsedRealtimeMs: Long,
    lastElapsedRealtimeMs: Long,
): Long {
    if (currentSourceKey != lastSourceKey) return 0L
    val deltaBytes = currentBytes - lastBytes
    if (deltaBytes <= 0L) return 0L
    val elapsedMs = (currentElapsedRealtimeMs - lastElapsedRealtimeMs).coerceAtLeast(1L)
    return (deltaBytes.toDouble() * 1000.0 / elapsedMs).toLong()
}
