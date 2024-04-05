package co.present.present.view

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.text.SpannableStringBuilder
import android.text.style.ReplacementSpan
import co.present.present.R
import co.present.present.extensions.string


class BadgeSpan(val radius: Float, val textSize: Float) : ReplacementSpan() {
    private val textColor = Color.WHITE
    private val backgroundColor = Color.RED
    private val padding = 15

    override fun getSize(paint: Paint, text: CharSequence, start: Int, end: Int, fm: Paint.FontMetricsInt?): Int {
        return (radius + paint.measureText(text.subSequence(start, end).toString())).toInt()
    }

    override fun draw(canvas: Canvas, text: CharSequence, start: Int, end: Int, x: Float, top: Int, y: Int, bottom: Int, paint: Paint) {
        val width = paint.measureText(text.subSequence(start, end).toString())
        val verticalMiddle = (top + bottom) / 2f
        val rect = RectF(x - padding, verticalMiddle + radius / 2f, x + width + padding.toFloat(), verticalMiddle - radius / 2f)
        paint.color = backgroundColor
        canvas.drawRoundRect(rect, radius, radius, paint)
        paint.color = textColor
        paint.textSize = textSize
        canvas.drawText(text, start, end, x + 2, y.toFloat() - 3, paint)
    }
}

fun Context.badge(resId: Int, badgeCount: Int): CharSequence {
    return SpannableStringBuilder(string(resId)).append("   $badgeCount").apply {
        val spTextSize = 12f
        val textSize = spTextSize * resources.displayMetrics.scaledDensity
        setSpan(BadgeSpan(resources.getDimension(R.dimen.number_badge_radius), textSize), length - badgeCount.toString().length, length, 0)
    }
}