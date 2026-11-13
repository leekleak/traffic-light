package com.leekleak.core.ui.components

import android.annotation.SuppressLint
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideIn
import androidx.compose.animation.slideOut
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.leekleak.core.ui.navigation.Navigator
import com.leekleak.core.util.PageTitle
import com.leekleak.core.util.TOP_BAR_HEIGHT
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.rememberHazeState

/**
 * Haze scaffold provides a top bar with a title and a back button.
 *
 * The default configuration automatically aligns items in a scrollable column meaning that for most
 * use cases it should be enough. In that case just provide the items, and they'll be arranged automatically.
 *
 * In case the content contains a LazyColumn though, it will crash due to the fact that the scrollable
 * internal column measures infinite height. In that case make sure to override scrollState to null.
 *
 * @param title The title of the page.
 * @param modifier The modifier to be applied to the scaffold.
 * @param scrollState The scroll state of the scaffold. IMPORTANT: Should be null if content contains LazyColumn
 * @param hazeState The haze state of the scaffold.
 * @param backAction Object to be defined if we want a "back" button.
 * @param verticalArrangement The vertical arrangement of the content.
 * @param actions The actions to be displayed in the top bar.
 * @param content The content of the scaffold.
 */

@Composable
@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
fun HazeScaffold(
    modifier: Modifier = Modifier,
    title: String? = null,
    scrollState: ScrollState? = rememberScrollState(),
    hazeState: HazeState? = rememberHazeState(),
    showTitle: Boolean = true,
    applyPadding: Boolean = scrollState != null,
    extraPadding: PaddingValues = PaddingValues(),
    verticalArrangement: Arrangement.Vertical = Arrangement.spacedBy(8.dp),
    horizontalAlignment: Alignment.Horizontal = Alignment.Start,
    backAction: BackAction,
    actions: @Composable RowScope.() -> Unit = {},
    content: @Composable ColumnScope.(PaddingValues) -> Unit,
) {
    val layoutDirection = LocalLayoutDirection.current
    val extraStart = extraPadding.calculateStartPadding(layoutDirection)
    val extraEnd = extraPadding.calculateEndPadding(layoutDirection)
    val contentSidePadding = 16.dp
    val outerStart = extraStart + if (applyPadding) contentSidePadding else 0.dp
    val outerEnd = extraEnd + if (applyPadding) contentSidePadding else 0.dp
    val contentStart = if (applyPadding) 0.dp else contentSidePadding
    val contentEnd = if (applyPadding) 0.dp else contentSidePadding
    val paddingTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + TOP_BAR_HEIGHT + 6.dp + extraPadding.calculateTopPadding()
    val paddingBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + 8.dp + extraPadding.calculateBottomPadding()
    val paddingValues = if (applyPadding) {
        PaddingValues()
    } else {
        PaddingValues(start = contentStart, top = paddingTop, end = contentEnd, bottom = paddingBottom)
    }
    Scaffold(
        contentWindowInsets = WindowInsets()
    ) {
        Box(modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .background(colorScheme.surface)
                    .fillMaxSize()
                    .then(
                        hazeState?.let { Modifier.hazeSource(it) } ?: Modifier
                    )
                    .padding(start = outerStart, end = outerEnd)
                    .then(if (scrollState != null) Modifier.verticalScroll(scrollState) else Modifier),
            ) {
                if (applyPadding) Spacer(Modifier.height((paddingTop).coerceAtLeast(0.dp)))
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = verticalArrangement,
                    horizontalAlignment = horizontalAlignment,
                ) {
                    content(paddingValues)
                }
                if (applyPadding) Spacer(Modifier.height((paddingBottom).coerceAtLeast(0.dp)))
            }
            AnimatedVisibility(
                visible = showTitle,
                enter = slideIn { IntOffset(x = 0, y = -it.height) },
                exit = slideOut { IntOffset(x = 0, y = -it.height) },
            ) {
                PageTitle(backAction, hazeState, title, actions)
            }
        }
    }
}

sealed interface BackAction {
    data object None : BackAction
    data class Normal(val navigator: Navigator) : BackAction
}

