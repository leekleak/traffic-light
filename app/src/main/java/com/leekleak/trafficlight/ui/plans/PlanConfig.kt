package com.leekleak.trafficlight.ui.plans

import android.Manifest
import android.os.Build
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
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
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.InputTransformation
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.maxLength
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonGroup
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DatePickerState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TimePickerDialog
import androidx.compose.material3.TimePickerState
import androidx.compose.material3.TooltipAnchorPosition
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.getSelectedDate
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.material3.rememberTooltipState
import androidx.compose.material3.toPath
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
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
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.leekleak.trafficlight.R
import com.leekleak.trafficlight.charts.GraphTheme.wifiShape
import com.leekleak.trafficlight.database.DataPlan
import com.leekleak.trafficlight.database.DataPlanDao
import com.leekleak.trafficlight.database.DataPlanExtra
import com.leekleak.trafficlight.database.TimeInterval
import com.leekleak.trafficlight.model.AppManager
import com.leekleak.trafficlight.model.DataUID
import com.leekleak.trafficlight.model.NetworkUsageManager
import com.leekleak.trafficlight.model.PermissionManager
import com.leekleak.trafficlight.model.search
import com.leekleak.trafficlight.ui.navigation.Navigator
import com.leekleak.trafficlight.ui.settings.IconPreference
import com.leekleak.trafficlight.ui.settings.PermissionCard
import com.leekleak.trafficlight.ui.settings.SliderComponent
import com.leekleak.trafficlight.ui.settings.SwitchPreference
import com.leekleak.trafficlight.ui.theme.backgrounds
import com.leekleak.trafficlight.ui.theme.card
import com.leekleak.trafficlight.ui.theme.doHyeonFont
import com.leekleak.trafficlight.ui.theme.googleSans
import com.leekleak.trafficlight.ui.theme.longGoogleSans
import com.leekleak.trafficlight.util.DataSize
import com.leekleak.trafficlight.util.DataSizeUnit
import com.leekleak.trafficlight.util.PageTitle
import com.leekleak.trafficlight.util.SearchField
import com.leekleak.trafficlight.util.TOP_BAR_HEIGHT
import com.leekleak.trafficlight.util.categoryTitleSmall
import com.leekleak.trafficlight.util.fromTimestamp
import com.leekleak.trafficlight.util.openLink
import com.leekleak.trafficlight.util.px
import com.leekleak.trafficlight.util.toDp
import com.leekleak.trafficlight.util.toTimestamp
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.rememberHazeState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import java.text.DecimalFormat
import java.text.NumberFormat
import java.time.LocalDate
import java.time.LocalTime
import kotlin.math.E
import kotlin.math.max
import kotlin.math.pow

@OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalMaterial3Api::class)
@Composable
fun PlanConfig(currentPlan: DataPlan) {
    val appManager: AppManager = koinInject()
    val dataPlanDao: DataPlanDao = koinInject()
    val networkUsageManager: NetworkUsageManager = koinInject()

    val scope = rememberCoroutineScope()
    val navigator: Navigator = koinInject()
    val permissionManager: PermissionManager = koinInject()

    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val activity = LocalActivity.current

    var newPlan by remember(currentPlan) { mutableStateOf(currentPlan.copy()) }

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
                            newPlan.resetUsage()
                            newPlan.updateUsage(networkUsageManager)
                            newPlan = newPlan.copy(lastSafetyState = -1, budgetOvershotNotified = false)
                            dataPlanDao.add(newPlan)
                            haptic.performHapticFeedback(HapticFeedbackType.ToggleOn)
                            navigator.goBack()
                        }
                    }
                ) {
                    Row (horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(
                            painter = painterResource(R.drawable.save),
                            contentDescription = null
                        )
                        Text(stringResource(R.string.save))
                    }
                }
            }
        }
    ) { paddingValues ->
        val hazeState = rememberHazeState()
        LazyColumn(
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .fillMaxSize()
                .hazeSource(hazeState),
            contentPadding = paddingValues
        ) {
            item {
                val size by remember { derivedStateOf {
                    newPlan.mainDataSize.getAsUnit(newPlan.mainDataSizeUnit)
                } }
                PlanSizeConfig (
                    size = size,
                    unit = newPlan.mainDataSizeUnit,
                    onSizeUpdate = {
                        val data = DataSize((it * newPlan.mainDataSizeUnit.toBits()).toLong())
                        newPlan = newPlan.copy(mainDataSize = data)
                    },
                    onUnitUpdate = {
                        newPlan = newPlan.copy(
                            mainDataSize = DataSize((size * it.toBits()).toLong()),
                            mainDataSizeUnit = it
                        )
                    }
                )
            }
            categoryTitleSmall { stringResource(R.string.type) }
            typeConfig(newPlan) { newPlan = it }
            categoryTitleSmall { stringResource(R.string.extras) }
            extrasConfig(newPlan) { newPlan = it }
            categoryTitleSmall { stringResource(R.string.zero_rated_apps) }
            item {
                val suspiciousApps by produceState(emptyList()) { value = appManager.getAllApps() }

                val excludedApps by remember { derivedStateOf {
                    suspiciousApps.filter { newPlan.excludedApps.contains(it.uid) }
                } }
                val includedApps by remember { derivedStateOf {
                    suspiciousApps.filter { !newPlan.excludedApps.contains(it.uid) }
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
                            modifier = Modifier
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
                            onClick = {
                                addApps = !addApps
                                haptic.performHapticFeedback(HapticFeedbackType.ContextClick)
                            },
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.add),
                                contentDescription = null
                            )
                        }
                    }

                    AnimatedVisibility(addApps, modifier = Modifier.fillMaxWidth()) {
                        val textFieldState = rememberTextFieldState()
                        val searchResults by produceState<List<DataUID>>(initialValue = includedApps, textFieldState.text) {
                            value = includedApps.search(textFieldState.text.toString(), context)
                        }

                        Column(
                            modifier = Modifier.padding(top = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            HorizontalDivider()
                            AppSelector(searchResults, Modifier.fillMaxWidth()) { uid ->
                                newPlan = newPlan.copy(excludedApps = newPlan.excludedApps + (includedApps.map { it.uid }.filter { it == uid }))
                            }
                            SearchField(textFieldState)
                        }
                    }

                }
            }
            categoryTitleSmall { stringResource(R.string.notifications) }
            item {
                val notificationPermission by permissionManager.notificationPermissionFlow.collectAsState(true)
                val notificationPermissionCallback = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {}

                AnimatedVisibility(
                    visible = !notificationPermission,
                    enter = fadeIn(tween()) + slideInVertically() + expandVertically(),
                    exit = fadeOut(tween()) + slideOutVertically() + shrinkVertically()
                ) {
                    PermissionCard(
                        title = stringResource(R.string.notification_permission),
                        description = stringResource(R.string.allow_app_to_send_notifications),
                        icon = painterResource(R.drawable.notification),
                        onClick = {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                notificationPermissionCallback.launch(
                                    Manifest.permission.POST_NOTIFICATIONS
                                )
                            }
                        }
                    )
                }

                SwitchPreference (
                    title = stringResource(R.string.notifications),
                    summary = stringResource(R.string.plan_notification_description),
                    icon = painterResource(R.drawable.notification),
                    value = newPlan.notification,
                    enabled = notificationPermission,
                    onValueChanged = {
                        scope.launch {
                            newPlan = newPlan.copy(notification = it)
                        }
                    },
                )
                AnimatedVisibility(
                    visible = newPlan.notification && Build.VERSION.SDK_INT >= Build.VERSION_CODES.BAKLAVA,
                    enter = fadeIn(tween()) + slideInVertically() + expandVertically(),
                    exit = fadeOut(tween()) + slideOutVertically() + shrinkVertically()
                ) {
                    Row (
                        modifier = Modifier.height(IntrinsicSize.Min),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        SwitchPreference(
                            modifier = Modifier.weight(1f),
                            title = stringResource(R.string.live_notification),
                            icon = painterResource(R.drawable.app_badging),
                            value = newPlan.liveNotification,
                            enabled = notificationPermission,
                            onValueChanged = {
                                scope.launch {
                                    newPlan = newPlan.copy(liveNotification = it)
                                }
                            }
                        )
                        IconPreference(
                            title = stringResource(R.string.help),
                            painter = painterResource(R.drawable.help),
                            enabled = notificationPermission,
                            onClick = { openLink(activity, "https://github.com/leekleak/traffic-light/wiki/Troubleshooting#notifications") },
                        )
                    }
                }
                SwitchPreference(
                    title = stringResource(R.string.budget_overshoot_warning),
                    summary = stringResource(R.string.budget_overshoot_warning_description),
                    icon = painterResource(R.drawable.warning),
                    value = newPlan.budgetWarning,
                    enabled = notificationPermission,
                    onValueChanged = {
                        scope.launch {
                            newPlan = newPlan.copy(budgetWarning = it)
                        }
                    }
                )
                SwitchPreference(
                    title = stringResource(R.string.safety_status_warning),
                    summary = stringResource(R.string.safety_status_warning_description),
                    icon = painterResource(R.drawable.shield),
                    value = newPlan.safetyWarning,
                    enabled = notificationPermission,
                    onValueChanged = {
                        scope.launch {
                            newPlan = newPlan.copy(safetyWarning = it)
                        }
                    }
                )
            }
            categoryTitleSmall { stringResource(R.string.notes) }
            item {
                val noteState = rememberTextFieldState(newPlan.note)
                LaunchedEffect(noteState.text) {
                    newPlan = newPlan.copy(note = noteState.text.toString())
                }
                BasicTextField(
                    state = noteState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .card()
                        .padding(12.dp),
                    textStyle = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.onSurface),
                    decorator = { innerTextField ->
                        if (noteState.text.isEmpty()) {
                            Text(
                                text = stringResource(R.string.write_your_note_here),
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                            )
                        }
                        innerTextField()
                    }
                )
            }
            categoryTitleSmall { stringResource(R.string.background) }
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
        PageTitle (true, hazeState, stringResource(R.string.configure_plan))
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
private fun LazyListScope.typeConfig(newPlan: DataPlan, onPlanChange: (plan: DataPlan) -> Unit) {
    item {
        val haptic = LocalHapticFeedback.current
        Column(
            modifier = Modifier
                .card()
                .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            val interval by remember(newPlan) { derivedStateOf { newPlan.interval } }
            val monthlyString = stringResource(R.string.monthly)
            val customString = stringResource(R.string.custom)
            ButtonGroup(
                modifier = Modifier.fillMaxWidth(),
                expandedRatio = 0.05f,
                overflowIndicator = {}
            ) {
                toggleableItem(
                    checked = interval == TimeInterval.MONTH,
                    label = monthlyString,
                    icon = {
                        Icon(
                            painter = painterResource(R.drawable.calendar_month),
                            contentDescription = null
                        )
                    },
                    onCheckedChange = {
                        onPlanChange(newPlan.copy(interval = TimeInterval.MONTH))
                        haptic.performHapticFeedback(HapticFeedbackType.ToggleOn)
                    },
                    weight = 1f,
                )
                toggleableItem(
                    checked = interval == TimeInterval.DAY,
                    label = customString,
                    icon = {
                        Icon(
                            painter = painterResource(R.drawable.custom),
                            contentDescription = null
                        )
                    },
                    onCheckedChange = {
                        onPlanChange(newPlan.copy(interval = TimeInterval.DAY))
                        haptic.performHapticFeedback(HapticFeedbackType.ToggleOn)
                    },
                    weight = 1f,
                )
            }

            var selectedMonthDay by remember(newPlan) {
                mutableIntStateOf(fromTimestamp(newPlan.startDate).dayOfMonth)
            }
            AnimatedContent(interval) { currentInterval ->
                if (currentInterval == TimeInterval.MONTH) {
                    SliderComponent(
                        modifier = Modifier.padding(horizontal = 4.dp),
                        modifierLabelText = Modifier.width(46.dp),
                        title = stringResource(R.string.reset_day),
                        icon = painterResource(R.drawable.history_2),
                        value = selectedMonthDay.toLong(),
                        values = remember { (1L..28L).map { it to null } },
                        onValueChanged = {
                            val newDate =
                                LocalDate.now().withDayOfMonth(it.toInt()).toTimestamp()
                            if (newDate != newPlan.startDate) {
                                onPlanChange(newPlan.copy(startDate = newDate))
                            }
                        }
                    )
                } else {
                    CustomPlanSetup(
                        newPlan = newPlan,
                        onChange = { date, time, multiplier ->
                            onPlanChange(
                                newPlan.copy(
                                    startDate = date.atStartOfDay().toTimestamp() + time.toSecondOfDay() * 1000,
                                    intervalMultiplier = multiplier
                                )
                            )
                        }
                    )
                }
            }
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun CustomPlanSetup(newPlan: DataPlan, onChange: (date:LocalDate, time: LocalTime, multiplier: Int) -> Unit) {
    var selectedDate by remember { mutableStateOf(fromTimestamp(newPlan.startDate).toLocalDate()) }
    var selectedTime by remember { mutableStateOf(fromTimestamp(newPlan.startDate).toLocalTime()) }

    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = newPlan.startDate,
        selectableDates = PastOrPresentSelectableDates
    )

    val timePickerState = rememberTimePickerState(
        initialHour = selectedTime.hour,
        initialMinute = selectedTime.minute,
    )

    val textFieldState = rememberTextFieldState((newPlan.intervalMultiplier).toString())

    LaunchedEffect(selectedDate, selectedTime, textFieldState.text) {
        val multiplier = max(textFieldState.text.toString().toIntOrNull() ?: 1, 1)
        onChange(selectedDate, selectedTime, multiplier)
    }

    DateAndTimePicker(
        selectedDate,
        selectedTime,
        datePickerState,
        timePickerState,
        onDateSelect = {selectedDate = it},
        onTimeSelect = {selectedTime = it}
    ) { fontFamily ->
        item {
            Column(
                modifier = Modifier.width(IntrinsicSize.Max)
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(painterResource(R.drawable.length), null)
                    Text(
                        text = stringResource(R.string.length),
                        fontFamily = fontFamily,
                    )
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 2.dp)
                        .clip(MaterialTheme.shapes.medium)
                        .background(MaterialTheme.colorScheme.primary)
                        .padding(horizontal = 6.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterHorizontally)
                ) {
                    BasicTextField(
                        modifier = Modifier.width(IntrinsicSize.Min),
                        state = textFieldState,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        inputTransformation = InputTransformation.maxLength(3),
                        textStyle = MaterialTheme.typography.titleMediumEmphasized.copy(color = MaterialTheme.colorScheme.onPrimary),
                        cursorBrush = SolidColor(MaterialTheme.colorScheme.onPrimary),
                    )
                    Text(
                        text = stringResource(R.string.days),
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onPrimary,
                        style = MaterialTheme.typography.titleMediumEmphasized
                    )
                }
            }
        }
    }
}

@Composable
private fun DateAndTimePicker(
    selectedDate: LocalDate?,
    selectedTime: LocalTime?,
    datePickerState: DatePickerState,
    timePickerState: TimePickerState,
    onDateSelect: (date: LocalDate) -> Unit,
    onTimeSelect: (time: LocalTime) -> Unit,
    extraItems: LazyListScope.(font: FontFamily) -> Unit = {}
) {
    val fontFamily = remember { googleSans(weight = 600f) }
    var datePickerVisible by remember { mutableStateOf(false) }
    var timePickerVisible by remember { mutableStateOf(false) }
    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            Column {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(painterResource(R.drawable.day), null)
                    Text(
                        text = stringResource(R.string.day),
                        fontFamily = fontFamily
                    )
                }
                Button(
                    shape = MaterialTheme.shapes.medium,
                    onClick = { datePickerVisible = true }
                ) {
                    Text(
                        text = selectedDate.toString(),
                        style = MaterialTheme.typography.titleMediumEmphasized
                    )
                }
            }
        }
        item {
            Column {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(painterResource(R.drawable.clock), null)
                    Text(
                        text = stringResource(R.string.time),
                        fontFamily = fontFamily,
                    )
                }
                Button(
                    shape = MaterialTheme.shapes.medium,
                    onClick = { timePickerVisible = true }
                ) {
                    Text(
                        text = selectedTime.toString(),
                        style = MaterialTheme.typography.titleMediumEmphasized
                    )
                }
            }
        }
        extraItems(fontFamily)
    }
    if (datePickerVisible) {
        DatePickerDialog(
            onDismissRequest = { datePickerVisible = false },
            confirmButton = {
                Button(
                    enabled = datePickerState.getSelectedDate()?.let { it <= LocalDate.now() }
                        ?: false,
                    onClick = {
                        datePickerVisible = false
                        datePickerState.getSelectedDate()?.let { onDateSelect(it) }
                    }
                ) {
                    Text(stringResource(R.string.save))
                }
            }
        ) {
            DatePicker(
                state = datePickerState,
                modifier = Modifier.verticalScroll(rememberScrollState())
            )
        }
    } else if (timePickerVisible) {
        TimePickerDialog(
            onDismissRequest = { timePickerVisible = false },
            title = {},
            confirmButton = {
                Button(
                    onClick = {
                        timePickerVisible = false
                        onTimeSelect(LocalTime.of(timePickerState.hour, timePickerState.minute))
                    }
                ) {
                    Text(stringResource(R.string.save))
                }
            }
        ) {
            TimePicker(timePickerState)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppSelector(
    uids: List<DataUID>,
    modifier: Modifier = Modifier,
    onClick: (uid: Int) -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val context = LocalContext.current

    LazyRow(modifier, contentPadding = PaddingValues(horizontal = 8.dp)) {
        item ("holder") {  }
        items(uids, {it.uid}) {
            TooltipBox(
                modifier = Modifier
                    .clip(MaterialTheme.shapes.medium)
                    .clickable {
                        onClick(it.uid)
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    }
                    .padding(4.dp),
                positionProvider = TooltipDefaults.rememberTooltipPositionProvider(
                    TooltipAnchorPosition.Above,
                    4.dp
                ),
                tooltip = {
                    PlainTooltip { Text(it.getName(context)) }
                },
                state = rememberTooltipState(),
            ) {
                Column(
                    Modifier.width(64.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    it.GetIcon(Modifier.size(52.dp))
                    Text(
                        text = it.getName(context),
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
                    fontFamily = longGoogleSans(),
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
            .border(1.dp, MaterialTheme.colorScheme.primaryContainer, MaterialTheme.shapes.medium)
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
            enter = fadeIn(tween()) + scaleIn(),
            exit = fadeOut(tween()) + scaleOut()
        ) {
            Icon (
                painter = painterResource(R.drawable.checkmark),
                contentDescription = stringResource(R.string.selected_item),
            )
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun PlanSizeConfig(
    size: Double,
    unit: DataSizeUnit,
    onSizeUpdate: (Float) -> Unit,
    onUnitUpdate: (DataSizeUnit) -> Unit
) {
    Box(
        modifier = Modifier
            .padding(top = TOP_BAR_HEIGHT)
            .height(128.dp * 2.5f)
            .fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        val shape = wifiShape().toPath()
        val shapeSizeBase = 128.dp.px
        val shapeColor = MaterialTheme.colorScheme.primaryContainer
        val scale = remember { Animatable(0f) }
        val haptic = LocalHapticFeedback.current

        val shapeTransformed = remember(scale.value) {
            val sizePx = shapeSizeBase * (1 + scale.value)
            val matrix = Matrix().apply {
                scale(sizePx, sizePx)
            }
            shape.copy().apply { transform(matrix) }
        }

        val formatter = remember { DecimalFormat("0.#") }
        val numberFormat = remember { NumberFormat.getInstance() }
        val fieldState = remember(size) {
            TextFieldState(formatter.format(size))
        }

        LaunchedEffect(fieldState.text) {
            val number = try { numberFormat.parse(fieldState.text.toString()) } catch (_: Exception) { null }
            if (number != null) {
                onSizeUpdate(number.toFloat())
                scale.animateTo(
                    targetValue = (1.5 * (1 - E.pow(-number.toFloat() * 0.1))).toFloat(),
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioLowBouncy,
                        stiffness = Spring.StiffnessMedium
                    )
                )
            }
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
                horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally)
            ) {
                var intrinsics by remember { mutableIntStateOf(0) }
                val fontFamilyDoHyeon = remember { doHyeonFont() }
                BasicTextField(
                    state = fieldState,
                    modifier = Modifier
                        .width(intrinsics.toDp)
                        .alignByBaseline(),
                    inputTransformation =  InputTransformation {
                        val newText = asCharSequence().toString()
                        if (newText.isEmpty()) {
                            replace(0, length, "0")
                        } else if (newText.length > 1 && newText.startsWith("0") && !newText.contains(".")) {
                            replace(0, 1, "")
                        }
                    }.maxLength(5),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number
                    ),
                    textStyle = TextStyle(
                        fontFamily = fontFamilyDoHyeon,
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
                Box (
                    Modifier
                        .clip(MaterialTheme.shapes.medium)
                        .clickable {
                            onUnitUpdate(if (unit == DataSizeUnit.GB) DataSizeUnit.MB else DataSizeUnit.GB)
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        }
                        .border(2f.dp, MaterialTheme.colorScheme.primary, MaterialTheme.shapes.medium)
                        .padding(start = 8.dp, end = 8.dp, top = 8.dp, bottom = 6.dp)
                ) {
                    Text(
                        fontFamily = fontFamilyDoHyeon,
                        fontSize = 18.sp * (1 + scale.value/2),
                        maxLines = 1,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        text = unit.name
                    )
                }
            }
        }
    }
}

private fun LazyListScope.extrasConfig(newPlan: DataPlan, onPlanChange: (plan: DataPlan) -> Unit) {
    item {
        val haptic = LocalHapticFeedback.current
        var showAddExtraDialog by remember { mutableStateOf(false) }

        if (showAddExtraDialog) {
            AddExtraDialog(
                onDismiss = { showAddExtraDialog = false },
                onConfirm = { extra ->
                    onPlanChange(newPlan.copy(extras = newPlan.extras + extra))
                }
            )
        }

        Column(
            modifier = Modifier.card().padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            newPlan.extras.forEach { extra ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(MaterialTheme.shapes.medium)
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        val dataSizeStr = extra.dataAmount.toString()
                        val startStr = fromTimestamp(extra.startStamp).toLocalDate().toString()
                        val expiryStr = if (extra.expiryStamp != Long.MAX_VALUE) " • Exp: ${fromTimestamp(extra.expiryStamp).toLocalDate()}" else ""
                        Text(
                            text = "+$dataSizeStr ($startStr$expiryStr)",
                            style = MaterialTheme.typography.titleMedium,
                        )
                    }
                    FilledIconButton(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onPlanChange(newPlan.copy(extras = newPlan.extras.filter { it.id != extra.id }))
                        },
                        modifier = Modifier.size(32.dp),
                        shape = MaterialTheme.shapes.small,
                        colors = androidx.compose.material3.IconButtonDefaults.filledIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer,
                            contentColor = MaterialTheme.colorScheme.onErrorContainer
                        )
                    ) {
                        Icon(painterResource(R.drawable.close), null, modifier = Modifier.size(16.dp))
                    }
                }
            }

            Button(
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.ContextClick)
                    showAddExtraDialog = true
                }
            ) {
                Icon(painterResource(R.drawable.add), null)
                Text("Add Extra")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddExtraDialog(
    onDismiss: () -> Unit,
    onConfirm: (DataPlanExtra) -> Unit
) {
    val amountState = rememberTextFieldState("1")
    var unit by remember { mutableStateOf(DataSizeUnit.GB) }
    var startDate by remember { mutableLongStateOf(LocalDate.now().toTimestamp()) }
    var expiryDate by remember { mutableLongStateOf(startDate + 30L * 24 * 60 * 60 * 1000) }

    val startDatePickerState = rememberDatePickerState(initialSelectedDateMillis = startDate)
    val startTimePickerState = rememberTimePickerState(
        initialHour = fromTimestamp(startDate).hour,
        initialMinute = fromTimestamp(startDate).minute
    )

    val expiryDatePickerState = rememberDatePickerState(initialSelectedDateMillis = expiryDate)
    val expiryTimePickerState = rememberTimePickerState(
        initialHour = fromTimestamp(expiryDate).hour,
        initialMinute = fromTimestamp(expiryDate).minute
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            Button(onClick = {
                val amountValue = amountState.text.toString().toDoubleOrNull() ?: 1.0
                val amountBytes = (amountValue * unit.toBits()).toLong()
                onConfirm(
                    DataPlanExtra(
                        dataAmount = DataSize(amountBytes),
                        unit = unit,
                        dataUsed = 0L,
                        startStamp = startDate,
                        expiryStamp = expiryDate,
                        expired = false
                    )
                )
                onDismiss()
            }) {
                Text(stringResource(R.string.save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.close))
            }
        },
        title = { Text("Add Extra") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Amount", style = MaterialTheme.typography.titleMedium)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    BasicTextField(
                        state = amountState,
                        modifier = Modifier
                            .weight(1f)
                            .card()
                            .background(MaterialTheme.colorScheme.surface)
                            .padding(8.dp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        textStyle = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface),
                        cursorBrush = SolidColor(MaterialTheme.colorScheme.onSurface),
                        lineLimits = TextFieldLineLimits.SingleLine
                    )
                    Button(
                        onClick = {
                            unit = if (unit == DataSizeUnit.GB) DataSizeUnit.MB else DataSizeUnit.GB
                        },
                        shape = MaterialTheme.shapes.medium,
                        contentPadding = PaddingValues(horizontal = 12.dp)
                    ) {
                        Text(unit.name)
                    }
                }

                Text("Start", style = MaterialTheme.typography.titleMedium)
                Box(Modifier.clip(MaterialTheme.shapes.medium).background(MaterialTheme.colorScheme.surface)) {
                    DateAndTimePicker(
                        selectedDate = fromTimestamp(startDate).toLocalDate(),
                        selectedTime = fromTimestamp(startDate).toLocalTime(),
                        datePickerState = startDatePickerState,
                        timePickerState = startTimePickerState,
                        onDateSelect = { date ->
                            val time = fromTimestamp(startDate).toLocalTime()
                            startDate = date.atStartOfDay().toTimestamp() + time.toSecondOfDay() * 1000
                        },
                        onTimeSelect = { time ->
                            val date = fromTimestamp(startDate).toLocalDate()
                            startDate = date.atStartOfDay().toTimestamp() + time.toSecondOfDay() * 1000
                        }
                    )
                }

                Text("Expiry", style = MaterialTheme.typography.titleMedium)
                Box(Modifier.clip(MaterialTheme.shapes.medium).background(MaterialTheme.colorScheme.surface)) {
                    DateAndTimePicker(
                        selectedDate = fromTimestamp(expiryDate).toLocalDate(),
                        selectedTime = fromTimestamp(expiryDate).toLocalTime(),
                        datePickerState = expiryDatePickerState,
                        timePickerState = expiryTimePickerState,
                        onDateSelect = { date ->
                            val time = fromTimestamp(expiryDate).toLocalTime()
                            expiryDate = date.atStartOfDay().toTimestamp() + time.toSecondOfDay() * 1000
                        },
                        onTimeSelect = { time ->
                            val date = fromTimestamp(expiryDate).toLocalDate()
                            expiryDate = date.atStartOfDay().toTimestamp() + time.toSecondOfDay() * 1000
                        }
                    )
                }
            }
        }
    )
}

object PastOrPresentSelectableDates: SelectableDates {
    override fun isSelectableDate(utcTimeMillis: Long): Boolean {
        return utcTimeMillis <= System.currentTimeMillis()
    }

    override fun isSelectableYear(year: Int): Boolean {
        return year <= LocalDate.now().year
    }
}
