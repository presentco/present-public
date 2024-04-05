package co.present.present.feature.profile

import android.content.Context
import androidx.vectordrawable.graphics.drawable.VectorDrawableCompat
import android.text.SpannableStringBuilder
import android.text.style.ImageSpan
import co.present.present.R
import co.present.present.extensions.string

class HeartSpan(context: Context): ImageSpan(VectorDrawableCompat.create(context.resources, R.drawable.ic_heart, context.theme)
        ?.apply { setBounds(0, 0, intrinsicWidth, intrinsicHeight) }) {

    companion object {
        /**
         * Substitutes a <3 image span wherever the value of R.string.follow_heart_string is.
         * Make sure to update this value and use the string, not a string literal, in case we need
         * translation in the future!  The actual string, not the spans, are read by the screen reader.
         */
        fun putAHeartInIt(context: Context, charSequence: CharSequence): CharSequence {
                val stringBuilder = SpannableStringBuilder(charSequence)
                val heart = context.string(R.string.follow_heart_string)
                val heartIndex = stringBuilder.indexOf(heart)
                stringBuilder.setSpan(HeartSpan(context), heartIndex, heartIndex + heart.length, 0)
                return stringBuilder
        }
    }

}