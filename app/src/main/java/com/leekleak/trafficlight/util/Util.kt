package com.leekleak.trafficlight.util

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.text.format.DateFormat
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import com.leekleak.trafficlight.R
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDateTime
import java.time.Month
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
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

fun LocalDateTime.toTimestamp(): Long = toInstant(currentTimezone()).toEpochMilli()
fun fromTimestamp(stamp: Long): LocalDateTime {
    return LocalDateTime.ofInstant(
        Instant.ofEpochMilli(stamp),
        ZoneId.systemDefault()
    )
}

fun LocalDateTime.toLocaleHourString(context: Context): String {
    val pattern = if (DateFormat.is24HourFormat(context)) "HH" else "hh a"
    val formatter = DateTimeFormatter.ofPattern(pattern, Locale.getDefault())
    return format(formatter)
}

fun DayOfWeek.getName(style: TextStyle) =
    this.getDisplayName(style, Locale.getDefault()).replaceFirstChar(Char::titlecase)

fun Month.getName(style: TextStyle) =
    this.getDisplayName(style, Locale.getDefault()).replaceFirstChar(Char::titlecase)

fun LazyListScope.categoryTitle(onBackPressed: (() -> Unit)? = null, text: @Composable (() -> String)){
    item { CategoryTitleText(text(), onBackPressed) }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun CategoryTitleText(text: String, onBackPressed: (() -> Unit)? = null) {
    Row (verticalAlignment = Alignment.CenterVertically){
        if (onBackPressed != null) {
            IconButton(onClick = { onBackPressed.invoke() }) {
                Icon(
                    painter = painterResource(R.drawable.arrow_back),
                    contentDescription = stringResource(R.string.go_back),
                )
            }
        }
        Text(
            modifier = Modifier.padding(8.dp),
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            text = text
        )
    }
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

fun openLink(activity: Activity?, link: String) {
    activity?.startActivity(
        Intent(
            Intent.ACTION_VIEW,
            link.toUri()
        )
    )
}

