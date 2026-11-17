package com.leekleak.iperf

import android.content.Context
import android.net.ConnectivityManager
import android.net.LinkProperties
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.wifi.WifiManager
import android.os.Build
import android.text.format.Formatter
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import java.net.Inet4Address

class IperfNetworkMonitor(context: Context) {

    private val connectivityManager = context.getSystemService(ConnectivityManager::class.java)
    private val wifiManager = context.getSystemService(WifiManager::class.java)

    private val _myIp = mutableStateOf<String?>(null)
    val myIp: State<String?> = _myIp

    private val callback = object : ConnectivityManager.NetworkCallback() {
        override fun onLinkPropertiesChanged(
            network: Network,
            linkProperties: LinkProperties
        ) {
            _myIp.value = linkProperties.linkAddresses
                .map { it.address }
                .filterIsInstance<Inet4Address>()
                .mapNotNull { it.hostAddress }
                .joinToString("\n")
        }

        override fun onLost(network: Network) {
            _myIp.value = null
        }
    }

    init {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val request = NetworkRequest.Builder()
                .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
                .build()
            connectivityManager.registerNetworkCallback(request, callback)
        } else {
            _myIp.value = Formatter.formatIpAddress(wifiManager.connectionInfo.ipAddress)
        }
    }
}
