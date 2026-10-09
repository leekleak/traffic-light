package com.leekleak.trafficlight.database

import android.net.ConnectivityManager
import android.net.LinkProperties
import android.net.Network
import android.net.NetworkRequest
import android.net.TrafficStats
import android.os.Build
import com.leekleak.trafficlight.model.PermissionManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File
import java.io.IOException
import java.util.concurrent.ConcurrentHashMap

class TrafficSnapshotManager(
    private val appPreferenceRepo: AppPreferenceRepo,
    private val connectivityManager: ConnectivityManager,
    private val permissionManager: PermissionManager,
) : AutoCloseable {
    @Volatile private var useFallback: Boolean = TrafficStats.getTotalTxBytes() == TrafficStats.UNSUPPORTED.toLong()
    private val activeInterfaceNames = ConcurrentHashMap<Network, String>()
    val interfaces: Set<String> get() = activeInterfaceNames.values.toSet()
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    val callback = object : ConnectivityManager.NetworkCallback() {
        override fun onLinkPropertiesChanged(
            network: Network,
            linkProperties: LinkProperties
        ) {
            val name = linkProperties.interfaceName

            if (name == null) {
                activeInterfaceNames.remove(network)
            } else {
                activeInterfaceNames[network] = name
            }
        }

        override fun onLost(network: Network) {
            activeInterfaceNames.remove(network)
        }
    }

    init {
        connectivityManager.allNetworks.forEach { network ->
            connectivityManager.getLinkProperties(network)?.interfaceName?.let { name ->
                activeInterfaceNames[network] = name
            }
        }
        scope.launch {
            appPreferenceRepo.forceFallback.collect { force ->
                useFallback = force || TrafficStats.getTotalTxBytes() == TrafficStats.UNSUPPORTED.toLong()
            }
        }

        // Default network request ignores VPNs, so it should (i hope) be safe from double-counting
        connectivityManager.registerNetworkCallback(NetworkRequest.Builder().build(), callback)
    }

    override fun close() {
        scope.cancel()
        runCatching {
            connectivityManager.unregisterNetworkCallback(callback)
        }
    }

    suspend fun getTrafficSnapshot(): TrafficSnapshot {
        return if (useFallback) {
            try {
                fallbackUpdateSnapshot()
            } catch (e: Exception) {
                when (e) {
                    is IOException, is NumberFormatException, is SecurityException -> {
                        Timber.e(e, "Fallback IO error")
                        scope.launch { appPreferenceRepo.setForceFallback(false) }
                        useFallback = false
                        regularUpdateSnapshot()
                    }

                    else -> throw e
                }
            }
        } else {
            regularUpdateSnapshot()
        }
    }

    private suspend fun regularUpdateSnapshot(): TrafficSnapshot = withContext(Dispatchers.IO) {
        val inter = interfaces
        return@withContext if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && permissionManager.internetPermission.value) {
            TrafficSnapshot(
                up = inter.sumOf { TrafficStats.getTxBytes(it).coerceAtLeast(0L) },
                down = inter.sumOf { TrafficStats.getRxBytes(it).coerceAtLeast(0L) },
                interfaces = inter
            )
        } else {
            TrafficSnapshot(
                up = TrafficStats.getTotalTxBytes(),
                down = TrafficStats.getTotalRxBytes(),
                interfaces = inter
            )
        }
    }

    private suspend fun fallbackUpdateSnapshot(): TrafficSnapshot = withContext(Dispatchers.IO) {
        val mobileUp = mobileTxFile.readLongOrZero()
        val mobileDown = mobileRxFile.readLongOrZero()
        val wifiUp = wifiTxFile.readLongOrZero() + ethTxFile.readLongOrZero()
        val wifiDown = wifiRxFile.readLongOrZero() + ethRxFile.readLongOrZero()
        return@withContext TrafficSnapshot(
            up = mobileUp + wifiUp,
            down = mobileDown + wifiDown,
            interfaces = interfaces
        )
    }

    private fun File.readLongOrZero() = if (canRead()) readText().trim().toLong() else 0L

    companion object {
        private val mobileRxFile: File by lazy { File("/sys/class/net/rmnet0/statistics/rx_bytes") }
        private val mobileTxFile: File by lazy { File("/sys/class/net/rmnet0/statistics/tx_bytes") }
        private val wifiRxFile: File by lazy { File("/sys/class/net/wlan0/statistics/rx_bytes") }
        private val wifiTxFile: File by lazy { File("/sys/class/net/wlan0/statistics/tx_bytes") }
        private val ethRxFile: File by lazy { File("/sys/class/net/eth0/statistics/rx_bytes") }
        private val ethTxFile: File by lazy { File("/sys/class/net/eth0/statistics/tx_bytes") }
        suspend fun doesFallbackWork(): Boolean = withContext(Dispatchers.IO) { mobileRxFile.canRead() || wifiRxFile.canRead() || ethRxFile.canRead() }
    }
}

data class TrafficSnapshot(
    val up: Long,
    val down: Long,
    val interfaces: Set<String>
) {
    val total: Long get() = up + down

    operator fun minus(other: TrafficSnapshot): TrafficSnapshot {
        return if (interfaces != other.interfaces)
            TrafficSnapshot(0L, 0L, interfaces)
        else
            TrafficSnapshot(
                up = (up - other.up).coerceAtLeast(0L),
                down = (down - other.down).coerceAtLeast(0L),
                interfaces = interfaces
            )
    }
}
