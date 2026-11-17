package com.leekleak.core.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipAnchorPosition
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.leekleak.core.R
import com.leekleak.core.ui.theme.card
import com.leekleak.core.ui.theme.googleSans

enum class MiniCardState {
    POSITIVE,
    NEUTRAL,
    NEGATIVE
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RowScope.MiniCard(
    state: MiniCardState,
    baseColor: Color = colorScheme.surfaceContainer,
    icon: Painter,
    title: String,
    tooltipText: String? = null,
    description: AnnotatedString
) {
    val fontFamily = remember { googleSans(weight = 600f) }
    val color by animateColorAsState(
        when(state) {
            MiniCardState.NEGATIVE -> colorScheme.errorContainer
            MiniCardState.POSITIVE -> colorScheme.primaryContainer
            MiniCardState.NEUTRAL -> baseColor
        }
    )

    val content = @Composable {
        Column(
            modifier = Modifier
                .card()
                .fillMaxSize()
                .background(color)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(icon, null)
                Text(title)
            }
            AnimatedContent(description) {
                Text(
                    text = it,
                    fontFamily = fontFamily,
                    fontSize = 24.sp
                )
            }
        }
    }

    Box(Modifier.weight(1f)) {
        if (tooltipText != null) {
            TooltipBox(
                positionProvider = TooltipDefaults.rememberTooltipPositionProvider(
                    positioning = TooltipAnchorPosition.Above
                ),
                tooltip = { PlainTooltip { Text(tooltipText) } },
                state = rememberTooltipState()
            ) {
                content()
            }
        } else {
            content()
        }
    }
}

@Composable
fun RowScope.TrendCard(
    trend: Int,
    baseColor: Color = colorScheme.surfaceContainer,
) {
    val state = when {
        trend > 50 -> MiniCardState.NEGATIVE
        trend < -25 -> MiniCardState.POSITIVE
        else -> MiniCardState.NEUTRAL
    }
    MiniCard(
        state = state,
        baseColor = baseColor,
        icon = when (state) {
            MiniCardState.NEGATIVE -> painterResource(R.drawable.trending_up)
            MiniCardState.POSITIVE -> painterResource(R.drawable.trending_down)
            else -> painterResource(R.drawable.trending_flat)
        },
        title = stringResource(R.string.trend),
        tooltipText = stringResource(R.string.trend_tooltip),
        description = buildAnnotatedString {
            append(if (trend < 1000) "%+d%%".format(trend) else stringResource(R.string.very_big))
        }
    )
}
