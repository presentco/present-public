package co.present.present.extensions

import android.view.View

fun View.hide() {
    visibility = View.GONE
}

fun View.show() {
    visibility = View.VISIBLE
}

fun View.setVisible(boolean: Boolean) {
    if (boolean) show() else hide()
}

fun List<View>.setOnClickListener(listener: (View) -> Unit) {
    forEach { it.setOnClickListener(listener) }
}