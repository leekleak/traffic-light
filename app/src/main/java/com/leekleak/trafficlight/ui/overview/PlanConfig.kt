package com.leekleak.trafficlight.ui.overview

import android.annotation.SuppressLint
import android.content.pm.ApplicationInfo
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.InputTransformation
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.maxLength
import androidx.compose.material3.ButtonGroup
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SearchBar
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipAnchorPosition
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberSearchBarState
import androidx.compose.material3.rememberTooltipState
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
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.copy
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import coil3.compose.rememberAsyncImagePainter
import com.leekleak.trafficlight.R
import com.leekleak.trafficlight.charts.GraphTheme.wifiShape
import com.leekleak.trafficlight.database.DataPlan
import com.leekleak.trafficlight.database.DataPlanDao
import com.leekleak.trafficlight.database.TimeInterval
import com.leekleak.trafficlight.model.AppDatabase
import com.leekleak.trafficlight.model.AppIcon
import com.leekleak.trafficlight.ui.theme.backgrounds
import com.leekleak.trafficlight.ui.theme.card
import com.leekleak.trafficlight.util.DataSize
import com.leekleak.trafficlight.util.DataSizeUnit
import com.leekleak.trafficlight.util.categoryTitle
import com.leekleak.trafficlight.util.categoryTitleSmall
import com.leekleak.trafficlight.util.fromTimestamp
import com.leekleak.trafficlight.util.px
import com.leekleak.trafficlight.util.toTimestamp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import timber.log.Timber
import java.text.DecimalFormat
import java.time.LocalDate
import java.time.LocalDateTime
import kotlin.math.E
import kotlin.math.pow
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalMaterial3Api::class)
@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter", "LocalContextGetResourceValueCall")
@Composable
fun PlanConfig(
    subscriberId: String,
    backStack: NavBackStack<NavKey>
) {
    val viewModel: OverviewVM = viewModel()
    val scope = rememberCoroutineScope()

    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current

    val dataPlanDao: DataPlanDao = koinInject()
    val currentPlan = remember { viewModel.getDataPlan(subscriberId) }.collectAsState(DataPlan(subscriberId))
    var newPlan by remember { mutableStateOf(DataPlan(subscriberId, uiBackground = 3)) }
    LaunchedEffect(currentPlan.value) {
        newPlan = currentPlan.value
    }

    Scaffold(
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
                            haptic.performHapticFeedback(HapticFeedbackType.ToggleOn)
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
                            text = stringResource(R.string.save)
                        )
                    }
                }
            }
        }
    ) { paddintValues ->
        LazyColumn(
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .fillMaxSize(),
            contentPadding = paddintValues
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
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    val interval by remember { derivedStateOf { newPlan.interval } }
                    ButtonGroup(
                        modifier = Modifier.fillMaxWidth(),
                        expandedRatio = 0.05f,
                        overflowIndicator = {}
                    ) {
                        toggleableItem(
                            checked = interval == TimeInterval.MONTH,
                            label = context.getString(R.string.monthly),
                            icon = {
                                Icon(
                                    painter = painterResource(R.drawable.calendar),
                                    contentDescription = null
                                )
                            },
                            onCheckedChange = { newPlan = newPlan.copy(interval = TimeInterval.MONTH) },
                            weight = 1f,
                        )
                        toggleableItem(
                            checked = interval == TimeInterval.DAY,
                            label = context.getString(R.string.daily),
                            icon = {
                                Icon(
                                    painter = painterResource(R.drawable.daily),
                                    contentDescription = null
                                )
                            },
                            onCheckedChange = { newPlan = newPlan.copy(interval = TimeInterval.DAY) },
                            weight = 1f,
                        )
                    }

                    AnimatedVisibility(interval == TimeInterval.MONTH) {
                        Column {
                            var selectedDate by remember(newPlan) {
                                mutableIntStateOf(
                                    fromTimestamp(
                                        newPlan.startDate
                                    ).dayOfMonth
                                )
                            }
                            Text(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 16.dp),
                                text = stringResource(R.string.resets_on, selectedDate),
                                fontSize = 18.sp,
                                fontFamily = robotoFlex(0f, 150f, 1000f),
                                textAlign = TextAlign.Center,
                            )
                            Slider(
                                value = selectedDate.toFloat(),
                                onValueChange = {
                                    val newDate = LocalDate.now().withDayOfMonth(it.roundToInt()).atStartOfDay().toTimestamp()
                                    if (newDate != newPlan.startDate) {
                                        haptic.performHapticFeedback(HapticFeedbackType.SegmentFrequentTick)
                                        newPlan = newPlan.copy(startDate = newDate)
                                    }
                                },
                                enabled = true,
                                valueRange = 1f..28f,
                                steps = 26
                            )
                        }
                    }
                }
            }

            categoryTitleSmall(R.string.zero_rated_apps)
            item {
                val appDatabase: AppDatabase = koinInject()
                val excludedApps by remember { derivedStateOf {
                    appDatabase.suspiciousApps.filter { newPlan.excludedApps.contains(it.uid) }
                } }
                val includedApps by remember { derivedStateOf {
                    appDatabase.suspiciousApps.filter { !newPlan.excludedApps.contains(it.uid) }
                } }
                Column(
                    modifier = Modifier
                        .card()
                        .padding(vertical = 8.dp),
                ) {
                    var addApps by remember { mutableStateOf(false) }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .animateContentSize(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val brush = Brush.horizontalGradient(0.95f to Color.Black, 1f to Color.Transparent)
                        AppSelector(
                            uids = excludedApps,
                            Modifier
                                .weight(1f)
                                .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)
                                .drawWithContent {
                                    drawContent()
                                    drawRect(brush = brush, blendMode = BlendMode.DstIn)
                                }
                        ) { uid ->
                            newPlan = newPlan.copy(excludedApps = newPlan.excludedApps.filter { it != uid })
                        }
                        FilledIconButton (
                            modifier = Modifier.padding(end = 8.dp),
                            onClick = {addApps = !addApps},
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.add),
                                contentDescription = null
                            )
                        }
                    }

                    AnimatedVisibility(addApps, modifier = Modifier.fillMaxWidth()) {
                        val searchBarState = rememberSearchBarState()
                        var query by remember { mutableStateOf("") }
                        val searchResults by remember { derivedStateOf {
                            if (query.isEmpty()) includedApps.sortedByDescending { specialApps.indexOf(it.packageName) }
                            else includedApps.filter { appDatabase.getLabel(it).lowercase().contains(query.lowercase()) }
                        } }

                        Column(
                            modifier = Modifier.padding(top = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            HorizontalDivider()
                            AppSelector(searchResults) { uid ->
                                newPlan = newPlan.copy(excludedApps = newPlan.excludedApps + (includedApps.map { it.uid }.filter { it == uid }))
                            }
                            SearchBar(
                                state = searchBarState,
                                inputField = { SearchBarDefaults.InputField(
                                    query = query,
                                    onQueryChange = {query = it},
                                    onSearch = {},
                                    expanded = false,
                                    onExpandedChange = {},
                                    leadingIcon = {
                                        Icon(
                                            painter = painterResource(R.drawable.search),
                                            contentDescription = null
                                        )
                                    }
                                )}
                            )
                        }
                    }

                }
            }
            categoryTitleSmall(R.string.background)
            item {
                LazyRow(
                    modifier = Modifier.card(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(8.dp)
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppSelector(
    uids: List<ApplicationInfo>,
    modifier: Modifier = Modifier,
    onClick: (uid: Int) -> Unit
) {
    val appDatabase: AppDatabase = koinInject()

    LazyRow(modifier, contentPadding = PaddingValues(horizontal = 8.dp)) {
        item ("holder") {  }
        items(uids, {it.uid}) {
            val painter = rememberAsyncImagePainter(AppIcon(it.packageName))
            val label = appDatabase.getLabel(it)
            TooltipBox(
                modifier = Modifier
                    .clip(MaterialTheme.shapes.medium)
                    .clickable { onClick(it.uid) }
                    .padding(4.dp),
                positionProvider = TooltipDefaults.rememberTooltipPositionProvider(
                    TooltipAnchorPosition.Above,
                    4.dp
                ),
                tooltip = {
                    PlainTooltip { Text(label) }
                },
                state = rememberTooltipState(),
            ) {
                Column(
                    Modifier.width(64.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Image(
                        modifier = Modifier.size(52.dp),
                        painter = painter,
                        contentDescription = null,
                    )
                    Text(
                        text = label,
                        fontFamily = robotoFlex(0f, 25f, 500f),
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
        if (uids.isEmpty()) {
            item {
                Text(
                    modifier = Modifier.padding(start = 8.dp),
                    text = stringResource(R.string.no_apps),
                    fontFamily = robotoFlex(0f, 151f, 1000f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun BackgroundSelector(i: Int, newPlan: DataPlan, onClick: () -> Unit) {
    val haptic = LocalHapticFeedback.current
    Box(
        modifier = Modifier
            .width(192.dp)
            .height(128.dp)
            .clip(MaterialTheme.shapes.medium)
            .background(MaterialTheme.colorScheme.background)
            .border(1.dp, MaterialTheme.colorScheme.primary, MaterialTheme.shapes.medium)
            .clickable {
                onClick()
                haptic.performHapticFeedback(HapticFeedbackType.Confirm)
            },
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
            val number = fieldState.text.toString().split('.').first().toFloatOrNull() ?: 0f
            onSizeUpdate(number)
            scale.animateTo(
                targetValue = (1.5 * (1 - E.pow(-number * 0.1))).toFloat(),
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

/**
 * Apps most often included as zero-rated.
 *
 * The further down the list the app, the higher it will be placed when sorted.
 */
val specialApps = listOf(
    "com.amazon.avod.thirdpartyclient", // Prime Video
    "org.telegram.messenger",
    "com.microsoft.teams",
    "us.zoom.videomeetings",
    "com.waze",
    "com.google.android.apps.maps",
    "com.apple.android.music",
    "com.netflix.mediaclient",
    "com.ss.android.ugc.trill", // TikTok
    "com.google.android.youtube",
    "com.spotify.music",
    "com.snapchat.android",
    "com.twitter.android",
    "com.instagram.android",
    "com.facebook.orca", // Facebook Messenger
    "com.facebook.katana", // Facebook
    "com.whatsapp",
)
