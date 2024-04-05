package co.present.present.view

import android.content.Context
import android.graphics.drawable.StateListDrawable
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import androidx.appcompat.content.res.AppCompatResources
import android.util.StateSet
import co.present.present.R


class JoinCircleSelector(context: Context): StateListDrawable() {
    init {
        val notJoinedDrawable = AppCompatResources.getDrawable(context, R.drawable.ic_not_joined)!!
        val joinedDrawable = AppCompatResources.getDrawable(context, R.drawable.ic_joined_circle)
        val disabledNotJoinedDrawable = AppCompatResources.getDrawable(context, R.drawable.ic_joined_circle)!!.mutate().apply {
            DrawableCompat.setTint(this, ContextCompat.getColor(context, R.color.lightGray1))
        }

        addState(intArrayOf(android.R.attr.state_pressed, android.R.attr.state_enabled), joinedDrawable)
        addState(intArrayOf(android.R.attr.state_selected, android.R.attr.state_enabled), joinedDrawable)
        addState(intArrayOf(android.R.attr.state_enabled), notJoinedDrawable)
        addState(StateSet.WILD_CARD, disabledNotJoinedDrawable)
    }
}