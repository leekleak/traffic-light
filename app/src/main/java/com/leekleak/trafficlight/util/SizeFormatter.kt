package com.leekleak.trafficlight.util

import java.math.RoundingMode
import kotlin.math.ceil
import kotlin.math.pow

enum class DataSizeUnit {
    B, KB, MB, GB, TB, // Actual sizes
    PB, EB, ZB, YB;  // Mental disorders
}

data class DataSize (
    var value: Double,
    var unit: DataSizeUnit = DataSizeUnit.B,
    val speed: Boolean = false,
    var precision: Int = 1
) {
    val precisionDec: Double
        get() = 10.0.pow(precision)

    init {
        var i = DataSizeUnit.entries.indexOf(unit)
        while (value >= 1000 && i < DataSizeUnit.entries.size) {
            value = if (value < 1024) 1.0 else value / 1024
            i++
        }
        unit = DataSizeUnit.entries[i]
    }

    fun applyPrecision(size: Double): String {
        return if (precision == 0 || size.toInt() >= 100) size.toInt().toString()
            else ((size * precisionDec).toInt().toFloat() / precisionDec).toString() // Round down
    }

    fun getComparisonValue(): DataSize {
        if (value < 10) return copy(value = ceil(value), unit = unit, speed = speed, precision = precision)
        if (value < 100) return copy(value = ceil(value / 10f) * 10f, unit = unit, speed = speed, precision = precision)
        return copy(value = ceil(value / 100f) * 100f, unit = unit, speed = speed, precision = precision)
    }

    fun getBitValue(): Long {
        return (value * 1024f.pow(DataSizeUnit.entries.indexOf(unit))).toLong()
    }

    fun getAsUnit(unit: DataSizeUnit): Double {
        return if (unit == this.unit) value
        else value * 1024.0.pow((this.unit.ordinal - unit.ordinal).toDouble())
    }

    override fun toString(): String {
        val outValue = if (value < 1024 && unit == DataSizeUnit.B) {
            unit = DataSizeUnit.KB
             if (value > 0) "<1" else "0"
        } else applyPrecision(value)
        return "$outValue $unit${if (speed) "/s" else ""}"
    }

    fun toStringParts(uppercase: Boolean = true): List<String> {
        val newValue = value.toBigDecimal().setScale(precision, RoundingMode.HALF_UP).toString()
        val newUnit = unit.toString().let { if (!uppercase) it.replace("B", "b") else it }
        return listOf(
            newValue.substringBefore('.'),
            if (newValue.contains('.')) newValue.substringAfter('.') else "",
            newUnit + if (speed) "/s" else ""
        )
    }
}

class SizeFormatter {
    var asBits = false

    fun format(size: Number, precision: Int, speed: Boolean = false): String {
        val realSize = size.toDouble() * if (asBits && speed) 8.0 else 1.0
        val dataSize = DataSize(realSize, DataSizeUnit.B, speed, precision)
        return "$dataSize".let { if (asBits && speed) it.replace("B", "b") else it }
    }

    fun partFormat(size: Number, speed: Boolean = false): List<String> {
        val realSize = size.toDouble() * if (asBits && speed) 8.0 else 1.0
        val dataSize = DataSize(realSize, DataSizeUnit.B, speed, 2)
        dataSize.precision = if (dataSize.value < 10 && dataSize.unit >= DataSizeUnit.MB) 1 else 0
        return dataSize.toStringParts(!asBits || !speed)
    }
}