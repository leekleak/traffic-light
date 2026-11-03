package com.leekleak.core.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.leekleak.core.R
import com.leekleak.core.ui.theme.card
import com.leekleak.core.ui.theme.googleSans
import com.leekleak.core.util.clearFocusOnTap

@Composable
fun FancyDialog(
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    title: String,
    @DrawableRes icon: Int,
    verticalArrangement: Arrangement.Vertical = Arrangement.spacedBy(16.dp),
    actionButton: @Composable (RowScope.() -> Unit) = {},
    content: @Composable (ColumnScope.() -> Unit),
) {
    Dialog(onDismissRequest = onDismissRequest) {
        Column(
            modifier = modifier
                .fillMaxWidth()
                .card(colorScheme.surfaceContainerLow)
                .padding(16.dp)
                .clearFocusOnTap(),
            verticalArrangement = verticalArrangement
        ) {
            val font = remember { googleSans(weight = 600f) }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    modifier = Modifier.size(32.dp),
                    painter = painterResource(icon),
                    contentDescription = null,
                    tint = colorScheme.onSurface
                )
                Text(
                    text = title,
                    fontFamily = font,
                    fontSize = 20.sp,
                    color = colorScheme.onSurface
                )
            }
            content()
            Row(
                modifier = Modifier.align(Alignment.End),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TextButton(onClick = onDismissRequest) {
                    Text(stringResource(R.string.close))
                }
                actionButton()
            }
        }
    }
}
