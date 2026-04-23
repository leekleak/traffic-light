@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package com.leekleak.trafficlight.ui.overview

import android.annotation.SuppressLint
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ButtonGroup
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.leekleak.trafficlight.R
import com.leekleak.trafficlight.database.DataPlan
import com.leekleak.trafficlight.database.resetString
import com.leekleak.trafficlight.model.NetworkUsageManager
import com.leekleak.trafficlight.ui.theme.backgrounds
import com.leekleak.trafficlight.ui.theme.carrierFont
import com.leekleak.trafficlight.ui.theme.doHyeonFont
import com.leekleak.trafficlight.ui.theme.robotoFlex
import com.leekleak.trafficlight.util.DataSize
import com.leekleak.trafficlight.util.DataSizeUnit
import com.leekleak.trafficlight.util.simIconRes
import org.koin.compose.koinInject
import java.text.DecimalFormat

@Composable
fun ConfiguredDataPlan(dataPlan: DataPlan, onConfigure: () -> Unit) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current

    var expanded by remember { mutableStateOf(false) }

    BoxBackground(
        background = backgrounds[dataPlan.uiBackground],
        onClick = {
            haptic.performHapticFeedback(HapticFeedbackType.ContextClick)
            expanded = !expanded
        }
    ) {
        Column {
            ConfiguredDataPlanContent(dataPlan)
            AnimatedVisibility(expanded, Modifier.fillMaxWidth()) {
                ButtonGroup(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp)
                        .height(48.dp),
                    horizontalArrangement = Arrangement.spacedBy(
                        4.dp,
                        Alignment.CenterHorizontally
                    ),
                    expandedRatio = 0.05f,
                    overflowIndicator = {}
                ) {
                    clickableItem(
                        label = context.getString(R.string.configure_plan),
                        icon = {
                            Icon(
                                painterResource(R.drawable.settings),
                                contentDescription = stringResource(R.string.configure_plan)
                            )
                        },
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onConfigure()
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun BoxBackground(
    background: Int? = null,
    onClick: () -> Unit,
    content: @Composable () -> Unit
) {
    Box (modifier = Modifier
        .fillMaxWidth()
        .clip(MaterialTheme.shapes.medium)
        .clickable { onClick() }
        .border(1.dp, MaterialTheme.colorScheme.primaryContainer, MaterialTheme.shapes.medium)
    ) {
        background?.let { background ->
            Image(
                modifier = Modifier
                    .matchParentSize()
                    .scale(1.2f),
                painter = painterResource(background),
                contentDescription = null,
                contentScale = ContentScale.FillWidth,
                colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.primaryContainer)
            )
        }
        content()
    }
}

@SuppressLint("LocalContextGetResourceValueCall")
@Composable
fun UnconfiguredDataPlan(dataPlan: DataPlan, onConfigure: () -> Unit) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val networkUsageManager: NetworkUsageManager = koinInject()

    val dataUsage by produceState(0L) { value = networkUsageManager.planUsage(dataPlan) }
    val usage = DataSize(dataUsage).getAsUnit(DataSizeUnit.GB)
    val formatter = remember { DecimalFormat("0.##") }
    Box (
        modifier = Modifier
            .height(200.dp)
            .clip(MaterialTheme.shapes.medium)
            .border(1.dp, MaterialTheme.colorScheme.primaryContainer, MaterialTheme.shapes.medium)
            .padding(8.dp),
    ) {
        Row {
            SimIcon(dataPlan.simIndex)
            Text(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(end = 4.dp),
                text = dataPlan.carrierName,
                fontFamily = carrierFont(),
                textAlign = TextAlign.End
            )
        }
        Column(
            modifier = Modifier.align(Alignment.Center).padding(bottom = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.Bottom
            ) {
                Text(
                    modifier = Modifier.alignByBaseline(),
                    text = formatter.format(usage),
                    fontFamily = doHyeonFont(),
                    fontSize = 64.sp,
                )
                Text(
                    modifier = Modifier.alignByBaseline(),
                    text = "GB",
                    fontFamily = doHyeonFont(),
                    fontSize = 36.sp,
                )
            }
            Text(
                text = stringResource(R.string.this_month),
                fontSize = 18.sp,
                fontFamily = robotoFlex(0f, 150f, 1000f)
            )
        }
        ButtonGroup(
            modifier = Modifier
                .align(Alignment.BottomCenter)
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
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onConfigure()
                }
            )
        }
    }
}

@Composable
fun DataPlanSelectorWidget(dataPlan: DataPlan, onClick: () -> Unit) {
    val haptic = LocalHapticFeedback.current
    BoxBackground(
        background = backgrounds[dataPlan.uiBackground],
        onClick = {
            haptic.performHapticFeedback(HapticFeedbackType.ContextClick)
            onClick()
        }
    ) {
        ConfiguredDataPlanContent(dataPlan)
    }
}

@Composable
private fun ConfiguredDataPlanContent(dataPlan: DataPlan) {
    val context = LocalContext.current
    val networkUsageManager: NetworkUsageManager = koinInject()
    val dataUsage by produceState(0L) { value = networkUsageManager.planUsage(dataPlan) }

    Column(Modifier.padding(8.dp)) {
        Box(Modifier.height(184.dp)) {
            Row(Modifier.fillMaxWidth()) {
                SimIcon(dataPlan.simIndex)
                Text(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(end = 4.dp),
                    text = dataPlan.carrierName,
                    fontFamily = carrierFont(),
                    textAlign = TextAlign.End
                )
            }
            val usage by remember(dataUsage) {
                derivedStateOf {
                    DataSize(dataUsage).getAsUnit(DataSizeUnit.GB)
                }
            }
            Row(
                modifier = Modifier
                    .align(Alignment.Center)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.Bottom
            ) {
                val formatter = remember { DecimalFormat("0.##") }
                Text(
                    modifier = Modifier.alignByBaseline(),
                    text = formatter.format(usage),
                    fontFamily = doHyeonFont(),
                    fontSize = 64.sp,
                )
                val data by remember(dataPlan) {
                    derivedStateOf {
                        formatter.format(
                            DataSize(dataPlan.dataMax).getAsUnit(DataSizeUnit.GB)
                        )
                    }
                }
                Text(
                    modifier = Modifier.alignByBaseline(),
                    text = "/${data}GB",
                    fontFamily = doHyeonFont(),
                    fontSize = 36.sp,
                )
            }

            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(8.dp)
                    .height(48.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp, Alignment.Bottom),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = dataPlan.resetString(context),
                    fontFamily = robotoFlex(0f, 150f, 1000f)
                )
                val lineUsage = DataSize((usage * DataSizeUnit.GB.toBits()).toLong())
                LinearWavyProgressIndicator(
                    modifier = Modifier.fillMaxWidth(),
                    progress = {
                        if (dataPlan.dataMax == 0L) 0f
                        else (lineUsage.byteValue
                            .toDouble() / dataPlan.dataMax.toDouble()).toFloat()
                            .coerceIn(0f, 1f)
                    },
                )
            }
        }
    }
}

@Composable
fun SimIcon(number: Int) {
    Box(contentAlignment = Alignment.Center) {
        Icon(
            painterResource(simIconRes(number)),
            contentDescription = stringResource(R.string.sim_card)
        )
    }
}