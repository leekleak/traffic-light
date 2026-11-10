package com.leekleak.trafficlight.ui.tools

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation3.runtime.rememberNavBackStack
import com.leekleak.core.R
import com.leekleak.core.ui.components.BackAction
import com.leekleak.core.ui.components.HazeScaffold
import com.leekleak.core.ui.navigation.LocalNavigationBarPadding
import com.leekleak.core.ui.navigation.Navigator
import com.leekleak.core.ui.theme.AppTheme
import com.leekleak.core.ui.theme.card
import com.leekleak.core.ui.theme.googleSans
import com.leekleak.core.util.CategoryTitleSmallText
import com.leekleak.trafficlight.BuildConfig
import com.leekleak.trafficlight.ui.navigation.IperfScreenKey
import com.leekleak.trafficlight.ui.navigation.NotificationSettingsKey
import com.leekleak.trafficlight.ui.navigation.ToolsKey
import com.leekleak.trafficlight.ui.settings.SpeedIconPreview

private val ToolCardMinWidth = 152.dp
private val ToolGridSpacing = 8.dp
private val ToolCardHeight = 104.dp

private class Tool(
    val title: String,
    val icon: @Composable () -> Unit,
    val onClick: () -> Unit,
)

@Composable
fun Tools(navigator: Navigator) {
    HazeScaffold(
        title = stringResource(R.string.tools),
        backAction = BackAction.None,
        extraPadding = LocalNavigationBarPadding.current,
    ) {
        CategoryTitleSmallText(stringResource(R.string.speed_tracking))
        ToolGrid(
            tools = listOf(
                Tool(
                    title = stringResource(R.string.notification),
                    icon = { SpeedIconPreview(modifier = Modifier.size(32.dp)) },
                    onClick = { navigator.goTo(NotificationSettingsKey) },
                ),
                Tool(
                    title = stringResource(R.string.overlay),
                    icon = { Icon(
                        modifier = Modifier.size(32.dp),
                        painter = painterResource(R.drawable.picture_in_picture),
                        contentDescription = null
                    ) },
                    onClick = { navigator.goTo(NotificationSettingsKey) },
                )
            )
        )
        if (BuildConfig.IPERF) {
            CategoryTitleSmallText(stringResource(R.string.reliability))
            ToolGrid(
                tools = listOf(
                    Tool(
                        title = stringResource(R.string.iperf3),
                        icon = {
                            Icon(
                                modifier = Modifier.size(32.dp),
                                painter = painterResource(R.drawable.speed),
                                contentDescription = null,
                            )
                        },
                        onClick = { navigator.goTo(IperfScreenKey) },
                    )
                )
            )
        }
    }
}

@Composable
private fun ToolGrid(tools: List<Tool>) {
    BoxWithConstraints {
        val columns = ((maxWidth + ToolGridSpacing) / (ToolCardMinWidth + ToolGridSpacing))
            .toInt()
            .coerceAtLeast(1)

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(ToolGridSpacing),
        ) {
            tools.chunked(columns).forEach { rowTools ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(ToolGridSpacing),
                ) {
                    rowTools.forEach { tool ->
                        ToolCard(
                            modifier = Modifier.weight(1f),
                            title = tool.title,
                            icon = tool.icon,
                            onClick = tool.onClick,
                        )
                    }
                    repeat(columns - rowTools.size) {
                        Spacer(Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
private fun ToolCard(
    modifier: Modifier = Modifier,
    title: String,
    icon: @Composable () -> Unit,
    onClick: () -> Unit,
) {
    val haptic = LocalHapticFeedback.current
    val font = remember { googleSans(weight = 600f) }

    Column(
        modifier = modifier
            .height(ToolCardHeight)
            .card()
            .clickable {
                haptic.performHapticFeedback(HapticFeedbackType.ContextClick)
                onClick()
            }
            .padding(16.dp),
        verticalArrangement = Arrangement.SpaceBetween,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        icon()
        Text(
            text = title,
            maxLines = 2,
            fontSize = 18.sp,
            fontFamily = font,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@PreviewLightDark
@Composable
private fun ToolsPreview() {
    val backStack = rememberNavBackStack(ToolsKey)
    val navigator = remember(backStack) { Navigator(backStack) }
    AppTheme {
        Tools(navigator)
    }
}
