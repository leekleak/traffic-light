package com.leekleak.trafficlight.database

import android.content.Context
import android.net.ConnectivityManager.TYPE_MOBILE
import android.net.ConnectivityManager.TYPE_WIFI
import android.view.View
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.leekleak.trafficlight.R
import com.leekleak.trafficlight.model.AppManager.Companion.allApp
import com.leekleak.trafficlight.model.DataUID

/**
 *
 * Data Type
 *
 */
enum class DataType(
    @StringRes val nameRes: Int,
    @DrawableRes val iconRes: Int,
    val queryIndex: Int
) : DropdownItem {
    Mobile(R.string.cellular, R.drawable.cellular, TYPE_MOBILE),
    Wifi(R.string.wifi, R.drawable.wifi, TYPE_WIFI),
    None(R.string.none, R.drawable.close, 0);

    @Composable
    override fun DropdownMenuItem(onClick: () -> Unit) {
        DropdownMenuItem(
            text = { Text(stringResource(nameRes)) },
            onClick = { onClick() },
            leadingIcon = { Icon(painterResource(iconRes), null) },
        )
    }
}

/**
 *
 * Data Direction
 *
 */
enum class DataDirection(
    @StringRes val nameRes: Int,
    @DrawableRes val iconRes: Int
) : DropdownItem {
    Bidirectional(R.string.bidirectional, R.drawable.mobiledata_arrows),
    Download(R.string.download, R.drawable.arrow_downward_alt),
    Upload(R.string.upload, R.drawable.arrow_upward_alt);


    @Composable
    override fun DropdownMenuItem(onClick: () -> Unit) {
        DropdownMenuItem(
            text = { Text(stringResource(nameRes)) },
            onClick = { onClick() },
            leadingIcon = { Icon(painterResource(iconRes), null) },
        )
    }
}

interface DropdownItem {
    @Composable
    fun DropdownMenuItem(onClick: () -> Unit)
}

/**
 *
 * Query
 *
 */
data class UsageQuery (
    val dataType: DataType,
    val dataDirection: DataDirection = DataDirection.Bidirectional,
    val dataUID: DataUID = allApp
) {
    fun toString(context: Context): String {
        if (dataType == DataType.None) return context.getString(dataType.nameRes)
        val parts = buildList {
            add(context.getString(dataType.nameRes))
            if (dataDirection != DataDirection.Bidirectional)
                add(context.getString(dataDirection.nameRes))
            if (dataUID.uidQuery != null)
                add(dataUID.getName(context))
        }

        val isRtl = context.resources.configuration.layoutDirection == View.LAYOUT_DIRECTION_RTL
        return (if (isRtl) parts.reversed() else parts).joinToString(" · ")
    }
}
