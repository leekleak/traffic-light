package com.leekleak.trafficlight.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.glance.ColorFilter
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.LinearProgressIndicator
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.state.getAppWidgetState
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.background
import androidx.glance.currentState
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.ContentScale
import androidx.glance.layout.Row
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.preview.ExperimentalGlancePreviewApi
import androidx.glance.preview.Preview
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import com.leekleak.trafficlight.MainActivity
import com.leekleak.trafficlight.R
import com.leekleak.trafficlight.database.AppPreferenceRepo
import com.leekleak.trafficlight.database.DataPlan
import com.leekleak.trafficlight.database.DataPlanDao
import com.leekleak.trafficlight.model.NetworkUsageManager
import com.leekleak.trafficlight.ui.theme.backgrounds
import com.leekleak.trafficlight.util.DataSize
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import org.koin.mp.KoinPlatform
import timber.log.Timber
import java.text.DecimalFormat
import kotlin.math.max

class Widget: GlanceAppWidget() {
    override val stateDefinition = PreferencesGlanceStateDefinition

    override val sizeMode: SizeMode = SizeMode.Exact

    override val previewSizeMode = SizeMode.Responsive(
        setOf(
            DpSize(250.dp, 200.dp),
        ),
    )

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val koinInstance = KoinPlatform.getKoin()
        val networkUsageManager: NetworkUsageManager = koinInstance.get()
        val dataPlanDao: DataPlanDao = koinInstance.get()
        val appPreferenceRepo: AppPreferenceRepo = koinInstance.get()

        val state = getAppWidgetState(context, stateDefinition, id)

        val dataPlan = withContext(Dispatchers.IO) {
            state[SUBSCRIBER_ID_HASH]?.let { dataPlanDao.getByHash(it) }
        }?: return

        val sizeMetric = appPreferenceRepo.sizeMetric.first()
        val usage = dataPlan.getUsage(networkUsageManager)
        val usageSize = DataSize(usage).getAsUnit(dataPlan.mainDataSizeUnit, sizeMetric)
        val dataMax = DataSize(dataPlan.getTotalMax()).getAsUnit(dataPlan.mainDataSizeUnit, sizeMetric)
        val formatter = DecimalFormat("0.##")

        val usageString = formatter.format(usageSize)
        val quotaString = formatter.format(dataMax)
        val unitString = dataPlan.mainDataSizeUnit.name

        var stateChanged = false

        updateAppWidgetState(context, PreferencesGlanceStateDefinition, id) { prefs ->
            val mutable = prefs.toMutablePreferences()

            if (
                (mutable[LAST_USAGE] == usageString) &&
                (mutable[LAST_MAX] == quotaString) &&
                (mutable[BACKGROUND] == dataPlan.uiBackground) &&
                (mutable[LAST_UNIT] == unitString) &&
                (mutable[FORCE_REFRESH] != true)
            ) {
                Timber.i("Skipping widget refresh")
                mutable
            } else {
                stateChanged = true
                mutable.apply {
                    this[LAST_USAGE] = usageString
                    this[LAST_MAX] = quotaString
                    this[BACKGROUND] = dataPlan.uiBackground
                    this[LAST_UNIT] = unitString
                    this[FORCE_REFRESH] = false
                }
            }
        }

        if (!stateChanged) return

        Timber.i("Updating widget")
        provideContent {
            GlanceTheme {
                BoxBackground(dataPlan, currentState(SIM_NUMBER) ?: 0, currentState(CARRIER_NAME) ?: "") {
                    if (dataPlan.configured) {
                        ConfiguredWidgetContent(
                            usageString = usageString,
                            quotaString = quotaString,
                            unitString = unitString,
                            progress = usageSize / max(dataMax, 1.0),
                            dataMax = dataMax,
                            resetString = dataPlan.resetString(context)
                        )
                    } else {
                        UnconfiguredWidgetContent(
                            usageString = usageString,
                            unitString = unitString,
                        )
                    }
                }
            }
        }
    }

    override suspend fun providePreview(context: Context, widgetCategory: Int) {
        provideContent {
            GlanceTheme {
                Preview()
            }
        }
    }

    @OptIn(ExperimentalGlancePreviewApi::class)
    @Composable
    @Preview(widthDp = 400, heightDp = 200)
    private fun Preview() {
        BoxBackground(
            dataPlan = DataPlan("", "", uiBackground = 2),
            simNumber = 1,
            carrierName = "AT&T"
        ) {
            ConfiguredWidgetContent(
                usageString = "8.5",
                quotaString = "20",
                unitString = "GB",
                progress = 8.5 / 20.0,
                resetString = "Resets in 8 days",
                dataMax = 20.0
            )
        }
    }

    @Composable
    private fun ConfiguredWidgetContent(
        usageString: String,
        quotaString: String,
        unitString: String,
        progress: Double,
        resetString: String,
        dataMax: Double
    ) {
        Column(
            modifier = GlanceModifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = GlanceModifier.padding(bottom = 8.dp),
                verticalAlignment = Alignment.Bottom,
            ) {
                Text(
                    text = usageString,
                    style = TextStyle(
                        color = GlanceTheme.colors.onSurface,
                        fontSize = 64.sp,
                    ),
                )
                Text(
                    text = "${if (dataMax != 0.0) "/$quotaString" else "" }$unitString",
                    style = TextStyle(
                        color = GlanceTheme.colors.onSurface,
                        fontSize = 36.sp,
                    ),
                )
            }
        }
        Column(
            modifier = GlanceModifier.fillMaxSize().padding(16.dp),
            verticalAlignment = Alignment.Bottom,
        ) {
            Text(
                modifier = GlanceModifier.fillMaxWidth().padding(bottom = 8.dp),
                text = resetString,
                style = TextStyle(
                    color = GlanceTheme.colors.onSurface,
                    fontSize = 16.sp,
                    textAlign = TextAlign.Center
                ),
            )
            if (dataMax != 0.0) {
                LinearProgressIndicator(
                    modifier = GlanceModifier
                        .height(4.dp)
                        .fillMaxWidth(),
                    color = GlanceTheme.colors.primary,
                    backgroundColor = GlanceTheme.colors.primaryContainer,
                    progress = progress.toFloat(),
                )
            }
        }
    }

    @Composable
    private fun UnconfiguredWidgetContent(usageString: String, unitString: String) {
        Column(
            modifier = GlanceModifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = GlanceModifier.padding(bottom = 8.dp),
                verticalAlignment = Alignment.Bottom,
            ) {
                Text(
                    text = usageString,
                    style = TextStyle(
                        color = GlanceTheme.colors.onSurface,
                        fontSize = 64.sp,
                    ),
                )
                Text(
                    text = unitString,
                    style = TextStyle(
                        color = GlanceTheme.colors.onSurface,
                        fontSize = 36.sp,
                    ),
                )
            }
            Text(
                text = LocalContext.current.getString(R.string.this_month),
                style = TextStyle(
                    color = GlanceTheme.colors.onSurface,
                    fontSize = 18.sp,
                ),
            )
        }
    }

    @Composable
    private fun BoxBackground(
        dataPlan: DataPlan,
        simNumber: Int,
        carrierName: String,
        content: @Composable (() -> Unit)
    ) {
        val cornerRadius = 24.dp
        Box(
            modifier = GlanceModifier
                .background(GlanceTheme.colors.primary)
                .padding(2.dp)
                .cornerRadius(cornerRadius)
                .clickable(actionStartActivity<MainActivity>()),
        ) {
            Box(
                modifier = GlanceModifier
                    .background(GlanceTheme.colors.surface)
                    .cornerRadius(cornerRadius)
            ) {
                backgrounds.getOrNull(dataPlan.uiBackground)?.let { background ->
                    Image(
                        modifier = GlanceModifier.fillMaxSize(),
                        provider = ImageProvider(background),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        colorFilter = ColorFilter.tint(GlanceTheme.colors.primaryContainer)
                    )
                }
                Row(GlanceModifier.padding(12.dp).fillMaxWidth()) {
                    SimIcon(simNumber)
                    Text(
                        modifier = GlanceModifier.fillMaxWidth().defaultWeight(),
                        text = carrierName,
                        style = TextStyle(
                            color = GlanceTheme.colors.onSurface,
                            textAlign = TextAlign.End
                        )
                    )
                }
                content()
            }
        }
    }

    @Composable
    private fun SimIcon(number: Int) {
        Box(contentAlignment = Alignment.Center) {
            Image(
                provider = ImageProvider(R.drawable.sim_card),
                contentDescription = null,
                colorFilter = ColorFilter.tint(GlanceTheme.colors.onSurface)
            )
            Text(
                modifier = GlanceModifier.padding(top = 2.dp, start = 0.5.dp),
                text = number.toString(),
                style = TextStyle(
                    color = GlanceTheme.colors.onSurface,
                    fontSize = 12.sp,
                ),
            )
        }
    }
    companion object {
        val SUBSCRIBER_ID_HASH = stringPreferencesKey("sub_id")
        val CARRIER_NAME = stringPreferencesKey("carrier")
        val SIM_NUMBER = intPreferencesKey("sim_number")
        val LAST_USAGE = stringPreferencesKey("last_usage")
        val LAST_MAX = stringPreferencesKey("last_max")
        val LAST_UNIT = stringPreferencesKey("last_unit")
        val BACKGROUND = intPreferencesKey("ui_background")
        val FORCE_REFRESH = booleanPreferencesKey("force_refresh")
    }
}