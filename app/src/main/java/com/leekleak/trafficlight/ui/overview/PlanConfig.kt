package com.leekleak.trafficlight.ui.overview

import android.annotation.SuppressLint
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.InputTransformation
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.maxLength
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonGroup
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.toPath
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.copy
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import com.leekleak.trafficlight.R
import com.leekleak.trafficlight.charts.GraphTheme.wifiShape
import com.leekleak.trafficlight.database.DataPlan
import com.leekleak.trafficlight.database.DataPlanDao
import com.leekleak.trafficlight.ui.theme.backgrounds
import com.leekleak.trafficlight.ui.theme.card
import com.leekleak.trafficlight.util.DataSize
import com.leekleak.trafficlight.util.DataSizeUnit
import com.leekleak.trafficlight.util.categoryTitle
import com.leekleak.trafficlight.util.categoryTitleSmall
import com.leekleak.trafficlight.util.fromTimestamp
import com.leekleak.trafficlight.util.px
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import java.text.DecimalFormat
import kotlin.math.E
import kotlin.math.pow

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter", "LocalContextGetResourceValueCall")
@Composable
fun PlanConfig(
    paddingValues: PaddingValues,
    subscriberId: String,
    backStack: NavBackStack<NavKey>
) {
    val viewModel: OverviewVM = viewModel()
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    val dataPlanDao: DataPlanDao = koinInject()
    val currentPlan = remember { viewModel.getDataPlan(subscriberId) }.collectAsState(DataPlan(subscriberId))
    var newPlan by remember { mutableStateOf(DataPlan(subscriberId, uiBackground = 3)) }
    LaunchedEffect(currentPlan.value) {
        newPlan = currentPlan.value
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(16.dp),
                contentAlignment = Alignment.BottomEnd
            ) {
                ExtendedFloatingActionButton (
                    onClick = {
                        scope.launch(Dispatchers.IO) {
                            dataPlanDao.add(newPlan)
                            backStack.removeAt(backStack.lastIndex)
                        }
                    }
                ) {
                    Row (horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(
                            painter = painterResource(R.drawable.save),
                            contentDescription = null
                        )
                        Text(
                            text = "Save"
                        )
                    }
                }
            }
        }
    ) {
        LazyColumn(
            modifier = Modifier
                .background(MaterialTheme.colorScheme.surface)
                .fillMaxSize(),
            contentPadding = paddingValues
        ) {
            categoryTitle(R.string.configure_plan)
            item {
                val size by remember { derivedStateOf { DataSize(currentPlan.value.dataMax.toDouble()).value.toFloat() } }
                PlanSizeConfig (
                    size = size
                ) {
                    val data = DataSize(it.toDouble(), unit = DataSizeUnit.GB)
                    newPlan = newPlan.copy(dataMax = data.getBitValue())
                }
            }
            categoryTitleSmall(R.string.type)
            item {
                Column(
                    modifier = Modifier
                        .card()
                        .padding(8.dp)
                ) {
                    ButtonGroup(
                        modifier = Modifier.fillMaxWidth(),
                        overflowIndicator = {}
                    ) {
                        toggleableItem(
                            checked = newPlan.recurring,
                            label = context.getString(R.string.monthly),
                            icon = {
                                Icon(
                                    painter = painterResource(R.drawable.calendar),
                                    contentDescription = null
                                )
                            },
                            onCheckedChange = {},
                            weight = 1f,
                        )
                    }

                    var selectDate by remember { mutableStateOf(false) }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text ("Cycle start day: ")
                        Button(onClick = { selectDate = true }) {
                            val date = fromTimestamp(newPlan.startDate)
                            Text(date.dayOfMonth.toString())
                        }
                    }

                    val datePickerState = rememberDatePickerState()
                    if (selectDate) {
                        DatePickerDialog(
                            onDismissRequest = { selectDate = false },
                            confirmButton = {
                                Button(onClick = {
                                    newPlan = newPlan.copy(startDate = datePickerState.selectedDateMillis!!)
                                    selectDate = false
                                }) {
                                    Row (horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Icon(
                                            painter = painterResource(R.drawable.save),
                                            contentDescription = null
                                        )
                                        Text(
                                            text = "Save"
                                        )
                                    }
                                }
                            }
                        ) {
                            DatePicker(datePickerState)
                        }
                    }
                }
            }
            categoryTitleSmall(R.string.background)
            item {
                LazyRow(
                    modifier = Modifier.card(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(16.dp)
                ) {
                    items(backgrounds.size) { i ->
                        BackgroundSelector(i, newPlan) {
                            newPlan = newPlan.copy(uiBackground = i)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun BackgroundSelector(i: Int, newPlan: DataPlan, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .width(192.dp)
            .height(128.dp)
            .clip(MaterialTheme.shapes.medium)
            .background(MaterialTheme.colorScheme.background)
            .border(1.dp, MaterialTheme.colorScheme.primary, MaterialTheme.shapes.medium)
            .clickable { onClick() },
    ) {
        backgrounds[i]?.let {
            Image(
                modifier = Modifier
                    .matchParentSize()
                    .graphicsLayer(scaleX = 1.2f, scaleY = 1.2f),
                painter = painterResource(it),
                contentDescription = null,
                contentScale = ContentScale.FillWidth,
                colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.primaryContainer)
            )
        }
        AnimatedVisibility(
            modifier = Modifier
                .padding(8.dp)
                .align(Alignment.TopEnd),
            visible = newPlan.uiBackground == i,
            enter = fadeIn() + scaleIn(),
            exit = fadeOut() + scaleOut()
        ) {
            Icon (
                painter = painterResource(R.drawable.checkmark),
                contentDescription = stringResource(R.string.selected),
            )
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun PlanSizeConfig(size: Float, onSizeUpdate: (Float) -> Unit) {
    Box(
        modifier = Modifier
            .height(128.dp * 2.5f)
            .fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        val shape = wifiShape().toPath()
        val shapeSizeBase = 128.dp.px
        val shapeColor = MaterialTheme.colorScheme.primaryContainer
        val scale = remember { Animatable(0f) }

        val shapeTransformed = remember(scale.value) {
            val sizePx = shapeSizeBase * (1 + scale.value)
            val matrix = Matrix().apply {
                scale(sizePx, sizePx)
            }
            shape.copy().apply { transform(matrix) }
        }

        val formatter = remember { DecimalFormat("0.#") }
        val fieldState = remember(size) {
            TextFieldState(formatter.format(size))
        }

        LaunchedEffect(fieldState.text) {
            val number = fieldState.text.toString().toFloatOrNull() ?: 0f
            onSizeUpdate(number)
            scale.animateTo(
                targetValue = (1.5 * (1-E.pow(-number * 0.1))).toFloat(),
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioLowBouncy,
                    stiffness = Spring.StiffnessMedium
                )
            )
        }

        Box(
            modifier = Modifier
                .size(128.dp * (1 + scale.value))
                .drawWithCache {
                    onDrawBehind {
                        rotate(scale.value * 60f) {
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
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp, Alignment.CenterHorizontally)
            ) {
                var intrinsics by remember { mutableIntStateOf(0) }
                BasicTextField(
                    state = fieldState,
                    modifier = Modifier.width (with(LocalDensity.current) { intrinsics.toDp() }),
                    inputTransformation = InputTransformation.maxLength(3),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number
                    ),
                    textStyle = TextStyle(
                        fontFamily = bigFont(),
                        fontSize = 40.sp * (1 + scale.value/2),
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        textAlign = TextAlign.End,
                    ),
                    onTextLayout = { out ->
                        val right = out()?.getLineRight(0)?.toInt()
                        val left = out()?.getLineLeft(0)?.toInt()
                        intrinsics = if (right != null && left != null) { right - left } else 0
                    },
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.surface),
                    lineLimits = TextFieldLineLimits.SingleLine,
                )
                Text(
                    fontFamily = bigFont(),
                    fontSize = 30.sp * (1 + scale.value/2),
                    maxLines = 1,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    text = "GB"
                )
            }
        }
    }
}
