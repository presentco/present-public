package co.present.present.view

import android.net.Uri
import androidx.core.content.ContextCompat
import android.text.Spannable
import android.text.TextPaint
import android.text.style.ClickableSpan
import android.text.style.URLSpan
import android.view.View
import android.widget.TextView
import co.present.present.R


interface OnLinkClickedListener {
    fun onLinkClick(uri: Uri)
}

class TouchableSpan(private val onLinkClickedListener: OnLinkClickedListener, val uri: Uri, private val mPressedTextColor: Int) : ClickableSpan() {

    private var pressed: Boolean = false

    override fun onClick(p0: View?) {
        onLinkClickedListener.onLinkClick(uri)
    }

    fun setPressed(isSelected: Boolean) {
        pressed = isSelected
    }

    override fun updateDrawState(ds: TextPaint) {
        super.updateDrawState(ds)
        ds.color = if (pressed) mPressedTextColor else ds.linkColor
    }
}

fun TextView.replaceSpans(onLinkClickedListener: OnLinkClickedListener) {
    if (text is Spannable) {
        val spannable = text as Spannable
        val spans = spannable.getSpans(0, spannable.length, URLSpan::class.java)
        spans.forEach { span ->
            val start = spannable.getSpanStart(span)
            val end = spannable.getSpanEnd(span)
            val flags = spannable.getSpanFlags(span)
            spannable.removeSpan(span)

            val myUrlSpan = TouchableSpan(onLinkClickedListener, Uri.parse(span.url), ContextCompat.getColor(context, R.color.pink))
            spannable.setSpan(myUrlSpan, start, end, flags)
        }
    }
}