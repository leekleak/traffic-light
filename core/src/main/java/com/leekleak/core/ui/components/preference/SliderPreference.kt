package com.leekleak.core.ui.components.preference

import androidx.annotation.DrawableRes
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSliderState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import com.leekleak.core.ui.theme.card
import com.leekleak.core.ui.theme.googleSans
import kotlin.math.roundToInt

@Composable
fun SliderPreference(
    modifier: Modifier = Modifier,
    modifierLabelText: Modifier = Modifier,
    title: String,
    @DrawableRes icon: Int? = null,
    value: Long,
    values: List<Pair<Long, String?>>,
    enabled: Boolean = true,
    onValueChanged: (Long) -> Unit
) {
   SliderComponent(
       modifier = modifier
           .fillMaxWidth()
           .padding(vertical = 4.dp)
           .card()
           .padding(start = 8.dp, end = 16.dp, bottom = 4.dp),
       modifierLabelText = modifierLabelText,
       title = title,
       icon = icon,
       value = value,
       values = values,
       enabled = enabled,
       onValueChanged = onValueChanged
   )
}

@Composable
fun SliderComponent(
    modifier: Modifier,
    modifierLabelText: Modifier = Modifier,
    title: String,
    @DrawableRes icon: Int? = null,
    value: Long,
    values: List<Pair<Long, String?>>,
    enabled: Boolean = true,
    onValueChanged: (Long) -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val fontFamilyBold = remember { googleSans(weight = 800f) }
    val sliderState = rememberSliderState(
        value = values.indexOfFirst { it.first == value }.coerceAtLeast(0).toFloat(),
        steps = (values.size - 2).coerceAtLeast(0),
        trackRange = 0f..((values.size - 1).coerceAtLeast(0).toFloat()),
    )

    Column(modifier = modifier.alpha(if (enabled) 1f else 0.38f)) {
        Row(
            modifier = Modifier.padding(top = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            icon?.let { Icon(
                modifier = Modifier.width(48.dp),
                painter = painterResource(it),
                contentDescription = null,
            ) }
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
            )
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            val interactionSource = remember { MutableInteractionSource() }
            Slider(
                state = sliderState,
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 8.dp),
                onValueChange = {
                    val newIndex = it.roundToInt()
                    if (newIndex != sliderState.value.roundToInt() && newIndex in values.indices) {
                        haptic.performHapticFeedback(HapticFeedbackType.SegmentFrequentTick)
                    }

                    // It seems that if we define onValueChange logic manually,
                    // we also have to update the state manually.
                    // Seems a bit weird to me, so check if this can be removed later.
                    // Last checked compose.material3 version: 1.15.0-alpha28
                    sliderState.value = it
                },
                onValueChangeFinished = {
                    onValueChanged(values[sliderState.value.roundToInt()].first)
                },
                thumb = {
                    SliderDefaults.Thumb(
                        interactionSource = interactionSource,
                        thumbSize = DpSize(4.dp, 28.dp)
                    )
                },
                interactionSource = interactionSource,
                enabled = enabled,
            )

            val valueLabel = remember(sliderState.value.roundToInt(), values) {
                val pair = values.getOrNull(sliderState.value.roundToInt())
                pair?.second ?: pair?.first?.toString() ?: ""
            }
            AnimatedContent(
                targetState = valueLabel,
                transitionSpec = {
                    (slideInVertically{ -it / 2 } + fadeIn()).togetherWith(
                        (slideOutVertically { it / 2 }) + fadeOut()
                    )
                }
            ) {
                Text(
                    modifier = modifierLabelText.padding(start = 16.dp),
                    text = it,
                    fontFamily = fontFamilyBold,
                    textAlign = TextAlign.Center,
                    overflow = TextOverflow.Visible,
                    softWrap = false,
                )
            }
        }
    }
}
