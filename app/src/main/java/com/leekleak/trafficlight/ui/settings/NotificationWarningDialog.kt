package com.leekleak.trafficlight.ui.settings

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.leekleak.core.R
import com.leekleak.core.ui.components.FancyDialog

@Composable
fun NotificationWarningDialog(
    onDismiss: () -> Unit,
) {
    FancyDialog (
        onDismissRequest = onDismiss,
        title = stringResource(R.string.notification_aggregation_warning_title),
        icon = R.drawable.warning,
        content = { Text(stringResource(R.string.notification_aggregation_warning_text)) },
    )
}
