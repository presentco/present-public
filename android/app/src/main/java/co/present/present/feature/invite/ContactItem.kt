package co.present.present.feature.invite

import android.net.Uri
import co.present.present.R
import co.present.present.extensions.hide
import co.present.present.extensions.loadCircularImageFromUri
import co.present.present.extensions.show
import co.present.present.feature.common.Payload
import co.present.present.model.CurrentUser
import co.present.present.model.FriendshipState
import com.xwray.groupie.kotlinandroidextensions.Item
import com.xwray.groupie.kotlinandroidextensions.ViewHolder
import kotlinx.android.synthetic.main.item_contact.*

/**
 * Like a user item, with an actionable join button showing if the current user has joined
 */
open class ContactItem(val contact: Contact, val currentUser: CurrentUser, val friendshipState: FriendshipState, val listener: OnContactAddFriendListener) : Item() {
    override fun getLayout() = R.layout.item_contact

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
            name.text = contact.displayName
            if (contact.thumbUri != null) {
                photo.loadCircularImageFromUri(Uri.parse(contact.thumbUri))
                initial.hide()
            } else {
                photo.setImageResource(R.drawable.circle_purple)
                initial.show()
                initial.text = contact.getInitials(itemView.context)
            }
            bindJoin()
        }
    }

    private fun ViewHolder.bindJoin() {
        joinButton.bind(contact, friendshipState, this@ContactItem, listener)
    }

    override fun isSameAs(other: com.xwray.groupie.Item<*>?): Boolean {
        return other is ContactItem && other.contact == contact
    }

    override fun getChangePayload(newItem: com.xwray.groupie.Item<*>): Any {
        if (newItem is ContactItem && newItem.friendshipState != friendshipState) {
            return Payload.Joined
        } else {
            return Payload.DontUpdate
        }
    }
}

interface OnContactAddFriendListener {
    fun onContactAddFriendClicked(item: Any, contact: Contact)
}