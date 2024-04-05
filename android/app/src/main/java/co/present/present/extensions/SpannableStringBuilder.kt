package co.present.present.extensions

import android.text.Spannable

fun Spannable.setSpan(span: Any): Spannable {
    setSpan(span, 0, length, 0)
    return this
}

inline fun <reified T> Spannable.getSpans(): Array<out T> {
    return getSpans(0, length, T::class.java)
}

