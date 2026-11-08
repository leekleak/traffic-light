package com.leekleak.trafficlight.charts

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.time.temporal.WeekFields
import java.util.Locale
import kotlin.math.ceil

data class ActivityDay(
    val date: LocalDate,
    val usage: Long = 0L,
    val budget: Long = 0L,
)

private val CellSize = 32.dp
private val MarkerThickness = 2.dp
private const val DotScale = 0.8f
private const val ActivityDays = 90
private const val EmptyBrightness = 0f
private const val BudgetBrightness = 0.8f

@Composable
fun ActivityGraph(
    data: List<ActivityDay>,
    periodStart: LocalDate?,
    selectedDate: LocalDate?,
    onSelectDate: (LocalDate) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (data.isEmpty()) return

    val haptic = LocalHapticFeedback.current
    val baseColor = MaterialTheme.colorScheme.surfaceVariant
    val primaryColor = MaterialTheme.colorScheme.primary
    val surfaceColor = MaterialTheme.colorScheme.surface
    val errorColor = MaterialTheme.colorScheme.error

    val scrollState = rememberScrollState()
    val weekFields = remember { WeekFields.of(Locale.getDefault()) }
    val firstDayOfWeek = weekFields.firstDayOfWeek

    val windowStart = remember(data) { data.last().date.minusDays(ActivityDays - 1L) }
    val leadingCells = remember(windowStart, firstDayOfWeek) {
        (windowStart.dayOfWeek.value - firstDayOfWeek.value + 7) % 7
    }
    val paddingCells = remember(data, windowStart, leadingCells) {
        leadingCells + ChronoUnit.DAYS.between(windowStart, data.first().date).toInt().coerceAtLeast(0)
    }
    val weeks = remember(leadingCells) {
        ceil((leadingCells + ActivityDays) / 7.0).toInt()
    }
    val shapeAnimations = remember(data.size) {
        List(data.size) { Animatable(0f) }
    }

    LaunchedEffect(selectedDate, shapeAnimations) {
        shapeAnimations.forEachIndexed { index, animation ->
            val target = if (data.getOrNull(index)?.date == selectedDate) 1f else 0f
            launch { animation.animateTo(target, tween(100)) }
        }
    }

    LaunchedEffect(scrollState.maxValue) {
        if (scrollState.maxValue > 0) scrollState.scrollTo(scrollState.maxValue)
    }

    Box(modifier = modifier.fillMaxWidth(), contentAlignment = Alignment.TopEnd) {
        Canvas(
            modifier = Modifier
                .pointerInput(data, selectedDate, weeks, paddingCells) {
                    detectTapGestures { offset ->
                        val cell = CellSize.toPx()
                        val col = ((offset.x + scrollState.value) / cell).toInt()
                        val row = (offset.y / cell).toInt()
                        if (col !in 0 until weeks || row !in 0..6) return@detectTapGestures

                        val index = col * 7 + row - paddingCells
                        val day = data.getOrNull(index) ?: return@detectTapGestures
                        if (day.date != selectedDate) {
                            haptic.performHapticFeedback(HapticFeedbackType.ContextClick)
                            onSelectDate(day.date)
                        }
                    }
                }
                .horizontalScroll(scrollState)
                .width(CellSize * weeks)
                .height(CellSize * 7)
                .padding(8.dp)
        ) {
            val cell = size.width / weeks
            val dot = cell * DotScale
            val inset = (cell - dot) / 2
            val marker = MarkerThickness.toPx()

            val placeholderDot = dot * 0.4f
            val placeholderColor = primaryColor.copy(alpha = 0.22f)
            for (position in 0 until paddingCells) {
                val col = position / 7
                val row = position % 7
                drawCircle(
                    color = placeholderColor,
                    radius = placeholderDot / 2,
                    center = Offset(col * cell + cell / 2, row * cell + cell / 2)
                )
            }

            data.forEachIndexed { index, day ->
                val position = index + paddingCells
                val col = position / 7
                val row = position % 7
                val left = col * cell + inset
                val top = row * cell + inset

                val ratio = if (day.budget > 0L) {
                    (day.usage.toFloat() / day.budget.toFloat()).coerceAtLeast(0f)
                } else {
                    0f
                }
                val brightness = when {
                    ratio <= 0f -> EmptyBrightness
                    ratio >= 1f -> 1f
                    else -> 0.15f + ratio * (BudgetBrightness - 0.15f)
                }
                val dotColor = lerp(baseColor, primaryColor, brightness)

                val shapeProgress = shapeAnimations.getOrNull(index)?.value ?: 0f
                val corner = dot / 2 - (dot / 4) * shapeProgress

                drawRoundRect(
                    color = dotColor,
                    topLeft = Offset(left, top),
                    size = Size(dot, dot),
                    cornerRadius = CornerRadius(corner, corner)
                )

                if (day.date.dayOfMonth == 1) {
                    drawCircle(
                        color = surfaceColor,
                        radius = placeholderDot / 2,
                        center = Offset(left + dot / 2, top + dot / 2)
                    )
                }
            }

            val markerIndex = periodStart?.let { start ->
                data.indexOfFirst { it.date == start }
            } ?: -1
            if (markerIndex >= 0) {
                val current = markerIndex + paddingCells
                if (current > 0) {
                    val previous = current - 1
                    val previousCol = previous / 7
                    val currentCol = current / 7

                    if (previousCol == currentCol) {
                        val y = (current % 7) * cell
                        drawRect(
                            color = errorColor,
                            topLeft = Offset(currentCol * cell, y - marker / 2),
                            size = Size(cell, marker)
                        )
                    } else {
                        val x = currentCol * cell
                        drawRect(
                            color = errorColor,
                            topLeft = Offset(x - marker / 2, 0f),
                            size = Size(marker, size.height)
                        )
                    }
                }
            }
        }
    }
}
