@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package com.leekleak.trafficlight.ui.overview

import android.telephony.SubscriptionInfo
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
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
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
import androidx.compose.ui.res.painterResource
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

@Composable
fun UnconfiguredDataPlan(info: SubscriptionInfo, onConfigure: () -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box(modifier = Modifier
        .fillMaxWidth()
        .clip(MaterialTheme.shapes.medium)
        .clickable(onClick = { expanded = !expanded })
        .border(1.dp, MaterialTheme.colorScheme.primary, MaterialTheme.shapes.medium),
    ) {
        Image(
            modifier = Modifier
                .matchParentSize()
                .graphicsLayer(scaleX = 1.2f, scaleY = 1.2f),
            painter = painterResource(R.drawable.background_4),
            contentDescription = null,
            contentScale = ContentScale.FillWidth,
            colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.primaryContainer)
        )
        Column (Modifier.padding(8.dp)) {
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
            Row (
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 32.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.Bottom
            ) {
                Text(
                    text = "12.1",
                    fontFamily = bigFont(),
                    fontSize = 64.sp,
                )
                Text(
                    text = "/15GB",
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
                Text(
                    text = "Resets in 6 days",
                    fontFamily = robotoFlex(0f,150f,1000f)
                )
                LinearWavyProgressIndicator(
                    modifier = Modifier.fillMaxWidth(),
                    progress = { 12.1f/15f },
                )
            }

            AnimatedVisibility(expanded) {
                ButtonGroup(
                    modifier = Modifier
                        .padding(horizontal = 8.dp)
                        .height(48.dp),
                    overflowIndicator = {}
                ) {
                    clickableItem(
                        label = "",
                        icon = {Icon(
                            painterResource(R.drawable.wifi),
                            contentDescription = stringResource(R.string.wifi)
                        )},
                        onClick = {}
                    )
                    clickableItem(
                        label = "",
                        icon = {Icon(
                            painterResource(R.drawable.cellular),
                            contentDescription = stringResource(R.string.cellular)
                        )},
                        onClick = {}
                    )
                    customItem(
                        buttonGroupContent = {
                            FilledIconButton(onClick = onConfigure, shape = MaterialTheme.shapes.small) {
                                Icon(
                                    painterResource(R.drawable.cellular),
                                    contentDescription = stringResource(R.string.cellular)
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
fun robotoFlex(slant: Float, width: Float, weight: Float): FontFamily? {
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