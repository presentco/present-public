package co.present.present.extensions

import android.content.Context
import android.view.View
import android.view.inputmethod.InputMethodManager

private fun Context.hideKeyboard(view: View) {
    inputMethodManager.hideSoftInputFromWindow(view.windowToken, 0)
}

fun View.hideKeyboard() {
    context.hideKeyboard(this)
}

fun View.showKeyboard() {
    this.requestFocus()
    context.inputMethodManager.showSoftInput(this, InputMethodManager.SHOW_IMPLICIT)
}
