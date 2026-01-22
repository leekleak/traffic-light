package com.leekleak.trafficlight.ui.overview

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.InputTransformation
import androidx.compose.foundation.text.input.OutputTransformation
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.maxLength
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.toPath
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.copy
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.leekleak.trafficlight.R
import com.leekleak.trafficlight.charts.GraphTheme.wifiShape
import com.leekleak.trafficlight.util.categoryTitle
import com.leekleak.trafficlight.util.px
import kotlin.math.E
import kotlin.math.pow

@Composable
fun PlanConfig(
    paddingValues: PaddingValues,
    subscriberId: String
) {
    val viewModel: PlanConfigVM = viewModel()

    LazyColumn(
        modifier = Modifier
            .background(MaterialTheme.colorScheme.surface)
            .fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = paddingValues
    ) {
        categoryTitle(R.string.configure_plan)
        item {
            PlanSizeConfig()
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun PlanSizeConfig() {
    Box(
        modifier = Modifier.height(128.dp * 2.5f).fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {

        val shape = wifiShape().toPath()
        val shapeSizeBase = 128.dp.px
        val shapeColor = MaterialTheme.colorScheme.primaryContainer
        val size = remember { Animatable(0f) }

        val shapeTransformed = remember(size.value) {
            val sizePx = shapeSizeBase * (1 + size.value)
            val matrix = Matrix().apply {
                scale(sizePx, sizePx)
            }
            shape.copy().apply { transform(matrix) }
        }

        val fieldState = rememberTextFieldState("10")
        LaunchedEffect(fieldState.text) {
            val number = fieldState.text.toString().toFloatOrNull() ?: 0f
            size.animateTo(
                targetValue = (1.5 * (1-E.pow(-number * 0.1))).toFloat(),
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioLowBouncy,
                    stiffness = Spring.StiffnessMedium
                )
            )
        }

        Box(
            modifier = Modifier
                .size(128.dp * (1 + size.value))
                .drawWithCache {
                    onDrawBehind {
                        rotate(size.value * 90f) {
                            drawPath(
                                path = shapeTransformed,
                                color = shapeColor,
                            )
                        }
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            Row(
                modifier = Modifier.size(128.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                BasicTextField(
                    state = fieldState,
                    modifier = Modifier.weight(1.1f),
                    inputTransformation = InputTransformation.maxLength(3),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number
                    ),
                    textStyle = TextStyle(
                        fontFamily = bigFont(),
                        fontSize = 40.sp,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        textAlign = TextAlign.End,
                    ),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.surface),
                    lineLimits = TextFieldLineLimits.SingleLine,
                )
                Text(
                    modifier = Modifier.weight(1f),
                    fontFamily = bigFont(),
                    fontSize = 30.sp,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    text = "GB"
                )
            }
        }
    }
}
