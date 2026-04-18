package com.leekleak.trafficlight.ui.settings

import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialShapes.Companion.Sunny
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.leekleak.trafficlight.R
import com.leekleak.trafficlight.model.PermissionManager
import com.leekleak.trafficlight.ui.navigation.Navigator
import com.leekleak.trafficlight.ui.navigation.Overview
import com.leekleak.trafficlight.ui.navigation.Settings
import com.leekleak.trafficlight.util.PageTitle
import org.koin.compose.koinInject

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun UsagePermissionRequest(paddingValues: PaddingValues) {
    val activity = LocalActivity.current
    val permissionManager: PermissionManager = koinInject()
    val navigator: Navigator = koinInject()
    val usagePermission by permissionManager.usagePermissionFlow.collectAsState()

    LaunchedEffect(usagePermission) {
        if (usagePermission) navigator.setTo(Overview)
    }

    PageTitle(false, null, "") {
        IconButton(
            modifier = Modifier.align(Alignment.TopEnd),
            onClick = { navigator.goTo(Settings) }
        ) {
            Icon(
                painterResource(R.drawable.settings),
                contentDescription = stringResource(R.string.settings)
            )
        }
    }
    Box(Modifier.fillMaxSize().padding(paddingValues)) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(124.dp)
                    .clip(Sunny.toShape())
                    .background(MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    modifier = Modifier.size(62.dp),
                    painter = painterResource(R.drawable.query_stats),
                    tint = MaterialTheme.colorScheme.onPrimary,
                    contentDescription = null,
                )
            }
            Column(
                modifier = Modifier.padding(top = 26.dp, bottom = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = stringResource(R.string.usage_statistics),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = stringResource(R.string.usage_statistics_description),
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    colors = ButtonDefaults.filledTonalButtonColors(),
                    onClick = { permissionManager.openUsagePermissionHelp(activity) },
                ) {
                    Row(
                        Modifier.padding(6.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.help),
                            contentDescription = null,
                        )
                        Text(stringResource(R.string.help))
                    }
                }
                Button(
                    colors = ButtonDefaults.buttonColors(),
                    onClick = { permissionManager.askUsagePermission(activity) },
                ) {
                    Row(
                        Modifier.padding(6.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.grant),
                            contentDescription = null,
                        )
                        Text(stringResource(R.string.grant))
                    }
                }
            }
        }
    }
}