package com.leekleak.trafficlight.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import kotlinx.serialization.Serializable
import java.math.BigDecimal
import kotlin.math.ceil
import kotlin.math.pow

enum class DataSizeUnit {
    B, KB, MB, GB, TB, // Actual sizes
    PB, EB, ZB, YB;  // Mental disorders

    fun toBits(base: Double = 1024.0): Long = base.pow(this.ordinal).toLong()
    fun getName(inBits: Boolean, speed: Boolean): String {
        return (if (inBits) name.replace("B", "b") else name) + (if (speed) "/s" else "")
    }
}

val Long.toKb get() = this / 1024L

val LocalSizeMetric = compositionLocalOf { false }
val LocalSpeedMetric = compositionLocalOf { false }

@Composable
fun DataSize.formatted(extraPrecision: Boolean = false, speed: Boolean = false, inBits: Boolean = false): String {
    val metric = if (speed) LocalSpeedMetric.current else LocalSizeMetric.current
    return this.toString(extraPrecision, speed, inBits, metric)
}

@Composable
fun DataSize.formattedParts(extraPrecision: Boolean = false, speed: Boolean = false, inBits: Boolean = false): Triple<String, String, String> {
    val metric = if (speed) LocalSpeedMetric.current else LocalSizeMetric.current
    return this.toStringParts(extraPrecision, speed, inBits, metric)
}

@Serializable
data class DataSize (
    val byteValue: Long,
) {
    val value: Double
    val unit: DataSizeUnit
    val precision: Int

    init {
        var i = 0
        var newValue = byteValue.toDouble()
        while (newValue >= 1024 && i < DataSizeUnit.entries.size - 1) {
            newValue /= 1024
            i++
        }
        value = newValue
        unit = DataSizeUnit.entries[i]
        precision = if (value >= 10 || unit == DataSizeUnit.KB) 0 else 1
    }

    fun value(metric: Boolean = false): Double {
        if (!metric) return value
        val base = 1000.0
        var i = 0
        var newValue = byteValue.toDouble()
        while (newValue >= base && i < DataSizeUnit.entries.size - 1) {
            newValue /= base
            i++
        }
        return newValue
    }

    fun unit(metric: Boolean = false): DataSizeUnit {
        if (!metric) return unit
        val base = 1000.0
        var i = 0
        var newValue = byteValue.toDouble()
        while (newValue >= base && i < DataSizeUnit.entries.size - 1) {
            newValue /= base
            i++
        }
        return DataSizeUnit.entries[i]
    }

    fun getComparisonValue(metric: Boolean = false): Long {
        val base = if (metric) 1000.0 else 1024.0
        val v = value(metric)
        val u = unit(metric)
        if (v < 10) return (ceil(v) * u.toBits(base)).toLong()
        if (v < 100) return ((ceil(v / 10f) * 10f) * u.toBits(base)).toLong()
        return ((ceil(v / 100f) * 100f) * u.toBits(base)).toLong()
    }

    fun getAsUnit(unit: DataSizeUnit, metric: Boolean): Double {
        val base = if (metric) 1000.0 else 1024.0
        return byteValue.toDouble() / unit.toBits(base).toDouble()
    }

    companion object {
        fun kb(value: Long) = DataSize(value * 1024L)
    }

    override fun toString(): String = toString(extraPrecision = false, speed = false, inBits = false)

    fun toString(extraPrecision: Boolean = false, speed: Boolean = false, inBits: Boolean = false, metric: Boolean = false): String {
        val parts = toStringParts(extraPrecision = extraPrecision, speed = speed, inBits = inBits, metric = metric)
        return "${parts.first}${parts.second} ${parts.third}"
    }

    fun toStringParts(extraPrecision: Boolean = false, speed: Boolean = false, inBits: Boolean = false, metric: Boolean = false): Triple<String, String, String> {
        val base = if (metric) 1000.0 else 1024.0
        val bitsMultiplier = if (inBits) 8 else 1
        var displayValue = byteValue.toDouble() * bitsMultiplier
        var i = 0
        val units = DataSizeUnit.entries
        do {
            displayValue /= base
            i++
        } while (displayValue >= base && i < units.size - 1)

        val threshold = if (metric) base - 1 else 1000.0
        if (displayValue >= threshold && i < units.size - 1) {
            displayValue = 1.0
            i++
        }

        val displayUnit = units[i]
        val precision = if (extraPrecision || (displayValue < 10 && displayUnit != DataSizeUnit.KB)) 1 else 0
        
        val parts = BigDecimal(displayValue.toString()).toPlainString().split(".")
        val fraction = parts.getOrNull(1)?.substring(0, precision) ?: ""
        
        val first = if (byteValue != 0L && displayValue < 1 && i == 1) "<1" else parts[0]
        val second = if (fraction != "") ".$fraction" else ""
        val third = displayUnit.getName(inBits, speed)

        return Triple(first, second, third)
    }
}
