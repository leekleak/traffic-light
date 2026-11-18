package com.leekleak.core.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip

@Composable
fun SlideAnimatedVisibility(
    visible: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable AnimatedVisibilityScope.() -> Unit
) {
    val visibleState = remember { MutableTransitionState(visible) }.apply { targetState = visible }

    AnimatedVisibility(
        visibleState = visibleState,
        modifier = modifier.clip(MaterialTheme.shapes.medium),
        enter = fadeIn(tween()) + slideInVertically() + expandVertically(),
        exit = fadeOut(tween()) + slideOutVertically() + shrinkVertically(),
        content = content
    )
}
