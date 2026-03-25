package com.leekleak.trafficlight.database

import android.net.ConnectivityManager
import com.leekleak.trafficlight.R

interface DataType {
    fun getQueryIndex(): Int
    fun getNameResource(): Int
    fun getIconResource(): Int
}

object Wifi : DataType {
    override fun getQueryIndex(): Int = ConnectivityManager.TYPE_WIFI
    override fun getNameResource(): Int = R.string.wifi
    override fun getIconResource(): Int = R.drawable.wifi
}

object Mobile : DataType {
    override fun getQueryIndex(): Int = ConnectivityManager.TYPE_MOBILE
    override fun getNameResource(): Int = R.string.cellular
    override fun getIconResource(): Int = R.drawable.cellular
}

object Bluetooth : DataType {
    override fun getQueryIndex(): Int = ConnectivityManager.TYPE_BLUETOOTH
    override fun getNameResource(): Int = R.string.cellular
    override fun getIconResource(): Int = R.drawable.cellular
}

object Ethernet : DataType {
    override fun getQueryIndex(): Int = ConnectivityManager.TYPE_ETHERNET
    override fun getNameResource(): Int = R.string.cellular
    override fun getIconResource(): Int = R.drawable.cellular
}