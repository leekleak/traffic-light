package com.leekleak.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.leekleak.core.R

@Composable
fun ShizukuStoppedTag(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .background(colorScheme.secondary, MaterialTheme.shapes.medium)
            .padding(vertical = 4.dp, horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Icon(
            painter = painterResource(R.drawable.warning),
            contentDescription = null,
            tint = colorScheme.onSecondary,
        )
        Text(
            text = stringResource(R.string.shizuku_stopped),
            color = colorScheme.onSecondary,
            style = MaterialTheme.typography.labelLarge,
            maxLines = 1,
        )
    }
}
