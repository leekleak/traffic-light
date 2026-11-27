package com.leekleak.core.util

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.database.ContentObserver
import android.graphics.Typeface
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.text.format.DateFormat
import android.widget.Toast
import androidx.annotation.DrawableRes
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.createFontFamilyResolver
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.core.net.toUri
import com.leekleak.core.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.Month
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale
import kotlin.enums.enumEntries

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

inline val Dp.toSp: TextUnit
    @Composable get() = with(LocalDensity.current) { this@toSp.toSp() }

fun LocalDateTime.toTimestamp(): Long = atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
fun LocalDate.toTimestamp(): Long = atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
fun fromTimestamp(stamp: Long): LocalDateTime {
    return LocalDateTime.ofInstant(
        Instant.ofEpochMilli(stamp),
        ZoneId.systemDefault()
    )
}

fun LocalTime.toLocaleHourString(is24HourFormat: Boolean, short: Boolean = false): String {
    val pattern = if (is24HourFormat){
        "HH:mm"
    } else {
        if (short) "hh a"
        else "hh:mm a"
    }
    val formatter = DateTimeFormatter.ofPattern(pattern, Locale.getDefault())
    return format(formatter)
}

fun LocalTime.toLocaleHourOnlyString(is24HourFormat: Boolean): String {
    val pattern = if (is24HourFormat) "HH" else "hh a"
    val formatter = DateTimeFormatter.ofPattern(pattern, Locale.getDefault())
    return format(formatter)
}

fun DayOfWeek.getName(style: TextStyle) =
    this.getDisplayName(style, Locale.getDefault()).replaceFirstChar(Char::titlecase)

fun Month.getName(style: TextStyle) =
    this.getDisplayName(style, Locale.getDefault()).replaceFirstChar(Char::titlecase)

inline fun <reified T : Enum<T>> valueOfOrNull(name: String): T? {
    return enumEntries<T>().find { it.name.equals(name, ignoreCase = true) }
}

fun openLink(activity: Activity?, link: String) {
    try {
        activity?.startActivity(
            Intent(
                Intent.ACTION_VIEW,
                link.toUri()
            )
        )
    } catch (_: ActivityNotFoundException) {
        Toast.makeText(activity, R.string.no_web_browser_installed, Toast.LENGTH_LONG).show()
    }
}

@DrawableRes
fun simIconRes(number: Int): Int {
    return when (number) {
        0 -> R.drawable.sim_card_1
        1 -> R.drawable.sim_card_2
        else -> R.drawable.sim_card
    }
}

fun convertFontFamilyToTypeface(context: Context, fontFamily: FontFamily): Typeface {
    val resolver = createFontFamilyResolver(context)

    val result = resolver.resolve(
        fontFamily = fontFamily
    )

    return result.value as Typeface
}

/**
 * Calculates what percentage of ratio 1 is covered by ratio 2
 */
fun overlapRatio(range1: ClosedRange<Long>, range2: ClosedRange<Long>): Double {
    val overlapStart = maxOf(range1.start, range2.start)
    val overlapEnd = minOf(range1.endInclusive, range2.endInclusive)

    val overlapLength = maxOf(0L, overlapEnd - overlapStart).toDouble()
    val range1Length = range1.endInclusive - range1.start

    if (range1Length == 0L) return if (range2.contains(range1.start)) 1.0 else 0.0

    return overlapLength / (range1Length.toDouble())
}

@Composable
fun Modifier.clearFocusOnTap(): Modifier {
    val focusManager = LocalFocusManager.current
    return this.pointerInput(Unit) {
        awaitEachGesture {
            awaitFirstDown(requireUnconsumed = true, pass = PointerEventPass.Final)
            focusManager.clearFocus()
        }
    }
}

@Composable
fun animateAlignmentAsState(
    targetAlignment: Alignment,
): State<Alignment> {
    val biased = targetAlignment as BiasAlignment
    val horizontal by animateFloatAsState(biased.horizontalBias)
    val vertical by animateFloatAsState(biased.verticalBias)
    return remember { derivedStateOf { BiasAlignment(horizontal, vertical) } }
}

@Composable
fun rememberIs24HourFormat(): State<Boolean> {
    val context = LocalContext.current.applicationContext
    val locale = LocalLocale.current.platformLocale
    return produceState(
        initialValue = DateFormat.getBestDateTimePattern(locale, "j").contains('H')
    ) {
        value = withContext(Dispatchers.IO) { DateFormat.is24HourFormat(context) }

        val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) {
                launch { value = withContext(Dispatchers.IO) { DateFormat.is24HourFormat(context) } }
            }
        }
        context.contentResolver.registerContentObserver(
            Settings.System.getUriFor(Settings.System.TIME_12_24), false, observer
        )
        awaitDispose { context.contentResolver.unregisterContentObserver(observer) }
    }
}

fun Any.toAnnotatedString(): AnnotatedString = AnnotatedString(this.toString())
