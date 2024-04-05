package co.present.present.view

import android.content.Context
import android.graphics.drawable.StateListDrawable
import androidx.appcompat.content.res.AppCompatResources
import android.util.StateSet
import co.present.present.R


class JoinUserSelector(context: Context): StateListDrawable() {
    init {
        addState(intArrayOf(android.R.attr.state_pressed), AppCompatResources.getDrawable(context, R.drawable.ic_joined_user))
        addState(intArrayOf(android.R.attr.state_selected), AppCompatResources.getDrawable(context, R.drawable.ic_joined_user))
        addState(StateSet.WILD_CARD, AppCompatResources.getDrawable(context, R.drawable.ic_not_joined))
    }
}