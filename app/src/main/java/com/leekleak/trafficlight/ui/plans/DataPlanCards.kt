@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package com.leekleak.trafficlight.ui.plans

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
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
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.leekleak.trafficlight.R
import com.leekleak.trafficlight.database.DataPlan
import com.leekleak.trafficlight.model.NetworkUsageManager
import com.leekleak.trafficlight.ui.theme.backgrounds
import com.leekleak.trafficlight.ui.theme.carrierFont
import com.leekleak.trafficlight.ui.theme.doHyeonFont
import com.leekleak.trafficlight.ui.theme.longGoogleSans
import com.leekleak.trafficlight.util.DataSize
import com.leekleak.trafficlight.util.DataSizeUnit
import com.leekleak.trafficlight.util.simIconRes
import org.koin.compose.koinInject
import java.text.DecimalFormat

@Composable
fun ConfiguredDataPlan(dataPlan: DataPlan, onConfigure: () -> Unit) {
    val haptic = LocalHapticFeedback.current

    BoxBackground(
        dataPlan = dataPlan,
        onClick = {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            onConfigure()
        }
    ) {
        ConfiguredDataPlanContent(dataPlan)
    }
}

@Composable
fun UnconfiguredDataPlan(dataPlan: DataPlan, showSettings: Boolean, onConfigure: () -> Unit) {
    val haptic = LocalHapticFeedback.current
    val networkUsageManager: NetworkUsageManager = koinInject()
    val fontFamilyGoogleSans = remember { longGoogleSans() }
    val fontFamilyDoHyeon = remember { doHyeonFont() }

    val dataUsage by produceState(0L) { value = networkUsageManager.planUsage(dataPlan) }
    val usage = DataSize(dataUsage).getAsUnit(DataSizeUnit.GB)
    val formatter = remember { DecimalFormat("0.##") }
    BoxBackground(
        dataPlan = dataPlan,
        onClick = {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            onConfigure()
        }
    ) {
        Box (Modifier.align(Alignment.Center)) {
            Text(
                fontFamily = fontFamilyDoHyeon,
                textAlign = TextAlign.Center,
                text = buildAnnotatedString {
                    withStyle(style = SpanStyle(fontSize = 64.sp, fontFamily = fontFamilyDoHyeon)) {
                        append(formatter.format(usage))
                    }
                    withStyle(style = SpanStyle(fontSize = 36.sp, fontFamily = fontFamilyDoHyeon)) {
                        appendLine("GB")
                    }
                    withStyle(
                        style = SpanStyle(
                            fontSize = 18.sp,
                            fontFamily = fontFamilyGoogleSans
                        )
                    ) {
                        append(stringResource(R.string.this_month))
                    }
                }
            )
        }
        if (showSettings) {
            FilledIconButton(
                modifier = Modifier.align(Alignment.BottomEnd),
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onConfigure()
                }
            ) {
                Icon(
                    painterResource(R.drawable.settings),
                    contentDescription = stringResource(R.string.configure_plan)
                )
            }
        }
    }
}

@Composable
private fun BoxBackground(
    dataPlan: DataPlan,
    onClick: () -> Unit,
    content: @Composable BoxScope.() -> Unit
) {
    val fontFamilyCarrier = remember { carrierFont() }
    val background = backgrounds[dataPlan.uiBackground]
    Box (modifier = Modifier
        .fillMaxWidth()
        .height(200.dp)
        .background(MaterialTheme.colorScheme.surface)
        .clip(MaterialTheme.shapes.medium)
        .clickable { onClick() }
        .border(2.dp, MaterialTheme.colorScheme.primaryContainer, MaterialTheme.shapes.medium)
        .padding(8.dp)
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
        Row {
            Icon(
                painterResource(simIconRes(dataPlan.simIndex)),
                contentDescription = stringResource(R.string.sim_card)
            )
            Text(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(end = 4.dp),
                text = dataPlan.carrierName,
                fontFamily = fontFamilyCarrier,
                textAlign = TextAlign.End
            )
        }
        content()
    }
}

@Composable
private fun BoxScope.ConfiguredDataPlanContent(dataPlan: DataPlan) {
    val context = LocalContext.current
    val networkUsageManager: NetworkUsageManager = koinInject()
    val fontFamilyGoogleSans = remember { longGoogleSans() }
    val fontFamilyDoHyeon = remember { doHyeonFont() }
    val dataUsage by produceState(0L) { value = networkUsageManager.planUsage(dataPlan) }
    val usage by remember(dataUsage) {
        derivedStateOf {
            DataSize(dataUsage).getAsUnit(DataSizeUnit.GB)
        }
    }

    val formatter = remember { DecimalFormat("0.##") }
    val data = remember(dataPlan) { formatter.format(DataSize(dataPlan.dataMax).getAsUnit(DataSizeUnit.GB)) }

    Box (Modifier.align(Alignment.Center)) {
        Text(
            fontFamily = fontFamilyDoHyeon,
            textAlign = TextAlign.Center,
            text = buildAnnotatedString {
                withStyle(style = SpanStyle(fontSize = 64.sp, fontFamily = fontFamilyDoHyeon)) {
                    append(formatter.format(usage))
                }
                withStyle(style = SpanStyle(fontSize = 36.sp, fontFamily = fontFamilyDoHyeon)) {
                    appendLine("/${data}GB")
                }
            }
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
            fontFamily = fontFamilyGoogleSans
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