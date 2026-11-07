package com.leekleak.trafficlight.ui.plans

import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import com.leekleak.core.util.DataSize
import com.leekleak.core.util.DataSizeUnit
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.text.NumberFormat
import java.text.ParsePosition

fun parseSizeNumber(text: String, numberFormat: NumberFormat): Double? {
    val trimmed = text.trim()
    if (trimmed.isEmpty()) return null

    val symbols = (numberFormat as? DecimalFormat)?.decimalFormatSymbols
        ?: DecimalFormatSymbols.getInstance()
    val decimalSeparator = symbols.decimalSeparator
    val alternateSeparator = if (decimalSeparator == '.') ',' else '.'
    val normalized =
        if (trimmed.contains(decimalSeparator)) trimmed
        else trimmed.replace(alternateSeparator, decimalSeparator)

    val position = ParsePosition(0)
    val value = numberFormat.parse(normalized, position) ?: return null
    if (position.index != normalized.length) return null
    return value.toDouble().takeIf { it.isFinite() && it >= 0.0 }
}

fun sizeToBytes(value: Double, unit: DataSizeUnit, metric: Boolean): Long {
    val base = if (metric) 1000.0 else 1024.0
    return (value * unit.toBits(base)).toLong()
}

fun parseSizeBytes(text: String, unit: DataSizeUnit, metric: Boolean, numberFormat: NumberFormat): Long? {
    val value = parseSizeNumber(text, numberFormat) ?: return null
    return sizeToBytes(value, unit, metric)
}

fun formatSizeValue(bytes: Long, unit: DataSizeUnit, metric: Boolean, formatter: DecimalFormat): String {
    return formatter.format(DataSize(bytes).getAsUnit(unit, metric))
}

@Composable
fun rememberSyncedSizeInput(
    bytes: Long,
    unit: DataSizeUnit,
    metric: Boolean,
    formatter: DecimalFormat,
    numberFormat: NumberFormat,
): TextFieldState {
    val state = rememberTextFieldState(formatSizeValue(bytes, unit, metric, formatter))

    LaunchedEffect(bytes, unit, metric) {
        val text = state.text.toString()
        if (parseSizeBytes(text, unit, metric, numberFormat) != bytes) {
            state.setTextAndPlaceCursorAtEnd(formatSizeValue(bytes, unit, metric, formatter))
        }
    }

    return state
}
