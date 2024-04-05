package co.present.present.extensions

import android.graphics.drawable.Drawable
import androidx.annotation.DrawableRes
import androidx.vectordrawable.graphics.drawable.VectorDrawableCompat
import android.text.Editable
import android.text.TextWatcher
import android.widget.TextView

fun TextView.afterTextChanged(func: (Editable) -> Unit) {
    addTextChangedListener(object: TextWatcher {
        override fun afterTextChanged(editable: Editable) {
            func.invoke(editable)
        }

        override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}

        override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}

    })
}

fun TextView.drawableLeft(@DrawableRes drawableRes: Int) {
    val drawable = VectorDrawableCompat.create(context.resources, drawableRes, context.theme)!!
    drawableLeft(drawable)
}

fun TextView.drawableLeft(drawable: Drawable) {
    setCompoundDrawablesWithIntrinsicBounds(drawable, compoundDrawables[1], compoundDrawables[2], compoundDrawables[3])
}

fun TextView.updateCompoundDrawablesRelative(@DrawableRes start: Int? = null,
                                             @DrawableRes top: Int? = null,
                                             @DrawableRes end: Int? = null,
                                             @DrawableRes bottom: Int? = null) {
    fun newOrExisting(@DrawableRes resId: Int?, index: Int): Drawable? {
        return if (resId != null) VectorDrawableCompat.create(context.resources, resId, context.theme)!!
        else compoundDrawablesRelative[index]
    }
    setCompoundDrawablesRelativeWithIntrinsicBounds(
            newOrExisting(start, 0), newOrExisting(top, 1), newOrExisting(end, 2),
            newOrExisting(bottom, 3))
}

fun TextView.updateCompoundDrawablesRelative(start: Drawable? = compoundDrawablesRelative[0],
                                             top: Drawable? = compoundDrawablesRelative[1],
                                             end: Drawable? = compoundDrawablesRelative[2],
                                             bottom: Drawable? = compoundDrawablesRelative[3]) {
    setCompoundDrawablesRelativeWithIntrinsicBounds(start, top, end, bottom)
}