package com.leekleak.trafficlight.util

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

data class DataSize (
    val byteValue: Long,
) {
    val value: Double
    val unit: DataSizeUnit
    val precision: Int

    init {
        var i = 0
        var newValue = byteValue.toDouble()
        while (newValue >= 1000 && i < DataSizeUnit.entries.size) {
            newValue = if (newValue < 1024) 1.0 else newValue / 1024
            i++
        }
        value = newValue
        unit = DataSizeUnit.entries.getOrNull(i) ?: DataSizeUnit.YB
        precision = if (value >= 10 || unit == DataSizeUnit.KB) 0 else 1
    }

    fun getComparisonValue(): Long {
        if (value < 10) return (ceil(value) * unit.toBits()).toLong()
        if (value < 100) return ((ceil(value / 10f) * 10f) * unit.toBits()).toLong()
        return ((ceil(value / 100f) * 100f) * unit.toBits()).toLong()
    }

    fun getAsUnit(unit: DataSizeUnit): Double {
        return if (unit == this.unit) value
        else value * 1024.0.pow((this.unit.ordinal - unit.ordinal).toDouble())
    }

    override fun toString(): String = toString(extraPrecision = false, speed = false, inBits = false)

    fun toString(extraPrecision: Boolean = false, speed: Boolean = false, inBits: Boolean = false): String {
        val parts = toStringParts(extraPrecision = extraPrecision, speed = speed, inBits = inBits)
        return "${parts.first}${parts.second} ${parts.third}"
    }

    fun toStringParts(extraPrecision: Boolean = false, speed: Boolean = false, inBits: Boolean = false): Triple<String, String, String> {
        val newDataSize = DataSize(byteValue * if (inBits) 8 else 1)
        return if (newDataSize.byteValue < 1000) {
            Triple((if (newDataSize.byteValue != 0L) "<1" else "0"), "", DataSizeUnit.KB.getName(inBits, speed))
        } else {
            val withPrecision = applyPrecision(newDataSize, extraPrecision)
            Triple(withPrecision.first, withPrecision.second, newDataSize.unit.getName(inBits, speed))
        }
    }

    companion object {
        fun applyPrecision(dataSize: DataSize, extraPrecision: Boolean): Pair<String, String> {
            val newPrecision = if (dataSize.precision > 0 || extraPrecision) 1 else 0
            val parts = BigDecimal(dataSize.value.toString()).toPlainString().split(".")
            val fraction = parts.getOrNull(1)?.substring(0, newPrecision) ?: ""

            return Pair(parts[0], if (fraction != "")".$fraction" else fraction)
        }
    }
}