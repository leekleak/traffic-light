package com.leekleak.trafficlight.database

import com.leekleak.trafficlight.model.DataUID
import com.leekleak.core.util.toLocaleHourString
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
    fun toString(is24HourFormat: Boolean): String {
        return "${start.toLocalTime().toLocaleHourString(is24HourFormat)} - ${end.toLocalTime().toLocaleHourString(is24HourFormat)}"
    }
}