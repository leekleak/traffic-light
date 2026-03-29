package com.leekleak.trafficlight.database

import android.content.Context
import android.net.ConnectivityManager
import android.view.View
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import coil3.compose.rememberAsyncImagePainter
import com.leekleak.trafficlight.R
import com.leekleak.trafficlight.model.AppIcon
import com.leekleak.trafficlight.model.AppManager
import com.leekleak.trafficlight.services.UsageService.Companion.getKoin
import org.koin.compose.koinInject

/**
 *
 * Data Type
 *
 */
interface DataType {
    fun getQueryIndex(): Int
    fun getNameResource(): Int
    fun getIconResource(): Int
}

object Wifi : DataType {
    override fun getQueryIndex(): Int = ConnectivityManager.TYPE_WIFI
    override fun getNameResource(): Int = R.string.wifi
    override fun getIconResource(): Int = R.drawable.wifi
}

object Mobile : DataType {
    override fun getQueryIndex(): Int = ConnectivityManager.TYPE_MOBILE
    override fun getNameResource(): Int = R.string.cellular
    override fun getIconResource(): Int = R.drawable.cellular
}

fun List<DataType>.getName(): Int {
    return if (size > 1) R.string.total
    else if (size == 1) first().getNameResource()
    else R.string.none
}

fun List<DataType>.getIcon(): Int {
    return if (size > 1) R.drawable.data_usage
    else if (size == 1) first().getIconResource()
    else R.drawable.close
}

fun List<DataType>.getNext(): List<DataType> = when {
    isEmpty() -> listOf(Mobile)
    size == 1 && first() == Mobile -> listOf(Wifi)
    size == 1 && first() == Wifi -> listOf(Wifi, Mobile)
    else -> emptyList()
}

/**
 *
 * Data Direction
 *
 */
enum class DataDirection {
    Upload,
    Download,
    Bidirectional
}

fun DataDirection.getNext(): DataDirection {
    val nextIndex = (ordinal + 1) % DataDirection.entries.size
    return DataDirection.entries[nextIndex]
}

fun DataDirection.getName(): Int = when(this) {
    DataDirection.Upload -> R.string.upload
    DataDirection.Download -> R.string.download
    DataDirection.Bidirectional -> R.string.bidirectional
}

fun DataDirection.getIcon(): Int = when(this) {
    DataDirection.Upload -> R.drawable.arrow_upward_alt
    DataDirection.Download -> R.drawable.arrow_downward_alt
    DataDirection.Bidirectional -> R.drawable.mobiledata_arrows
}

/**
 *
 * Data UID
 *
 */

data class DataUID(
    val uid: Int? = null
)

fun DataUID.getName(context: Context): String? {
    val appManager: AppManager = getKoin().get()
    return when(this.uid) {
        null -> context.getString(R.string.all_apps)
        else -> appManager.getNameForUID(this.uid)
    }
}

@Composable
fun DataUID.getIcon(tint: Color = LocalContentColor.current) {
    val appManager: AppManager = koinInject()
    var set = false
    if (this.uid != null) {
        val packageName = appManager.getPackageNamesForUID(this.uid)
        if (!packageName.isNullOrEmpty()) {
            set = true
            Image(
                modifier = Modifier.size(24.dp),
                painter = rememberAsyncImagePainter(AppIcon(packageName.first())),
                contentDescription = null
            )
        }
    }
    if (!set) {
        Icon(
            painter = painterResource(if (this.uid == null) R.drawable.apps else R.drawable.help),
            contentDescription = null,
            tint = tint
        )
    }

}

/**
 *
 * Query
 *
 */
data class UsageQuery (
    val dataType: List<DataType>,
    val dataDirection: DataDirection = DataDirection.Bidirectional,
    val dataUID: DataUID = DataUID()
) {
    fun toString(context: Context): String {
        if (dataType.isEmpty()) return context.getString(dataType.getName())
        val parts = buildList {
            add(context.getString(dataType.getName()))
            if (dataDirection != DataDirection.Bidirectional)
                add(context.getString(dataDirection.getName()))
            if (dataUID.uid != null)
                add(dataUID.getName(context))
        }

        val isRtl = context.resources.configuration.layoutDirection == View.LAYOUT_DIRECTION_RTL
        return (if (isRtl) parts.reversed() else parts).joinToString(" · ")
    }
}
