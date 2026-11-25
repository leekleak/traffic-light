package com.leekleak.core.ui.components.preference

import androidx.annotation.DrawableRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.leekleak.core.ui.theme.card

@Composable
fun IconPreference(
    title: String,
    @DrawableRes icon: Int,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .width(52.dp)
            .fillMaxHeight()
            .padding(vertical = 4.dp)
            .card()
            .clickable(enabled = enabled) { onClick.invoke() }
            .alpha(if (enabled) 1f else 0.38f),
    ) {
        Icon(
            modifier = Modifier.align(Alignment.Center),
            painter = painterResource(icon),
            contentDescription = title
        )
    }
}
