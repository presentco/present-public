package co.present.present.feature.detail.info

import androidx.recyclerview.widget.ItemTouchHelper
import co.present.present.R
import co.present.present.extensions.loadCircularImage
import co.present.present.extensions.string
import co.present.present.feature.common.Payload
import co.present.present.model.CurrentUser
import co.present.present.model.User
import com.xwray.groupie.kotlinandroidextensions.Item
import com.xwray.groupie.kotlinandroidextensions.ViewHolder
import kotlinx.android.synthetic.main.item_member_request.*

open class UserRequestItem(val user: User, val currentUser: CurrentUser, val isApproved: Boolean, val listener: OnUserApproveListener) : Item() {
    override fun getLayout() = R.layout.item_member_request

    override fun bind(holder: ViewHolder, position: Int, payloads: MutableList<Any>) {
        if (payloads.contains(ApprovePayload)) {
            holder.bindApprove()
        } else if (payloads.contains(Payload.DontUpdate)) {
            return
        } else {
            bind(holder, position)
        }
    }

    override fun bind(viewHolder: ViewHolder, position: Int) {
        with(viewHolder) {
            name.text = user.name
            photo.loadCircularImage(user.photo)
            bindApprove()
        }
    }

    private fun ViewHolder.bindApprove() {
        if (isApproved) {
            approveButton.isEnabled = false
        } else {
            approveButton.setOnClickListener { listener.onApproveClicked(this@UserRequestItem, user, currentUser) }
        }

        approveButton.isSelected = isApproved
        approveButton.text = root.context.string(if (isApproved) R.string.approved else R.string.approve)
    }

    override fun isSameAs(other: com.xwray.groupie.Item<*>?): Boolean {
        return other is UserRequestItem && other.user == user
    }

    override fun getSwipeDirs() = ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT

    override fun getChangePayload(newItem: com.xwray.groupie.Item<*>): Any {
        if (newItem is UserRequestItem && newItem.isApproved != isApproved) {
            return ApprovePayload
        } else {
            return Payload.DontUpdate
        }
    }

    interface OnUserApproveListener {
        fun onApproveClicked(item: com.xwray.groupie.Item<*>, user: User, currentUser: CurrentUser)
    }

    companion object {
        const val ApprovePayload = "approvePayload"
    }
}