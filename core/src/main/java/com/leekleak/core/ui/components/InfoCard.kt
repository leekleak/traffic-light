package com.leekleak.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.leekleak.core.R
import com.leekleak.core.ui.theme.card

@Composable
fun PermissionCard(
    title: String,
    description: String,
    icon: Painter,
    onClick: () -> Unit,
) {
    InfoCard(
        title = title,
        description = description,
        icon = icon,
        buttonIcon = painterResource(R.drawable.grant),
        buttonDescription = stringResource(R.string.grant),
        onClick = onClick,
    )
}

@Composable
fun InfoCard(
    title: String,
    description: String,
    icon: Painter,
    backgroundColor: Color = colorScheme.surfaceContainer,
    buttonIcon: Painter? = null,
    buttonDescription: String? = null,
    onClick: (() -> Unit)? = null,
) {
    Row (
        modifier = Modifier
            .height(IntrinsicSize.Max)
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .card()
                .background(backgroundColor)
                .padding(16.dp),
            horizontalAlignment = Alignment.End
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(icon, null)
                Text(modifier = Modifier.fillMaxWidth(), fontWeight = FontWeight.Bold, text = title)
            }
            Text(modifier = Modifier.fillMaxWidth(), text = description)
        }
        if (onClick != null && buttonIcon != null) {
            FilledIconButton(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(56.dp),
                shape = MaterialTheme.shapes.large,
                onClick = onClick,
            ) {
                Icon(
                    painter = buttonIcon,
                    contentDescription = buttonDescription,
                )
            }
        }
    }
}
