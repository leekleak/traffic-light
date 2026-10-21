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
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.foundation.text.input.maxLength
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonGroup
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DatePickerState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.MaterialTheme.typography
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
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
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
import androidx.compose.ui.graphics.drawscope.translate
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
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.leekleak.trafficlight.R
import com.leekleak.trafficlight.charts.ExtraGraph
import com.leekleak.trafficlight.charts.GraphTheme.wifiShape
import com.leekleak.trafficlight.database.DataPlan
import com.leekleak.trafficlight.database.DataPlanExtra
import com.leekleak.trafficlight.database.TimeInterval
import com.leekleak.trafficlight.model.DataUID
import com.leekleak.trafficlight.model.search
import com.leekleak.trafficlight.ui.components.BackAction
import com.leekleak.trafficlight.ui.navigation.Navigator
import com.leekleak.trafficlight.ui.settings.FancyDialog
import com.leekleak.trafficlight.ui.settings.IconPreference
import com.leekleak.trafficlight.ui.settings.NotificationWarningDialog
import com.leekleak.trafficlight.ui.settings.PermissionCard
import com.leekleak.trafficlight.ui.settings.Preference
import com.leekleak.trafficlight.ui.settings.SliderComponent
import com.leekleak.trafficlight.ui.settings.SwitchPreference
import com.leekleak.trafficlight.ui.theme.LocalSizeMetric
import com.leekleak.trafficlight.ui.theme.backgrounds
import com.leekleak.trafficlight.ui.theme.card
import com.leekleak.trafficlight.ui.theme.doHyeonFont
import com.leekleak.trafficlight.ui.theme.googleSans
import com.leekleak.trafficlight.ui.theme.googleSansEmphasized
import com.leekleak.trafficlight.util.DataSize
import com.leekleak.trafficlight.util.DataSizeUnit
import com.leekleak.trafficlight.util.PageTitle
import com.leekleak.trafficlight.util.SearchField
import com.leekleak.trafficlight.util.SlideAnimatedVisibility
import com.leekleak.trafficlight.util.TOP_BAR_HEIGHT
import com.leekleak.trafficlight.util.categoryTitleSmall
import com.leekleak.trafficlight.util.clearFocusOnTap
import com.leekleak.trafficlight.util.fromTimestamp
import com.leekleak.trafficlight.util.openLink
import com.leekleak.trafficlight.util.px
import com.leekleak.trafficlight.util.toTimestamp
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.rememberHazeState
import timber.log.Timber
import java.text.DecimalFormat
import java.text.NumberFormat
import java.time.LocalDate
import java.time.LocalTime
import kotlin.math.E
import kotlin.math.max
import kotlin.math.pow

@Composable
fun DataPlanConfig(
    navigator: Navigator,
    viewModel: DataPlanConfigVM
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val activity = LocalActivity.current

    val newPlan by viewModel.newPlan.collectAsStateWithLifecycle()
    var showDeleteDialog by remember { mutableStateOf(false) }

    val showForegroundNotificationWarning by viewModel.showForegroundNotificationWarning.collectAsStateWithLifecycle()
    if (showForegroundNotificationWarning) {
        NotificationWarningDialog(onDismiss = viewModel::hideForegroundNotificationWarning)
    }

    if (showDeleteDialog) {
        FancyDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = stringResource(R.string.delete_plan),
            icon = painterResource(R.drawable.deleted),
            content = {Text(stringResource(R.string.delete_plan_confirmation))},
            actionButton = {
                TextButton(
                    onClick = {
                        viewModel.delete()
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        navigator.goBack()
                    }
                ) {
                    Text(stringResource(R.string.delete), color = colorScheme.error)
                }
            }
        )
    }

    val metric = LocalSizeMetric.current

    Scaffold(
        modifier = Modifier.clearFocusOnTap(),
        bottomBar = {
            Row(
                modifier = Modifier.fillMaxWidth().padding(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally))
            {
                Row(
                    modifier = Modifier
                        .navigationBarsPadding()
                        .clip(MaterialTheme.shapes.extraLargeIncreased)
                        .background(colorScheme.surface)
                        .border(
                            width = 1.5.dp,
                            color = colorScheme.onSurfaceVariant.copy(alpha = 0.38f),
                            shape = MaterialTheme.shapes.extraLargeIncreased
                        )
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TextButton(
                        onClick = {
                            viewModel.onCalculateUsage()
                            haptic.performHapticFeedback(HapticFeedbackType.ContextClick)
                        },
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.magic),
                            contentDescription = null
                        )
                        Text(
                            modifier = Modifier.padding(start = 8.dp),
                            text = stringResource(R.string.calculate_usage)
                        )
                    }

                    Button(onClick = {
                        viewModel.save()
                        haptic.performHapticFeedback(HapticFeedbackType.ToggleOn)
                        navigator.goBack()
                    }) {
                        Icon(
                            painter = painterResource(R.drawable.save),
                            contentDescription = stringResource(R.string.save)
                        )
                        Text(
                            modifier = Modifier.padding(start = 8.dp),
                            text = stringResource(R.string.save)
                        )
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
                val size by remember(newPlan, metric) { derivedStateOf {
                    newPlan.mainDataSize.getAsUnit(newPlan.mainDataSizeUnit, metric)
                } }
                PlanSizeConfig (
                    size = size,
                    unit = newPlan.mainDataSizeUnit,
                    onSizeUpdate = { size ->
                        viewModel.updatePlan { it.copy(mainDataSize = DataSize(size)) }
                    },
                    onUnitUpdate = { unit ->
                        viewModel.updatePlan {
                            it.copy(
                                mainDataSize = DataSize((size * unit.toBits(if (metric) 1000.0 else 1024.0)).toLong()),
                                mainDataSizeUnit = unit
                            )
                        }
                    }
                )
            }
            categoryTitleSmall { stringResource(R.string.type) }
            typeConfig(
                plan = newPlan,
                onManualUsageChange = { usage ->
                    viewModel.updatePlan {
                        it.copy(
                            mainDataUsed = usage,
                        )
                    }
                },
            ) { plan ->
                viewModel.updatePlan { plan }
            }
            categoryTitleSmall { stringResource(R.string.extras) }
            extrasConfig(newPlan) { plan -> viewModel.updatePlan { it.copy(extras = plan.extras) } }
            categoryTitleSmall { stringResource(R.string.zero_rated_apps) }
            item {
                val suspiciousApps by viewModel.suspiciousApps.collectAsStateWithLifecycle()

                val excludedApps by remember { derivedStateOf {
                    suspiciousApps.filter { newPlan.excludedApps.contains(it.uid) }
                } }
                val includedApps by remember { derivedStateOf {
                    suspiciousApps.filter { !newPlan.excludedApps.contains(it.uid) }
                } }

                Column(
                    modifier = Modifier
                        .padding(vertical = 4.dp)
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
                            viewModel.updatePlan {
                                it.copy(excludedApps = it.excludedApps.filter { app -> app != uid })
                            }
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
                                viewModel.updatePlan {
                                    it.copy(excludedApps = it.excludedApps +
                                        (includedApps
                                            .map { app -> app.uid }
                                            .filter { app -> app == uid })
                                    )
                                }
                            }
                            Box(modifier = Modifier.padding(horizontal = 8.dp)) {
                                SearchField(
                                    textFieldState = textFieldState,
                                    placeholder = stringResource(R.string.search),
                                    icon = painterResource(R.drawable.search)
                                )
                            }
                        }
                    }

                }
            }
            categoryTitleSmall { stringResource(R.string.notifications) }
            item {
                val notificationPermission by viewModel.notificationPermission.collectAsStateWithLifecycle()
                val notificationPermissionCallback = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {}

                SlideAnimatedVisibility(!notificationPermission) {
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
                    icon = painterResource(R.drawable.usage_notification),
                    value = newPlan.notification,
                    enabled = notificationPermission,
                    onValueChanged = viewModel::onForegroundNotificationValueChanged,
                )
                SlideAnimatedVisibility(newPlan.notification && Build.VERSION.SDK_INT >= Build.VERSION_CODES.BAKLAVA) {
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
                            onValueChanged = viewModel::onLiveNotificationValueChanged
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
                    onValueChanged = viewModel::onBudgetNotificationValueChanged
                )
                SwitchPreference(
                    title = stringResource(R.string.safety_status_warning),
                    summary = stringResource(R.string.safety_status_warning_description),
                    icon = painterResource(R.drawable.shield),
                    value = newPlan.safetyWarning,
                    enabled = notificationPermission,
                    onValueChanged = viewModel::onSafetyNotificationValueChanged
                )
            }
            categoryTitleSmall { stringResource(R.string.notes) }
            item {
                val noteState = rememberTextFieldState(newPlan.note)
                LaunchedEffect(noteState.text) {
                    viewModel.updatePlan { it.copy(note = noteState.text.toString()) }
                }
                BasicTextField(
                    state = noteState,
                    modifier = Modifier
                        .padding(vertical = 4.dp)
                        .fillMaxWidth()
                        .card()
                        .padding(12.dp),
                    textStyle = typography.bodyLarge.copy(color = colorScheme.onSurface),
                    cursorBrush = SolidColor(colorScheme.onSurface),
                    decorator = { innerTextField ->
                        if (noteState.text.isEmpty()) {
                            Text(
                                text = stringResource(R.string.write_your_note_here),
                                style = typography.bodyLarge,
                                color = colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                            )
                        }
                        innerTextField()
                    }
                )
            }
            categoryTitleSmall { stringResource(R.string.background) }
            item {
                LazyRow(
                    modifier = Modifier.padding(vertical = 4.dp).card(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(8.dp)
                ) {
                    items(backgrounds.size) { i ->
                        BackgroundSelector(i, newPlan) {
                            viewModel.updatePlan { it.copy(uiBackground = i) }
                        }
                    }
                }
            }
        }
        PageTitle (
            backAction = BackAction.Normal(navigator),
            hazeState = hazeState,
            text = stringResource(R.string.configure_plan),
            customElement = {
                IconButton(
                    enabled = newPlan.configured,
                    onClick = { showDeleteDialog = true }
                ) {
                    Icon(
                        painter = painterResource(R.drawable.deleted),
                        contentDescription = stringResource(R.string.delete),
                    )
                }
            }
        )
    }
}

private fun LazyListScope.typeConfig(
    plan: DataPlan,
    onManualUsageChange: (Long) -> Unit,
    onPlanChange: (plan: DataPlan) -> Unit
) {
    item {
        val haptic = LocalHapticFeedback.current
        Column(
            modifier = Modifier
                .padding(vertical = 4.dp)
                .card()
                .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            val interval by remember(plan) { derivedStateOf { plan.interval } }
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
                        onPlanChange(plan.copy(interval = TimeInterval.MONTH))
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
                        onPlanChange(plan.copy(interval = TimeInterval.DAY))
                        haptic.performHapticFeedback(HapticFeedbackType.ToggleOn)
                    },
                    weight = 1f,
                )
            }

            val selectedMonthDay = fromTimestamp(plan.startDate).dayOfMonth

            AnimatedContent(interval) { currentInterval ->
                if (currentInterval == TimeInterval.MONTH) {
                    Column {
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
                                if (newDate != plan.startDate) {
                                    onPlanChange(plan.copy(startDate = newDate))
                                }
                            }
                        )
                        HorizontalDivider()
                        SwitchPreference(
                            title = stringResource(R.string.rollover),
                            summary = stringResource(R.string.rollover_description),
                            icon = painterResource(R.drawable.repeat),
                            value = plan.recurring,
                            onValueChanged = { onPlanChange(plan.copy(recurring = it)) }
                        )
                    }
                } else {
                    CustomPlanSetup(
                        newPlan = plan,
                        onChange = { date, time, multiplier ->
                            onPlanChange(
                                plan.copy(
                                    startDate = date.atStartOfDay().toTimestamp() + time.toSecondOfDay() * 1000,
                                    intervalMultiplier = multiplier
                                )
                            )
                        }
                    )
                }
            }
            
            val font = remember { googleSans(weight = 600f) }
            val metric = LocalSizeMetric.current
            val formatter = remember { DecimalFormat("0.##") }
            val numberFormat = remember { NumberFormat.getInstance() }
            var displayUnit by remember {
                val unit = DataSize(plan.mainDataUsed).unit(metric)
                mutableStateOf(if (unit == DataSizeUnit.GB) DataSizeUnit.GB else DataSizeUnit.MB)
            }
            val textFieldState = rememberTextFieldState(
                formatter.format(DataSize(plan.mainDataUsed).getAsUnit(displayUnit, metric))
            )

            LaunchedEffect(textFieldState.text) {
                val text = textFieldState.text.toString()
                if (text.isNotEmpty()) {
                    val parsed = try { numberFormat.parse(text)?.toDouble() } catch (_: Exception) { null }
                    if (parsed != null) {
                        val base = if (metric) 1000.0 else 1024.0
                        val newUsed = (parsed * displayUnit.toBits(base)).toLong()

                        val currentValue = DataSize(plan.mainDataUsed).getAsUnit(displayUnit, metric)
                        if (text != formatter.format(currentValue)) {
                            Timber.e(newUsed.toString())
                            onManualUsageChange(newUsed)
                        }
                    }
                }
            }

            HorizontalDivider()

            Preference(
                title = stringResource(R.string.plan_usage),
                icon = painterResource(R.drawable.data_usage),
                controls = {
                    Row(
                        modifier = Modifier.height(IntrinsicSize.Min),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .padding(vertical = 4.dp)
                                .background(colorScheme.primaryContainer, MaterialTheme.shapes.medium)
                                .padding(horizontal = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            BasicTextField(
                                modifier = Modifier.width(IntrinsicSize.Min).height(IntrinsicSize.Min),
                                state = textFieldState,
                                textStyle = TextStyle(
                                    fontFamily = font,
                                    color = colorScheme.onPrimaryContainer,
                                    fontSize = 22.sp,
                                    textAlign = TextAlign.End,
                                ),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                            )
                        }
                        Button(
                            onClick = {
                                displayUnit = if (displayUnit == DataSizeUnit.GB) DataSizeUnit.MB else DataSizeUnit.GB
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            },
                            shape = MaterialTheme.shapes.medium,
                            contentPadding = PaddingValues(horizontal = 12.dp)
                        ) {
                            Text(
                                text = displayUnit.name,
                                fontFamily = font,
                                fontSize = 16.sp,
                            )
                        }
                    }
                }
            )
        }
    }
}

@Composable
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
        contentPadding = PaddingValues(8.dp),
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
                        .background(colorScheme.primary)
                        .padding(horizontal = 6.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterHorizontally)
                ) {
                    BasicTextField(
                        modifier = Modifier.width(IntrinsicSize.Min),
                        state = textFieldState,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        inputTransformation = InputTransformation.maxLength(3),
                        textStyle = typography.titleMediumEmphasized.copy(colorScheme.onPrimary),
                        cursorBrush = SolidColor(colorScheme.onPrimary),
                    )
                    Text(
                        text = stringResource(R.string.days),
                        textAlign = TextAlign.Center,
                        color = colorScheme.onPrimary,
                        style = typography.titleMediumEmphasized
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
    contentPadding: PaddingValues = PaddingValues(0.dp),
    onDateSelect: (date: LocalDate) -> Unit,
    onTimeSelect: (time: LocalTime) -> Unit,
    extraItems: LazyListScope.(font: FontFamily) -> Unit = {}
) {
    val haptic = LocalHapticFeedback.current
    val fontFamily = remember { googleSans(weight = 600f) }
    var datePickerVisible by remember { mutableStateOf(false) }
    var timePickerVisible by remember { mutableStateOf(false) }
    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = contentPadding,
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
                        style = typography.titleMediumEmphasized
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
                        style = typography.titleMediumEmphasized
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
                    onClick = {
                        datePickerVisible = false
                        datePickerState.getSelectedDate()?.let { onDateSelect(it) }
                        haptic.performHapticFeedback(HapticFeedbackType.Confirm)
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
                        haptic.performHapticFeedback(HapticFeedbackType.Confirm)
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
                    fontFamily = googleSansEmphasized(),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = colorScheme.onSurfaceVariant
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
            .background(colorScheme.background)
            .border(1.dp, colorScheme.primaryContainer, MaterialTheme.shapes.medium)
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
                colorFilter = ColorFilter.tint(colorScheme.primaryContainer)
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
    onSizeUpdate: (Long) -> Unit,
    onUnitUpdate: (DataSizeUnit) -> Unit
) {
    Box(
        modifier = Modifier
            .padding(top = TOP_BAR_HEIGHT)
            .height(172.dp * 1.5f)
            .fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        val shape = wifiShape().toPath()
        val shapeSizeBase = 172.dp.px
        val shapeColor = colorScheme.primaryContainer
        val scale = remember { Animatable(0f) }
        val haptic = LocalHapticFeedback.current
        val metric = LocalSizeMetric.current

        val shapeTransformed = remember(scale.value) {
            val sizePx = shapeSizeBase * (1 + scale.value)
            val matrix = Matrix().apply {
                scale(sizePx, sizePx)
                translate(-0.5f, -0.5f)
            }
            shape.copy().apply { transform(matrix) }
        }

        val formatter = remember { DecimalFormat("0.###") }
        val numberFormat = remember { NumberFormat.getInstance() }
        val fieldState = rememberTextFieldState()

        LaunchedEffect(size) {
            val formatted = formatter.format(size)
            if (fieldState.text.toString() != formatted) {
                fieldState.setTextAndPlaceCursorAtEnd(formatted)
            }
        }

        LaunchedEffect(fieldState.text) {
            val number = try { numberFormat.parse(fieldState.text.toString()) } catch (_: Exception) { null }
            if (number != null) {
                onSizeUpdate((number.toFloat() * unit.toBits(if (metric) 1000.0 else 1024.0)).toLong())
                scale.animateTo(
                    targetValue = (0.75 * (1 - E.pow(-number.toFloat() * 0.1))).toFloat(),
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessMedium
                    )
                )
            }
        }

        Box(
            modifier = Modifier
                .size(172.dp * (1 + scale.value))
                .drawWithCache {
                    onDrawBehind {
                        rotate(scale.value * 60f, pivot = this.center) {
                            translate(this.size.width / 2, this.size.height / 2) {
                                drawPath(
                                    path = shapeTransformed,
                                    color = shapeColor,
                                )
                            }
                        }
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally)
            ) {
                val fontFamilyDoHyeon = remember { doHyeonFont() }
                BasicTextField(
                    state = fieldState,
                    modifier = Modifier
                        .width(IntrinsicSize.Min)
                        .alignByBaseline(),
                    inputTransformation = InputTransformation {
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
                        fontSize = 64.sp,
                        color = colorScheme.onPrimaryContainer,
                        textAlign = TextAlign.End,
                        textDecoration = TextDecoration.Underline
                    ),
                    cursorBrush = SolidColor(colorScheme.primary),
                    lineLimits = TextFieldLineLimits.SingleLine,
                )
                Text(
                    modifier = Modifier
                        .alignBy { it.measuredHeight }
                        .clip(MaterialTheme.shapes.medium)
                        .background(colorScheme.onPrimaryContainer)
                        .clickable {
                            onUnitUpdate(if (unit == DataSizeUnit.GB) DataSizeUnit.MB else DataSizeUnit.GB)
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        }
                        .padding(start = 8.dp, end = 8.dp, top = 8.dp, bottom = 6.dp),
                    fontFamily = fontFamilyDoHyeon,
                    fontSize = 24.sp,
                    maxLines = 1,
                    color = colorScheme.primaryContainer,
                    text = unit.name
                )
            }
        }
    }
}

private fun LazyListScope.extrasConfig(newPlan: DataPlan, onPlanChange: (plan: DataPlan) -> Unit) {
    val extrasWithAdd = newPlan.extras.map { it as Any? } + listOf(null)
    extrasWithAdd.chunked(2).forEach { chunk ->
        item {
            val haptic = LocalHapticFeedback.current
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                chunk.forEach { item ->
                    if (item is DataPlanExtra) {
                        Box(modifier = Modifier.weight(1f)) {
                            ExtraGraph(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(
                                        colorScheme.surfaceContainer,
                                        MaterialTheme.shapes.medium
                                    ),
                                extra = item,
                                showOnlyMax = false
                            )
                            FilledIconButton(
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    onPlanChange(newPlan.copy(extras = newPlan.extras.filter { it.id != item.id }))
                                },
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(4.dp)
                                    .size(32.dp),
                                shape = MaterialTheme.shapes.small,
                                colors = IconButtonDefaults.filledIconButtonColors(
                                    containerColor = colorScheme.errorContainer.copy(alpha = 0.8f),
                                    contentColor = colorScheme.onErrorContainer
                                )
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.close),
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    } else {
                        var showAddExtraDialog by remember { mutableStateOf(false) }
                        if (showAddExtraDialog) {
                            AddExtraDialog(
                                onDismiss = { showAddExtraDialog = false },
                                onConfirm = { extra ->
                                    onPlanChange(newPlan.copy(extras = newPlan.extras + extra))
                                }
                            )
                        }
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(128.dp)
                                .clip(MaterialTheme.shapes.medium)
                                .background(colorScheme.surfaceContainer)
                                .clickable {
                                    haptic.performHapticFeedback(HapticFeedbackType.ContextClick)
                                    showAddExtraDialog = true
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.add),
                                    contentDescription = null,
                                    tint = colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = stringResource(R.string.add_extra),
                                    style = typography.labelLarge,
                                    color = colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
                if (chunk.size == 1) {
                    Box(Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun AddExtraDialog(
    onDismiss: () -> Unit,
    onConfirm: (DataPlanExtra) -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val metric = LocalSizeMetric.current
    val amountState = rememberTextFieldState("1")
    val usageState = rememberTextFieldState("0")
    var amountUnit by remember { mutableStateOf(DataSizeUnit.GB) }
    var usageUnit by remember { mutableStateOf(DataSizeUnit.GB) }
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

    FancyDialog(
        onDismissRequest = onDismiss,
        title = stringResource(R.string.add_extra),
        icon = painterResource(R.drawable.add),
        actionButton = {
            Button(onClick = {
                val amountValue = amountState.text.toString().toDoubleOrNull() ?: 1.0
                val usageValue = usageState.text.toString().toDoubleOrNull() ?: 0.0
                val base = if (metric) 1000.0 else 1024.0
                val amountBytes = (amountValue * amountUnit.toBits(base)).toLong()
                val usageBytes = (usageValue * usageUnit.toBits(base)).toLong()
                onConfirm(
                    DataPlanExtra(
                        dataAmount = DataSize(amountBytes),
                        unit = amountUnit,
                        dataUsed = usageBytes,
                        startStamp = startDate,
                        expiryStamp = expiryDate,
                        expired = false
                    )
                )
                onDismiss()
            }) {
                Text(stringResource(R.string.save))
            }
        }
    ) {
        Column(
            modifier = Modifier.verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(stringResource(R.string.amount), style = typography.titleMedium, color = colorScheme.tertiary)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                BasicTextField(
                    state = amountState,
                    modifier = Modifier
                        .weight(1f)
                        .card()
                        .background(colorScheme.background)
                        .padding(8.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    textStyle = typography.bodyLarge.copy(color = colorScheme.onSurface),
                    cursorBrush = SolidColor(colorScheme.onSurface),
                    lineLimits = TextFieldLineLimits.SingleLine
                )
                Button(
                    onClick = {
                        amountUnit = if (amountUnit == DataSizeUnit.GB) DataSizeUnit.MB else DataSizeUnit.GB
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    },
                    shape = MaterialTheme.shapes.medium,
                    contentPadding = PaddingValues(horizontal = 12.dp)
                ) {
                    Text(amountUnit.name)
                }
            }

            Text(stringResource(R.string.usage), style = typography.titleMedium, color = colorScheme.tertiary)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                BasicTextField(
                    state = usageState,
                    modifier = Modifier
                        .weight(1f)
                        .card()
                        .background(colorScheme.background)
                        .padding(8.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    textStyle = typography.bodyLarge.copy(color = colorScheme.onSurface),
                    cursorBrush = SolidColor(colorScheme.onSurface),
                    lineLimits = TextFieldLineLimits.SingleLine
                )
                Button(
                    onClick = {
                        usageUnit = if (usageUnit == DataSizeUnit.GB) DataSizeUnit.MB else DataSizeUnit.GB
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    },
                    shape = MaterialTheme.shapes.medium,
                    contentPadding = PaddingValues(horizontal = 12.dp)
                ) {
                    Text(usageUnit.name)
                }
            }

            Text(stringResource(R.string.start), style = typography.titleMedium, color = colorScheme.tertiary)
            DateAndTimePicker(
                selectedDate = fromTimestamp(startDate).toLocalDate(),
                selectedTime = fromTimestamp(startDate).toLocalTime(),
                datePickerState = startDatePickerState,
                timePickerState = startTimePickerState,
                onDateSelect = { date ->
                    val time = fromTimestamp(startDate).toLocalTime()
                    startDate =
                        date.atStartOfDay().toTimestamp() + time.toSecondOfDay() * 1000
                },
                onTimeSelect = { time ->
                    val date = fromTimestamp(startDate).toLocalDate()
                    startDate =
                        date.atStartOfDay().toTimestamp() + time.toSecondOfDay() * 1000
                }
            )

            Text(stringResource(R.string.expiry), style = typography.titleMedium, color = colorScheme.tertiary)
            DateAndTimePicker(
                selectedDate = fromTimestamp(expiryDate).toLocalDate(),
                selectedTime = fromTimestamp(expiryDate).toLocalTime(),
                datePickerState = expiryDatePickerState,
                timePickerState = expiryTimePickerState,
                onDateSelect = { date ->
                    val time = fromTimestamp(expiryDate).toLocalTime()
                    expiryDate =
                        date.atStartOfDay().toTimestamp() + time.toSecondOfDay() * 1000
                },
                onTimeSelect = { time ->
                    val date = fromTimestamp(expiryDate).toLocalDate()
                    expiryDate =
                        date.atStartOfDay().toTimestamp() + time.toSecondOfDay() * 1000
                }
            )
        }
    }
}

object PastOrPresentSelectableDates: SelectableDates {
    override fun isSelectableDate(utcTimeMillis: Long): Boolean {
        return utcTimeMillis <= System.currentTimeMillis()
    }

    override fun isSelectableYear(year: Int): Boolean {
        return year <= LocalDate.now().year
    }
}
