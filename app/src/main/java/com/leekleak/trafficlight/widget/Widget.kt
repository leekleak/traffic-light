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
import androidx.glance.layout.fillMaxHeight
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
import com.leekleak.trafficlight.database.DataPlan
import com.leekleak.trafficlight.database.DataPlanDao
import com.leekleak.trafficlight.database.HourlyUsageRepo
import com.leekleak.trafficlight.database.resetString
import com.leekleak.trafficlight.ui.theme.backgrounds
import com.leekleak.trafficlight.util.DataSize
import com.leekleak.trafficlight.util.DataSizeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.koin.mp.KoinPlatform
import timber.log.Timber
import java.text.DecimalFormat

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
        val hourlyUsageRepo: HourlyUsageRepo = koinInstance.get()
        val dataPlanDao: DataPlanDao = koinInstance.get()

        val state = getAppWidgetState(context, stateDefinition, id)
        val subscriberID = state[SUBSCRIBER_ID] ?: return

        val dataPlan = withContext(Dispatchers.IO) { dataPlanDao.get(subscriberID) }!!
        val usage = hourlyUsageRepo.planUsage(dataPlan)
        val usageSize = DataSize(usage.totalCellular.toDouble()).getAsUnit(DataSizeUnit.GB)
        val dataMax = DataSize(dataPlan.dataMax.toDouble()).getAsUnit(DataSizeUnit.GB)
        val formatter = DecimalFormat("0.##")

        val usageString = formatter.format(usageSize)
        val quotaString = formatter.format(dataMax)

        var stateChanged = false

        updateAppWidgetState(context, PreferencesGlanceStateDefinition, id) { prefs ->
            val mutable = prefs.toMutablePreferences()

            if (
                mutable[LAST_USAGE] == usageString &&
                mutable[LAST_MAX] == quotaString &&
                mutable[BACKGROUND] == dataPlan.uiBackground &&
                mutable[FORCE_REFRESH] != true
            ) {
                Timber.i("Skipping widget refresh")
                mutable
            } else {
                stateChanged = true
                mutable.apply {
                    this[LAST_USAGE] = usageString
                    this[LAST_MAX] = quotaString
                    this[BACKGROUND] = dataPlan.uiBackground
                    this[FORCE_REFRESH] = false
                }
            }
        }

        if (!stateChanged) return

        Timber.i("Updating widget")
        provideContent {
            GlanceTheme {
                WidgetContent(
                    dataPlan = dataPlan,
                    simNumber = currentState(SIM_NUMBER) ?: 0,
                    carrierName = currentState(CARRIER_NAME) ?: "",
                    usageString = usageString,
                    quotaString = quotaString,
                    progress = usageSize/dataMax,
                    resetString = dataPlan.resetString(context)
                )
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
        WidgetContent(
            dataPlan = DataPlan("", uiBackground = 2),
            simNumber = 1,
            carrierName = "AT&T",
            usageString = "8.5",
            quotaString = "20",
            progress = 8.5 / 20.0,
            resetString = "Resets in 8 days"
        )
    }

    @Composable
    private fun WidgetContent(
        dataPlan: DataPlan,
        simNumber: Int,
        carrierName: String,
        usageString: String,
        quotaString: String,
        progress: Double,
        resetString: String
    ) {
        val cornerRadius = 24.dp
        Column(
            modifier = GlanceModifier
                .background(GlanceTheme.colors.primary)
                .padding(1.dp)
                .cornerRadius(cornerRadius)
                .clickable(actionStartActivity<MainActivity>()),
        ) {
            Box(
                modifier = GlanceModifier
                    .background(GlanceTheme.colors.surface)
                    .cornerRadius(cornerRadius)
            ) {
                backgrounds[dataPlan.uiBackground]?.let { background ->
                    Image(
                        modifier = GlanceModifier.fillMaxSize(),
                        provider = ImageProvider(background),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        colorFilter = ColorFilter.tint(GlanceTheme.colors.primaryContainer)
                    )
                }
                Column(GlanceModifier.fillMaxHeight()) {
                    Row(GlanceModifier.padding(16.dp).fillMaxWidth()) {
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
                    Column(
                        modifier = GlanceModifier.fillMaxSize().defaultWeight(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            modifier = GlanceModifier.fillMaxWidth(),
                            verticalAlignment = Alignment.Bottom,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = usageString,
                                style = TextStyle(
                                    color = GlanceTheme.colors.onSurface,
                                    fontSize = 64.sp
                                ),
                            )
                            Text(
                                text = "/${quotaString}GB",
                                style = TextStyle(
                                    color = GlanceTheme.colors.onSurface,
                                    fontSize = 36.sp,
                                ),
                            )
                        }
                    }
                    Column(GlanceModifier.padding(16.dp)) {
                        Text(
                            modifier = GlanceModifier.fillMaxWidth().padding(bottom = 8.dp),
                            text = resetString,
                            style = TextStyle(
                                color = GlanceTheme.colors.onSurface,
                                fontSize = 16.sp,
                                textAlign = TextAlign.Center
                            ),
                        )
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
        val SUBSCRIBER_ID = stringPreferencesKey("sub_id")
        val CARRIER_NAME = stringPreferencesKey("carrier")
        val SIM_NUMBER = intPreferencesKey("sim_number")
        val LAST_USAGE = stringPreferencesKey("last_usage")
        val LAST_MAX = stringPreferencesKey("last_max")
        val BACKGROUND = intPreferencesKey("ui_background")
        val FORCE_REFRESH = booleanPreferencesKey("force_refresh")
    }
}