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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.leekleak.core.R
import com.leekleak.core.ui.components.BackAction
import com.leekleak.core.ui.components.HazeScaffold
import com.leekleak.core.ui.navigation.LocalNavigationBarPadding
import com.leekleak.core.ui.navigation.Navigator
import com.leekleak.core.ui.theme.card
import com.leekleak.core.util.CategoryTitleSmallText
import com.leekleak.trafficlight.BuildConfig
import com.leekleak.trafficlight.ui.navigation.IperfScreenKey
import com.leekleak.trafficlight.ui.navigation.NotificationSettingsKey

private val ToolCardMinWidth = 152.dp
private val ToolGridSpacing = 8.dp
private val ToolCardHeight = 104.dp

private class Tool(
    val title: String,
    val icon: Int,
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
                    icon = R.drawable.speed_notification,
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
                        icon = R.drawable.speed,
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
                            icon = painterResource(tool.icon),
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
    icon: Painter,
    onClick: () -> Unit,
) {
    val haptic = LocalHapticFeedback.current

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
    ) {
        Icon(
            modifier = Modifier.size(28.dp),
            painter = icon,
            contentDescription = null,
        )
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
