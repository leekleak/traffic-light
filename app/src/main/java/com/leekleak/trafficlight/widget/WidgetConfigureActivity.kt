package com.leekleak.trafficlight.widget

import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.state.updateAppWidgetState
import com.leekleak.trafficlight.R
import com.leekleak.trafficlight.database.DataPlanDao
import com.leekleak.trafficlight.model.ShizukuDataManager
import com.leekleak.trafficlight.ui.overview.DataPlanSelectorWidget
import com.leekleak.trafficlight.ui.theme.Theme
import com.leekleak.trafficlight.util.categoryTitle
import com.leekleak.trafficlight.util.categoryTitleSmall
import com.leekleak.trafficlight.widget.Widget.Companion.CARRIER_NAME
import com.leekleak.trafficlight.widget.Widget.Companion.SIM_NUMBER
import com.leekleak.trafficlight.widget.Widget.Companion.SUBSCRIBER_ID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.runBlocking
import org.koin.compose.koinInject

class WidgetConfigureActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()

        val appWidgetId = intent?.extras?.getInt(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID
        ) ?: AppWidgetManager.INVALID_APPWIDGET_ID
        val resultValue = Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
        setResult(RESULT_CANCELED, resultValue)

        setContent {
            Theme {
                Scaffold { paddingValues ->
                    Content(appWidgetId, resultValue, paddingValues)
                }
            }
        }
    }

    @Composable
    private fun Content(appWidgetId: Int, resultValue: Intent, paddingValues: PaddingValues) {
        val dataPlanDao: DataPlanDao = koinInject()
        val shizukuManager: ShizukuDataManager = koinInject()
        val context = LocalContext.current

        val subscriptionInfos = shizukuManager.getSubscriptionInfos()

        val pairs = subscriptionInfos.map { info ->
            shizukuManager.getSubscriberID(info.subscriptionId)?.let {
                Pair(
                    info,
                    it
                )
            }
        }

        LazyColumn (
            modifier = Modifier.padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = paddingValues
        ){
            categoryTitle(R.string.add_widget)
            categoryTitleSmall(R.string.configured_plans)
            items(pairs.filterNotNull(), {it.second}) {
                val dataPlan by remember {
                    flow { emit(dataPlanDao.get(it.second)) }.flowOn(Dispatchers.IO)
                }.collectAsState(null)
                dataPlan?.let { plan ->
                    DataPlanSelectorWidget(it.first, plan) {
                        val subscriberId = it.second
                        val glanceManager = GlanceAppWidgetManager(this@WidgetConfigureActivity)
                        val glanceId = glanceManager.getGlanceIdBy(appWidgetId)

                        runBlocking {
                            updateAppWidgetState(this@WidgetConfigureActivity, glanceId) { prefs ->
                                prefs[SUBSCRIBER_ID] = subscriberId
                                prefs[SIM_NUMBER] = it.first.simSlotIndex + 1
                                prefs[CARRIER_NAME] = it.first.carrierName.toString()
                            }
                        }

                        startAlarmManager(context)

                        setResult(RESULT_OK, resultValue)
                        finish()

                        runBlocking {
                            Widget().update(this@WidgetConfigureActivity, glanceId)
                        }
                    }
                }
            }
        }
    }
}