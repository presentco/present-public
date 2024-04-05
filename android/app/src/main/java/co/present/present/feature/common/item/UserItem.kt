package co.present.present.feature.common.item

import co.present.present.R
import co.present.present.extensions.loadCircularImage
import co.present.present.feature.common.OnUserAddFriendListener
import co.present.present.feature.common.Payload
import co.present.present.model.CurrentUser
import co.present.present.model.FriendshipState
import co.present.present.model.User
import com.xwray.groupie.kotlinandroidextensions.Item
import com.xwray.groupie.kotlinandroidextensions.ViewHolder
import kotlinx.android.synthetic.main.item_user.*

/**
 * A user item with an actionable join button showing if the current user has joined
 */
open class UserItem(val user: User, val currentUser: CurrentUser, val friendshipState: FriendshipState, val listener: OnUserAddFriendListener) : Item() {
    override fun getLayout() = R.layout.item_user

    override fun bind(holder: ViewHolder, position: Int, payloads: MutableList<Any>) {
        if (payloads.contains(Payload.Joined)) {
            holder.bindJoin()
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
            bindJoin()
        }
    }

    private fun ViewHolder.bindJoin() {
        joinButton.bind(user, currentUser, friendshipState, this@UserItem, listener)
    }

    override fun isSameAs(other: com.xwray.groupie.Item<*>?): Boolean {
        return other is UserItem && other.user == user
    }

    override fun getChangePayload(newItem: com.xwray.groupie.Item<*>): Any {
        if (newItem is UserItem && newItem.friendshipState != friendshipState) {
            return Payload.Joined
        } else {
            return Payload.DontUpdate
        }
    }
}