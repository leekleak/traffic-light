package com.leekleak.trafficlight.services.notifications

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import androidx.compose.ui.unit.Density
import androidx.core.graphics.createBitmap
import androidx.core.graphics.drawable.IconCompat
import com.leekleak.trafficlight.R
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class NotificationIconHelper(private val context: Context) {
    private val renderer = NotificationIconRenderer(context)
    private val sizePx = 24 * Density(context).density
    private val height = sizePx.toInt()
    private var bitmap: Bitmap = createBitmap(height, height, Bitmap.Config.ARGB_8888)
    private val bitmapMutex = Mutex()

    suspend fun createIcon(speed: String, unit: String): IconCompat {
        bitmapMutex.withLock {
            val bitmap = prepareBitmap()
            renderer.draw(Canvas(bitmap), sizePx, speed, unit, context.getColor(R.color.white))
            return IconCompat.createWithBitmap(bitmap.copy(Bitmap.Config.ARGB_8888, false))
        }
    }

    suspend fun createIconSeparate(speed1: String, speed2: String): IconCompat {
        bitmapMutex.withLock {
            val bitmap = prepareBitmap()
            renderer.drawSeparate(
                Canvas(bitmap),
                sizePx,
                speed1,
                speed2,
                context.getColor(R.color.white),
            )
            return IconCompat.createWithBitmap(bitmap.copy(Bitmap.Config.ARGB_8888, false))
        }
    }

    private fun prepareBitmap(): Bitmap {
        if (bitmap.height != height) {
            bitmap = createBitmap(height, height, Bitmap.Config.ARGB_8888)
        } else {
            bitmap.eraseColor(Color.TRANSPARENT)
        }
        return bitmap
    }
}
