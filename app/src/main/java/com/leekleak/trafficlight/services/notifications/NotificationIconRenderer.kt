package com.leekleak.trafficlight.services.notifications

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import com.leekleak.core.ui.theme.googleSans
import com.leekleak.core.util.convertFontFamilyToTypeface

class NotificationIconRenderer(context: Context) {
    private val paintValue = textPaint(context, weight = 600f, width = 60f, align = Paint.Align.CENTER)
    private val paintUnit = textPaint(context, weight = 600f, width = 80f, align = Paint.Align.CENTER)
    private val paintSeparate = textPaint(context, weight = 600f, width = 50f, align = Paint.Align.RIGHT)

    fun draw(canvas: Canvas, sizePx: Float, value: String, unit: String, color: Int) {
        val scale = sizePx / DESIGN_SIZE
        paintValue.apply {
            textSize = 72f * scale
            setColor(color)
        }
        paintUnit.apply {
            textSize = 46f * scale
            setColor(color)
        }
        canvas.drawText(value, 48f * scale, 54f * scale, paintValue)
        canvas.drawText(unit, 48f * scale, 94f * scale, paintUnit)
    }

    fun drawSeparate(canvas: Canvas, sizePx: Float, top: String, bottom: String, color: Int) {
        val scale = sizePx / DESIGN_SIZE
        paintSeparate.apply {
            textSize = 56f * scale
            setColor(color)
        }
        canvas.drawText(compact(top), DESIGN_SIZE * scale, 48f * scale, paintSeparate)
        canvas.drawText(compact(bottom), DESIGN_SIZE * scale, 96f * scale, paintSeparate)
    }

    private fun compact(text: String): String {
        return if (text.length >= 5) text.replace(" ", "") else text
    }

    private fun textPaint(context: Context, weight: Float, width: Float, align: Paint.Align): Paint {
        return Paint().apply {
            typeface = convertFontFamilyToTypeface(context, googleSans(weight = weight, width = width))
            textAlign = align
            letterSpacing = 0f
            isSubpixelText = true
            isAntiAlias = true
        }
    }

    private companion object {
        const val DESIGN_SIZE = 96f
    }
}
