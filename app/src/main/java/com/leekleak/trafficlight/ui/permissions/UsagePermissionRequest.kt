package com.leekleak.trafficlight.ui.permissions

import android.app.Activity
import androidx.activity.compose.LocalActivity
import androidx.annotation.DrawableRes
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialShapes.Companion.Sunny
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.leekleak.core.R
import com.leekleak.core.ui.components.BackAction
import com.leekleak.core.ui.components.HazeScaffold
import com.leekleak.core.ui.navigation.Navigator
import com.leekleak.core.ui.theme.card
import com.leekleak.trafficlight.ui.navigation.OverviewKey

@Composable
fun UsagePermissionRequest(
    navigator: Navigator,
    uiState: UsagePermissionRequestUiState,
    askUsagePermission: (Activity?) -> Unit,
    openUsagePermissionHelp: (Activity?) -> Unit,
    askBackgroundPermission: (Activity?) -> Unit,
    openBatteryPermissionHelp: (Activity?) -> Unit,
) {
    val activity = LocalActivity.current

    val firstUngranted = when {
        !uiState.usagePermission -> 0
        !uiState.backgroundPermission -> 1
        else -> null
    }
    var selected by remember { mutableStateOf<Int?>(null) }

    LaunchedEffect(uiState.usagePermission, uiState.backgroundPermission) {
        selected = null
        if (uiState.usagePermission && uiState.backgroundPermission) navigator.setTo(OverviewKey)
    }

    val expandedCard = selected ?: firstUngranted

    HazeScaffold(
        backAction = BackAction.None,
        scrollState = null,
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(
                    start = paddingValues.calculateStartPadding(LocalLayoutDirection.current),
                    end = paddingValues.calculateEndPadding(LocalLayoutDirection.current)
                ),
            verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            val topPadding = paddingValues.calculateTopPadding()
            Spacer(Modifier.height(topPadding))
            PermissionRequestCard(
                modifier = Modifier.fillMaxWidth(),
                expanded = expandedCard == 0,
                icon = R.drawable.query_stats,
                title = stringResource(R.string.usage_statistics),
                description = stringResource(R.string.usage_statistics_description),
                onHelp = { openUsagePermissionHelp(activity) },
                onGrant = { askUsagePermission(activity) },
                onClick = { selected = 0 },
            )
            PermissionRequestCard(
                modifier = Modifier.fillMaxWidth(),
                expanded = expandedCard == 1,
                icon = R.drawable.battery,
                title = stringResource(R.string.battery_optimization),
                description = stringResource(R.string.battery_optimization_description),
                onHelp = { openBatteryPermissionHelp(activity) },
                onGrant = { askBackgroundPermission(activity) },
                onClick = { selected = 1 },
            )
            Spacer(Modifier.height(topPadding)) // Aligned vertically, so this fixes symmetry
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun PermissionRequestCard(
    modifier: Modifier,
    expanded: Boolean,
    @DrawableRes icon: Int,
    title: String,
    description: String,
    onHelp: () -> Unit,
    onGrant: () -> Unit,
    onClick: () -> Unit,
) {
    SharedTransitionScope { sharedModifier ->
        val interactionSource = remember { MutableInteractionSource() }
        AnimatedContent(
            targetState = expanded,
            transitionSpec = {
                ContentTransform(fadeIn(), fadeOut(), sizeTransform = SizeTransform(clip = false))
            },
            modifier = sharedModifier
                .then(modifier)
                .fillMaxWidth()
                .padding(vertical = 2.dp)
                .card()
                .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
                .padding(16.dp),
            label = "permission_card",
        ) { isExpanded ->
            if (isExpanded) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier
                            .sharedBounds(
                                rememberSharedContentState(key = "$title-box"),
                                this@AnimatedContent,
                                resizeMode = SharedTransitionScope.ResizeMode.scaleToBounds(),
                            )
                            .size(124.dp)
                            .clip(Sunny.toShape())
                            .background(MaterialTheme.colorScheme.primary),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            modifier = Modifier.size(62.dp),
                            painter = painterResource(icon),
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
                            text = title,
                            modifier = Modifier.sharedBounds(
                                rememberSharedContentState(key = "$title-text"),
                                this@AnimatedContent
                            ),
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = description,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            colors = ButtonDefaults.filledTonalButtonColors(),
                            onClick = onHelp,
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
                            onClick = onGrant,
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
            } else {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .sharedBounds(
                                rememberSharedContentState(key = "$title-box"),
                                this@AnimatedContent,
                                resizeMode = SharedTransitionScope.ResizeMode.scaleToBounds(),
                            )
                            .size(40.dp)
                            .clip(Sunny.toShape())
                            .background(MaterialTheme.colorScheme.primary),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            modifier = Modifier.size(20.dp),
                            painter = painterResource(icon),
                            tint = MaterialTheme.colorScheme.onPrimary,
                            contentDescription = null,
                        )
                    }
                    Text(
                        text = title,
                        modifier = Modifier.sharedBounds(
                            rememberSharedContentState(key = "$title-text"),
                            this@AnimatedContent
                        ),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
