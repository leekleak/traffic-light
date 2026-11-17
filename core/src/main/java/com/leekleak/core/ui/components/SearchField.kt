package com.leekleak.core.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.MaterialTheme.shapes
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.leekleak.core.R
import com.leekleak.core.ui.theme.card

@Composable
fun SearchField(
    modifier: Modifier = Modifier,
    textFieldState: TextFieldState,
    placeholder: String,
    isError: String? = null,
    icon: Painter? = null,
) {
    Column {
        TextField(
            modifier = modifier.fillMaxWidth(),
            state = textFieldState,
            textStyle = MaterialTheme.typography.titleMedium.copy(color = colorScheme.onSurface),
            placeholder = { Text(placeholder) },
            leadingIcon =
                icon?.let {
                    {
                        Icon(
                            modifier = Modifier.padding(start = 6.dp),
                            painter = it,
                            contentDescription = null
                        )
                    }
                },
            isError = isError != null,
            shape = shapes.extraLarge,
            colors = TextFieldDefaults.colors().copy(
                focusedContainerColor = colorScheme.surfaceContainerHigh,
                errorContainerColor = colorScheme.surfaceContainerHigh,
                unfocusedContainerColor = colorScheme.surfaceContainerHigh,
                disabledContainerColor = colorScheme.surfaceContainerHigh,
                unfocusedIndicatorColor = Color.Transparent,
                errorIndicatorColor = Color.Transparent,
                focusedIndicatorColor = Color.Transparent,
                disabledIndicatorColor = Color.Transparent
            )
        )
        SlideAnimatedVisibility(isError != null) {
            Row (
                modifier = Modifier
                    .padding(top = 4.dp)
                    .fillMaxWidth()
                    .card(colorScheme.errorContainer)
                    .padding(vertical = 8.dp, horizontal = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(painterResource(R.drawable.warning), null, tint = colorScheme.error)
                Text(text = isError ?: "", color = colorScheme.error)
            }
        }
    }
}
