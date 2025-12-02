package com.leekleak.trafficlight.database

data class HourUsage(
    val timestamp: Long,
    val totalWifi: Long,
    val totalCellular: Long
)