@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package com.leekleak.trafficlight.ui.overview

import android.annotation.SuppressLint
import android.telephony.SubscriptionInfo
import androidx.annotation.FloatRange
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ButtonGroup
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.leekleak.trafficlight.R
import com.leekleak.trafficlight.database.DataPlan
import com.leekleak.trafficlight.database.HourlyUsageRepo
import com.leekleak.trafficlight.ui.theme.backgrounds
import com.leekleak.trafficlight.util.DataSize
import com.leekleak.trafficlight.util.DataSizeUnit
import com.leekleak.trafficlight.util.fromTimestamp
import com.leekleak.trafficlight.util.toTimestamp
import org.koin.compose.koinInject
import java.text.DecimalFormat
import java.time.Duration
import java.time.LocalDateTime

@SuppressLint("LocalContextGetResourceValueCall")
@Composable
fun ConfiguredDataPlan(info: SubscriptionInfo, dataPlan: DataPlan, onConfigure: () -> Unit) {
    val context = LocalContext.current
    val hourlyUsageRepo: HourlyUsageRepo = koinInject()

    var expanded by remember { mutableStateOf(false) }
    val dataUsage = remember(dataPlan) { hourlyUsageRepo.planUsage(dataPlan) }

    Box(modifier = Modifier
        .fillMaxWidth()
        .clip(MaterialTheme.shapes.medium)
        .clickable(onClick = { expanded = !expanded })
        .border(1.dp, MaterialTheme.colorScheme.primary, MaterialTheme.shapes.medium),
    ) {
        backgrounds[dataPlan.uiBackground]?.let { background ->
            Image(
                modifier = Modifier
                    .matchParentSize()
                    .graphicsLayer(scaleX = 1.2f, scaleY = 1.2f),
                painter = painterResource(background),
                contentDescription = null,
                contentScale = ContentScale.FillWidth,
                colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.primaryContainer)
            )
        }
        Column (Modifier.padding(8.dp)) {
            Column(Modifier.height(184.dp)) {
                Row {
                    SimIcon(info.simSlotIndex + 1)
                    Text(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(end = 4.dp),
                        text = info.carrierName.toString(),
                        fontFamily = carrierFont(),
                        textAlign = TextAlign.End
                    )
                }
                val usage = DataSize(dataUsage.totalCellular.toDouble()).getAsUnit(DataSizeUnit.GB)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 32.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.Bottom
                ) {
                    val formatter = remember { DecimalFormat("0.##") }
                    Text(
                        text = formatter.format(usage),
                        fontFamily = bigFont(),
                        fontSize = 64.sp,
                    )
                    val data = DataSize(dataPlan.dataMax.toDouble()).toStringParts()
                    Text(
                        text = "/${data[0]}${if (data[1] != "0") ("." + data[1]) else ""}GB",
                        fontFamily = bigFont(),
                        fontSize = 36.sp,
                        lineHeight = 48.sp
                    )
                }

                Column(
                    modifier = Modifier
                        .padding(8.dp)
                        .height(48.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp, Alignment.Bottom),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    val reset by remember(dataPlan) {
                        derivedStateOf {
                            val now = LocalDateTime.now()
                            var next = fromTimestamp(dataPlan.startDate)
                            while (next < now) next = next.plusMonths(1)
                            Duration.between(now, next).toDays().toInt() + 1
                        }
                    }
                    Text(
                        text = pluralStringResource(R.plurals.resets_in_days, reset, reset),
                        fontFamily = robotoFlex(0f, 150f, 1000f)
                    )
                    val lineUsage = DataSize(usage, unit = DataSizeUnit.GB)
                    LinearWavyProgressIndicator(
                        modifier = Modifier.fillMaxWidth(),
                        progress = {
                            if (dataPlan.dataMax == 0L) 0f
                            else (lineUsage.getBitValue()
                                .toDouble() / dataPlan.dataMax.toDouble()).toFloat()
                                .coerceIn(0f, 1f)
                        },
                    )
                }
            }

            AnimatedVisibility(expanded, Modifier.fillMaxWidth()) {
                ButtonGroup(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp)
                        .height(48.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterHorizontally),
                    expandedRatio = 0.05f,
                    overflowIndicator = {}
                ) {
                    clickableItem(
                        label = context.getString(R.string.add_data),
                        icon = {
                            Icon(
                                painterResource(R.drawable.add),
                                contentDescription = stringResource(R.string.wifi)
                            )
                        },
                        weight = 1f,
                        onClick = {}
                    )
                    customItem(
                        buttonGroupContent = {
                            val interactionSource = remember { MutableInteractionSource() }
                            FilledIconButton(
                                modifier = Modifier.animateWidth(interactionSource),
                                onClick = onConfigure,
                                shape = MaterialTheme.shapes.extraLargeIncreased,
                                interactionSource = interactionSource
                            ) {
                                Icon(
                                    painterResource(R.drawable.settings),
                                    contentDescription = stringResource(R.string.configure_plan)
                                )
                            }
                        },
                        menuContent = {}
                    )
                }
            }
        }
    }
}


@SuppressLint("LocalContextGetResourceValueCall")
@Composable
fun UnconfiguredDataPlan(info: SubscriptionInfo, subscriberID: String, onConfigure: () -> Unit) {
    val context = LocalContext.current
    val hourlyUsageRepo: HourlyUsageRepo = koinInject()

    val dataUsage = remember {
        hourlyUsageRepo.planUsage(
            DataPlan(
                subscriberID = subscriberID,
                startDate = LocalDateTime.now().withDayOfMonth(1).minusDays(1).toTimestamp()
            )
        )
    }
    val usage = DataSize(dataUsage.totalCellular.toDouble()).getAsUnit(DataSizeUnit.GB)
    val formatter = remember { DecimalFormat("0.##") }
    Column(
        modifier = Modifier
            .height(200.dp)
            .clip(MaterialTheme.shapes.medium)
            .border(1.dp, MaterialTheme.colorScheme.primary, MaterialTheme.shapes.medium)
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row {
            SimIcon(info.simSlotIndex + 1)
            Text(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(end = 4.dp),
                text = info.carrierName.toString(),
                fontFamily = carrierFont(),
                textAlign = TextAlign.End
            )
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.Bottom
        ) {
            Text(
                text = formatter.format(usage),
                fontFamily = bigFont(),
                fontSize = 64.sp,
            )
            Text(
                text = "GB",
                fontFamily = bigFont(),
                fontSize = 36.sp,
                lineHeight = 48.sp
            )
        }
        Text(
            text = stringResource(R.string.this_month),
            fontSize = 18.sp,
            fontFamily = robotoFlex(0f,150f,1000f)
        )
        ButtonGroup(
            modifier = Modifier
                .padding(top = 8.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.Bottom,
            overflowIndicator = {}
        ) {
            clickableItem(
                label = context.getString(R.string.configure_plan),
                icon = {
                    Icon(
                        painterResource(R.drawable.settings),
                        contentDescription = null
                    )
                },
                onClick = onConfigure
            )
        }
    }
}

@Composable
fun SimIcon(number: Int) {
    Box(contentAlignment = Alignment.Center) {
        Icon(
            painterResource(R.drawable.sim_card),
            contentDescription = stringResource(R.string.sim_card)
        )
        Text(
            modifier = Modifier.padding(top = 2.dp, start = 0.5.dp),
            text = number.toString(),
            fontSize = 12.sp,
            fontFamily = robotoFlex(0f,25f,500f)
        )
    }
}

@Composable
fun carrierFont(): FontFamily? = robotoFlex(-10f,25f,675f)

@OptIn(ExperimentalTextApi::class)
@Composable
fun bigFont(): FontFamily? {
    val viewModel: OverviewVM = viewModel()
    val expressiveFonts by viewModel.preferenceRepo.expressiveFonts.collectAsState(true)

    return if (expressiveFonts) {
        FontFamily(
            Font(
                R.font.do_hyeon,
            ),
        )
    } else {
        null
    }
}

@OptIn(ExperimentalTextApi::class)
@Composable
fun robotoFlex(
    @FloatRange(-10.0, 0.0) slant: Float,
    @FloatRange(25.0, 151.0) width: Float,
    @FloatRange(100.0, 1000.0) weight: Float
): FontFamily? {
    val viewModel: OverviewVM = viewModel()
    val expressiveFonts by viewModel.preferenceRepo.expressiveFonts.collectAsState(true)

    return if (expressiveFonts) {
        FontFamily(
            Font(
                R.font.roboto_flex,
                variationSettings = FontVariation.Settings(
                    FontVariation.Setting("slnt", slant),
                    FontVariation.Setting("wdth", width),
                    FontVariation.Setting("wght", weight)
                )
            ),
        )
    } else {
        null
    }
}