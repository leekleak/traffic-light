package com.leekleak.trafficlight.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.ColorFilter
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.LinearProgressIndicator
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
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
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import com.leekleak.trafficlight.R

class Widget: GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            GlanceTheme {
                MyContent()
            }
        }
    }

    @Composable
    private fun MyContent() {
        Column(
            modifier = GlanceModifier
                .background(GlanceTheme.colors.primary)
                .padding(1.dp)
                .cornerRadius(32.dp),
        ) {
            Box(
                modifier = GlanceModifier
                    .background(GlanceTheme.colors.surface)
                    .cornerRadius(32.dp)
            ) {
                Image(
                    modifier = GlanceModifier.fillMaxSize(),
                    provider = ImageProvider(R.drawable.background_2),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    colorFilter = ColorFilter.tint(GlanceTheme.colors.primaryContainer)
                )
                Column(GlanceModifier.fillMaxHeight()) {
                    Row(GlanceModifier.padding(16.dp)) {
                        SimIcon(1)
                        Text(
                            modifier = GlanceModifier.fillMaxWidth().padding(end = 4.dp),
                            text = "Odido",
                            style = TextStyle(
                                color = GlanceTheme.colors.onSurface,
                                textAlign = TextAlign.End
                            )
                        )
                    }
                    Column(
                        modifier = GlanceModifier.fillMaxWidth().fillMaxHeight().defaultWeight(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            modifier = GlanceModifier.fillMaxWidth(),
                            verticalAlignment = Alignment.Bottom,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "15",
                                style = TextStyle(
                                    color = GlanceTheme.colors.onSurface,
                                    fontSize = 64.sp
                                ),
                            )
                            Text(
                                text = "/20GB",
                                style = TextStyle(
                                    color = GlanceTheme.colors.onSurface,
                                    fontSize = 36.sp,
                                ),
                            )
                        }
                    }
                    LinearProgressIndicator(
                        modifier = GlanceModifier
                            .height(54.dp)
                            .padding(start = 16.dp, end = 16.dp, bottom = 16.dp, top = 32.dp)
                            .fillMaxWidth(),
                        color = GlanceTheme.colors.primary,
                        backgroundColor = GlanceTheme.colors.primaryContainer,
                        progress = 0.75f,
                    )
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
}