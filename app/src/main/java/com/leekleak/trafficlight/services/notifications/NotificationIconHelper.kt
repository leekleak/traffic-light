package com.leekleak.trafficlight.services.notifications

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import androidx.collection.LruCache
import androidx.compose.ui.unit.Density
import androidx.core.graphics.createBitmap
import androidx.core.graphics.drawable.IconCompat
import com.leekleak.trafficlight.R
import com.leekleak.trafficlight.ui.theme.notificationIconFont
import com.leekleak.trafficlight.util.convertFontFamilyToTypeface
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class NotificationIconHelper(private val context: Context) {
    private val paint by lazy {
        Paint().apply {
            color = context.getColor(R.color.white)
            typeface = convertFontFamilyToTypeface(context, notificationIconFont())
            textAlign = Paint.Align.CENTER
        }
    }
    private var cachedIcons = LruCache<String, IconCompat>(50)
    private var bitmap: Bitmap? = null
    private val bitmapMutex = Mutex()
    suspend fun createIcon(speed: String, unit: String): IconCompat {
        val density = Density(context)
        val multiplier = 24 * density.density / 96f * 1f
        val height = (96 * multiplier).toInt()

        val iconTag = "$speed$unit$height"

        cachedIcons[iconTag]?.let { return it }

        bitmapMutex.withLock {
            if (bitmap == null || bitmap!!.height != height) {
                bitmap = createBitmap(height, height, Bitmap.Config.ALPHA_8)
            } else {
                bitmap?.eraseColor(Color.TRANSPARENT)
            }

            val canvas = Canvas(bitmap!!)

            paint.apply {
                textSize = 72f * multiplier
                letterSpacing = -0.05f * multiplier
            }
            canvas.drawText(speed, 48f * multiplier, 54f * multiplier, paint)

            paint.apply {
                textSize = 46f * multiplier
                letterSpacing = 0f * multiplier
            }
            canvas.drawText(unit, 48f * multiplier, 94f * multiplier, paint)

            /**
             * Don't cache numbers with many digits as they appear much more often and are unlikely
             * to be worth the cost of creating a new bitmap
             *
             * Mostly there to avoid re-rendering common values like 0KB/s, <1KB/s or other small values
             * caused by many background processes.
             *
             * Making caching more aggressive is probably a bad idea as duplicating bitmaps is quite
             * expensive and not worth it if the value appears once a day.
             *
             * Generally one would worry about bitmap corruption, but as the icon never updates more than
             * once every 900ms, that's incredibly unlikely and duplicating is not worth the performance/
             * efficiency cost.
             */
            if (speed.count(Char::isDigit) == 1) {
                cachedIcons.put(
                    iconTag,
                    IconCompat.createWithBitmap(bitmap!!.copy(Bitmap.Config.ALPHA_8, false)),
                )
                return cachedIcons[iconTag]!!
            } else {
                return IconCompat.createWithBitmap(bitmap!!)
            }
        }
    }
}