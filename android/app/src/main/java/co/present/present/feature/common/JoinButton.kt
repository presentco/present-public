package co.present.present.feature.common

import android.content.Context
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatButton
import co.present.present.R
import co.present.present.extensions.hide
import co.present.present.extensions.show
import co.present.present.extensions.string
import co.present.present.feature.common.item.OnCircleJoinClickListener
import co.present.present.model.Circle
import co.present.present.model.CurrentUser
import com.xwray.groupie.kotlinandroidextensions.Item
import present.proto.GroupMembershipState

class JoinButton(context: Context, attributeSet: AttributeSet) : AppCompatButton(context, attributeSet) {

    fun bindJoin(circle: Circle, currentUser: CurrentUser?, item: Item, listener: OnCircleJoinClickListener? = null) {
        if (listener == null) {
            hide()
        } else {
            show()
            isSelected = isJoinButtonSelected(circle)
            text = context.string(getJoinButtonText(circle))
            setOnClickListener {
                listener.onCircleJoinClicked(circle, currentUser)
            }
        }
    }

    private fun getJoinButtonText(circle: Circle): Int {
        return when (circle.getGroupMembershipState()) {
            GroupMembershipState.REQUESTED -> R.string.requested
            GroupMembershipState.ACTIVE -> R.string.joined
            else -> R.string.join
        }
    }

    private fun isJoinButtonSelected(circle: Circle): Boolean {
        return when (circle.getGroupMembershipState()) {
            GroupMembershipState.ACTIVE, GroupMembershipState.REQUESTED -> true
            else -> false
        }
    }

}