package co.present.present.view

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.text.Layout
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.BulletSpan
import android.text.style.ForegroundColorSpan
import androidx.core.content.ContextCompat
import co.present.present.R
import co.present.present.extensions.string


class BadgeBulletSpan(context: Context) : BulletSpan(context.resources.getDimensionPixelSize(R.dimen.spacing_small)) {

    private val bulletRadius = context.resources.getDimension(R.dimen.badge_diameter) / 2f
    private val bulletColor = ContextCompat.getColor(context, R.color.presentPurple)

    override fun drawLeadingMargin(c: Canvas, paint: Paint, x: Int, dir: Int, top: Int, baseline: Int, bottom: Int, text: CharSequence?, start: Int, end: Int, first: Boolean, l: Layout?) {
        if ((text as Spanned).getSpanStart(this) == start) {
            val style = paint.style
            val oldColor = paint.color

            paint.color = bulletColor
            paint.style = Paint.Style.FILL

            c.drawCircle(x + dir * bulletRadius, (top + bottom) / 2.0f, bulletRadius, paint)

            paint.color = oldColor
            paint.style = style
        }
    }
}

fun Context.badge(resId: Int): CharSequence {
    return SpannableStringBuilder(string(resId)).apply {
        setSpan(BadgeBulletSpan(this@badge), 0, length, 0)
        setSpan(ForegroundColorSpan(ContextCompat.getColor(this@badge, R.color.presentPurple)),
                0, length, 0)
    }
}