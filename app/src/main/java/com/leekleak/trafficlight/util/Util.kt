package com.leekleak.trafficlight.util

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.text.format.DateFormat
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ButtonGroupScope
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import com.leekleak.trafficlight.R
import dev.chrisbanes.haze.HazeProgressive
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.materials.ExperimentalHazeMaterialsApi
import dev.chrisbanes.haze.materials.HazeMaterials
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.Month
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale
import kotlin.enums.enumEntries

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

fun LocalTime.toLocaleHourString(context: Context, short: Boolean = false): String {
    val pattern = if (DateFormat.is24HourFormat(context)) "HH:mm"
        else (if (short) "hh a" else "hh:mm a")
    val formatter = DateTimeFormatter.ofPattern(pattern, Locale.getDefault())
    return format(formatter)
}

fun DayOfWeek.getName(style: TextStyle) =
    this.getDisplayName(style, Locale.getDefault()).replaceFirstChar(Char::titlecase)

fun Month.getName(style: TextStyle) =
    this.getDisplayName(style, Locale.getDefault()).replaceFirstChar(Char::titlecase)

@OptIn(ExperimentalHazeMaterialsApi::class)
@Composable
fun PageTitle(
    onBackPressed: (() -> Unit)? = null,
    hazeState: HazeState? = null,
    text: String,
    customElement: @Composable (BoxScope.() -> Unit)? = null,
){
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                hazeState?.let {
                    Modifier.hazeEffect(state = it, style = HazeMaterials.ultraThin()) {
                        progressive = HazeProgressive.verticalGradient(startIntensity = 1f, endIntensity = 0f)
                    }
                } ?: Modifier
            )
    ) {
        Box(Modifier.statusBarsPadding().padding(horizontal = 16.dp).padding(bottom = 6.dp).fillMaxWidth()) {
            CategoryTitleText(text, onBackPressed)
            customElement?.let { it() }
        }
    }
}

fun LazyListScope.categoryTitle(text: @Composable (() -> String)){
    item { CategoryTitleText(text()) }
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
        color = colorScheme.tertiary
    )
}

@Composable
fun WideScreenWrapper(content: @Composable () -> Unit) {
    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = Alignment.TopCenter
    ) {
        Box(Modifier.widthIn(20.dp, 500.dp).clipToBounds()) {
            content()
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
fun ButtonGroupScope.iconButton(
    text: String? = null,
    showBadge: Boolean = false,
    onClick: () -> Unit,
    icon: @Composable (() -> Unit)?
) {
    customItem(
        buttonGroupContent = {
            val source = remember { MutableInteractionSource() }
            val press by source.collectIsPressedAsState()
            val cornerRadius by animateDpAsState(if (press) 24.dp else 6.dp)
            IconButton(
                modifier = Modifier.animateWidth(source),
                colors = IconButtonDefaults.iconButtonColors(
                    containerColor = colorScheme.surfaceContainer,
                    contentColor = colorScheme.onSurfaceVariant
                ),
                shape = RoundedCornerShape(cornerRadius),
                interactionSource = source,
                onClick = onClick
            ) {
                icon?.let {
                    BadgedBox({ if (showBadge) { Badge() } }) {
                        it()
                    }
                }
                text?.let {
                    Text(it)
                }
            }
        },
        menuContent = {}
    )
}

inline fun <reified T : Enum<T>> valueOfOrNull(name: String): T? {
    return enumEntries<T>().find { it.name.equals(name, ignoreCase = true) }
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

