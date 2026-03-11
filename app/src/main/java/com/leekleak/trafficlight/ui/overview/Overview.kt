package com.leekleak.trafficlight.ui.overview

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import com.leekleak.trafficlight.R
import com.leekleak.trafficlight.charts.BarGraph
import com.leekleak.trafficlight.charts.model.BarData
import com.leekleak.trafficlight.database.DataPlanDao
import com.leekleak.trafficlight.database.HistoricalDataDao
import com.leekleak.trafficlight.database.HourlyUsageRepo
import com.leekleak.trafficlight.ui.navigation.PlanConfig
import com.leekleak.trafficlight.ui.theme.card
import com.leekleak.trafficlight.ui.theme.jetbrainsMono
import com.leekleak.trafficlight.util.DataSize
import com.leekleak.trafficlight.util.categoryTitle
import org.koin.compose.koinInject

@Composable
fun Overview(
    paddingValues: PaddingValues,
    backStack: NavBackStack<NavKey>
) {
    val hourlyUsageRepo: HourlyUsageRepo = koinInject()
    val dataPlanDao: DataPlanDao = koinInject()

    val weeklyUsage by hourlyUsageRepo.weekUsage().collectAsState(listOf())
    val activePlans by remember { dataPlanDao.getActiveFlow() }.collectAsState(listOf())

    val columnState = rememberLazyListState()
    LazyColumn(
        modifier = Modifier
            .background(MaterialTheme.colorScheme.surface)
            .fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = paddingValues,
        state = columnState
    ) {
        categoryTitle { stringResource(R.string.today) }
        item {
            Row{
                PredictionCard()
            }
        }
        categoryTitle { stringResource(R.string.data_plans) }

        items(activePlans, {it.subscriberID}) {
            if (it.dataMax != 0L) {
                ConfiguredDataPlan(it) {
                    backStack.add(PlanConfig(it.subscriberID))
                }
            } else {
                UnconfiguredDataPlan(it) {
                    backStack.add(PlanConfig(it.subscriberID))
                }
            }
        }

        if (weeklyUsage.isNotEmpty()) {
            overviewTab(
                label = R.string.this_week,
                data = weeklyUsage,
                finalGridPoint = "",
                centerLabels = true
            )
        }
    }
}

@Composable
private fun RowScope.PredictionCard() {
    val hourlyUsageRepo: HourlyUsageRepo = koinInject()
    val historicalDataDao: HistoricalDataDao = koinInject()
    Column(
        modifier = Modifier
            .card()
            .padding(16.dp)
            .weight(1f),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        val prediction by remember { hourlyUsageRepo.predictUsage() }.collectAsState(0.0)
        val size by remember { historicalDataDao.getAllFlow() }.collectAsState(listOf())
        val string = DataSize(prediction).toStringParts()

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                painterResource(R.drawable.query_stats),
                contentDescription = null
            )
            Text(stringResource(R.string.prediction) + size.size)
        }
        Row {
            Text(
                modifier = Modifier.alignByBaseline(),
                text = string[0] + "." + string[1],
                fontFamily = jetbrainsMono(),
                fontSize = 32.sp
            )
            Text(
                modifier = Modifier.alignByBaseline(),
                text = string[2],
                fontFamily = jetbrainsMono(),
                fontSize = 20.sp
            )
        }
    }
}

fun LazyListScope.overviewTab(
    label: Int,
    data: List<BarData>,
    finalGridPoint: String = "24",
    centerLabels: Boolean = false
) {
    categoryTitle { stringResource(label) }
    item {
        Box(
            modifier = Modifier
                .card()
                .padding(6.dp)
        ) {
            BarGraph(
                data = data,
                finalGridPoint = finalGridPoint,
                centerLabels = centerLabels
            )
        }
    }
}
