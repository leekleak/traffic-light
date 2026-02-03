package com.leekleak.trafficlight.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.background
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
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import com.leekleak.trafficlight.MainActivity
import com.leekleak.trafficlight.R
import com.leekleak.trafficlight.database.DataPlanDao
import com.leekleak.trafficlight.database.HourlyUsageRepo
import com.leekleak.trafficlight.database.resetString
import com.leekleak.trafficlight.model.ShizukuDataManager
import com.leekleak.trafficlight.ui.theme.backgrounds
import com.leekleak.trafficlight.util.DataSize
import com.leekleak.trafficlight.util.DataSizeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.koin.mp.KoinPlatform
import timber.log.Timber
import java.text.DecimalFormat
import java.time.LocalDateTime

class Widget: GlanceAppWidget() {

    override val stateDefinition = PreferencesGlanceStateDefinition

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val koinInstance = KoinPlatform.getKoin()
        val hourlyUsageRepo: HourlyUsageRepo = koinInstance.get()
        val shizukuManager: ShizukuDataManager = koinInstance.get()
        val dataPlanDao: DataPlanDao = koinInstance.get()

        // Get all data in the suspend context
        val subscriptionInfos = shizukuManager.getSubscriptionInfos()

        val subscriberID = shizukuManager.getSubscriberID(
            subscriptionInfos.firstOrNull()?.subscriptionId ?: return
        ) ?: return

        val dataPlan = withContext(Dispatchers.IO) { dataPlanDao.get(subscriberID) } ?: return
        val usage = hourlyUsageRepo.planUsage(dataPlan)
        val usageSize = DataSize(usage.totalCellular.toDouble()).getAsUnit(DataSizeUnit.GB)
        val dataMax = DataSize(dataPlan.dataMax.toDouble()).getAsUnit(DataSizeUnit.GB)
        val formatter = DecimalFormat("0.##")

        val lastUsage = formatter.format(usageSize)
        val lastMax = formatter.format(dataMax)

        var stateChanged = false

        updateAppWidgetState(context, PreferencesGlanceStateDefinition, id) { prefs ->
            val mutable = prefs.toMutablePreferences()

            if (
                mutable[LAST_USAGE] == lastUsage &&
                mutable[LAST_MAX] == lastMax &&
                mutable[BACKGROUND] == dataPlan.uiBackground
            ) {
                Timber.i("Skipping widget refresh")
                mutable
            } else {
                stateChanged = true
                mutable.apply {
                    this[LAST_USAGE] = lastUsage
                    this[LAST_MAX] = lastMax
                    this[BACKGROUND] = dataPlan.uiBackground
                }
            }
        }

        if (!stateChanged) return

        Timber.i("Updating widget")
        provideContent {
            GlanceTheme {
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
                                SimIcon(1)
                                Text(
                                    text = LocalDateTime.now().minute.toString(),
                                    style = TextStyle(
                                        color = GlanceTheme.colors.onSurface,
                                        textAlign = TextAlign.End
                                    )
                                )
                                Text(
                                    modifier = GlanceModifier.fillMaxWidth().defaultWeight(),
                                    text = subscriptionInfos.first().carrierName.toString(),
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
                                        text = formatter.format(usageSize),
                                        style = TextStyle(
                                            color = GlanceTheme.colors.onSurface,
                                            fontSize = 64.sp
                                        ),
                                    )
                                    Text(
                                        text = "/${formatter.format(dataMax)}GB",
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
                                    text = dataPlan.resetString(context),
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
                                    progress = 0.75f,
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    @Composable
    fun SimIcon(number: Int) {
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
        val LAST_USAGE = stringPreferencesKey("last_usage")
        val LAST_MAX = stringPreferencesKey("last_max")
        val BACKGROUND = intPreferencesKey("ui_background")
    }
}