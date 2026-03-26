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

enum class DataDirection {
    Upload,
    Download,
    Both
}

data class UsageQuery (
    val dataType: List<DataType>,
    val dataDirection: DataDirection,
    val dataUID: Int?
)

fun List<DataType>.getName(): Int {
    return if (size > 1) R.string.total
        else if (size == 1) first().getNameResource()
        else R.string.none
}

fun List<DataType>.getIcon(): Int {
    return if (size > 1) R.drawable.data_usage
    else if (size == 1) first().getIconResource()
    else R.drawable.close
}

fun List<DataType>.getNext(): List<DataType> = when {
    isEmpty() -> listOf(Mobile)
    size == 1 && first() == Mobile -> listOf(Wifi)
    size == 1 && first() == Wifi -> listOf(Wifi, Mobile)
    else -> emptyList()
}