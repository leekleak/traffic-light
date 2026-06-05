package com.leekleak.trafficlight.util

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

    fun getComparisonValue(): Long {
        if (value < 10) return (ceil(value) * unit.toBits()).toLong()
        if (value < 100) return ((ceil(value / 10f) * 10f) * unit.toBits()).toLong()
        return ((ceil(value / 100f) * 100f) * unit.toBits()).toLong()
    }

    fun getAsUnit(unit: DataSizeUnit): Double {
        return byteValue.toDouble() / unit.toBits().toDouble()
    }

    override fun toString(): String = toString(extraPrecision = false, speed = false, inBits = false)

    fun toString(extraPrecision: Boolean = false, speed: Boolean = false, inBits: Boolean = false): String {
        val parts = toStringParts(extraPrecision = extraPrecision, speed = speed, inBits = inBits)
        return "${parts.first}${parts.second} ${parts.third}"
    }

    fun toStringParts(extraPrecision: Boolean = false, speed: Boolean = false, inBits: Boolean = false): Triple<String, String, String> {
        val bitsMultiplier = if (inBits) 8 else 1
        var displayValue = byteValue.toDouble() * bitsMultiplier
        var i = 0
        val units = DataSizeUnit.entries
        do {
            displayValue /= 1024
            i++
        } while (displayValue >= 1024 && i < units.size - 1)

        if (displayValue >= 1000 && i < units.size - 1) {
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

    companion object {
    }
}