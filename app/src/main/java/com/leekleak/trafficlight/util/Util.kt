package com.leekleak.trafficlight.util

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDateTime
import java.time.Month
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.TextStyle
import java.util.Locale

enum class NetworkType {
    Cellular,
    Wifi,
}

fun String.clipAndPad(length: Int): String {
    return if (this.length >= length) {
        this.substring(0, length)
    } else {
        this.padEnd(length, ' ')
    }
}

inline val Dp.px: Float
    @Composable get() = with(LocalDensity.current) { this@px.toPx() }

inline val Int.toDp: Dp
    @Composable get() = with(LocalDensity.current) { this@toDp.toDp() }

fun padHour(time: Number): String {
    if (time.toLong() % 6 == 0L) return time.toString().padStart(2, '0')
    return ""
}

fun LocalDateTime.toTimestamp(): Long = toInstant(currentTimezone()).toEpochMilli()
fun fromTimestamp(stamp: Long): LocalDateTime {
    return LocalDateTime.ofInstant(
        Instant.ofEpochMilli(stamp),
        ZoneId.systemDefault()
    )
}

fun DayOfWeek.getName(style: TextStyle) =
    this.getDisplayName(style, Locale.getDefault()).replaceFirstChar(Char::titlecase)

fun Month.getName(style: TextStyle) =
    this.getDisplayName(style, Locale.getDefault()).replaceFirstChar(Char::titlecase)

fun LazyListScope.categoryTitle(text: @Composable (() -> String)) = item { CategoryTitleText(text()) }

@Composable
fun CategoryTitleText(text: String) {
    Text(
        modifier = Modifier.padding(8.dp),
        fontSize = 24.sp,
        fontWeight = FontWeight.Bold,
        text = text
    )
}

fun LazyListScope.categoryTitleSmall(text: @Composable (() -> String)) = item { CategoryTitleSmallText(text()) }

@Composable
fun CategoryTitleSmallText(text: String) {
    Text(
        modifier = Modifier.padding(8.dp),
        text = text,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.tertiary
    )
}

@Composable
fun WideScreenWrapper(content: @Composable () -> Unit) {
    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = Alignment.TopCenter
    ) {
        Box(Modifier.widthIn(20.dp, 500.dp)) {
            content()
        }
    }
}

fun currentTimezone(): ZoneOffset = ZoneId.systemDefault().rules.getOffset(Instant.now())

