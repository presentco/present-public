package co.present.present.feature.common

import android.content.Context
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatButton
import co.present.present.R
import co.present.present.extensions.hide
import co.present.present.extensions.show
import co.present.present.extensions.string
import co.present.present.feature.invite.Contact
import co.present.present.feature.invite.OnContactAddFriendListener
import co.present.present.model.CurrentUser
import co.present.present.model.FriendshipState
import co.present.present.model.User
import co.present.present.model.isAlso
import com.xwray.groupie.kotlinandroidextensions.Item

class FriendButton(context: Context, attributeSet: AttributeSet) : AppCompatButton(context, attributeSet) {

    var progress = false

    fun bind(user: User, currentUser: CurrentUser?, friendshipState: FriendshipState, item: Item, listener: OnUserAddFriendListener? = null) {
        if (listener == null || user.isAlso(currentUser)) {
            hide()
        } else {
            show()
            isSelected = isSelected(friendshipState)
            text = context.string(getText(friendshipState))
            setOnClickListener {
                listener.onUserAddFriendClicked(item, user, currentUser)
            }
        }
    }

    private fun getText(friendshipState: FriendshipState): Int {
        return when (friendshipState) {
            FriendshipState.Requested -> R.string.requested
            FriendshipState.Accepted -> R.string.added
            else -> R.string.add_friend
        }
    }

    private fun isSelected(friendshipState: FriendshipState): Boolean {
        return when (friendshipState) {
            FriendshipState.None -> false
            else -> true
        }
    }

}

class ContactButton(context: Context, attributeSet: AttributeSet) : AppCompatButton(context, attributeSet) {

    var progress = false

    fun bind(contact: Contact, friendshipState: FriendshipState, item: Item, listener: OnContactAddFriendListener) {

            isSelected = isSelected(friendshipState)
            text = context.string(getText(friendshipState))
            setOnClickListener {
                listener.onContactAddFriendClicked(item, contact)
            }
    }

    private fun getText(friendshipState: FriendshipState): Int {
        return when (friendshipState) {
            FriendshipState.Requested -> R.string.requested
            else -> R.string.add_friend
        }
    }

    private fun isSelected(friendshipState: FriendshipState): Boolean {
        return when (friendshipState) {
            FriendshipState.None -> false
            else -> true
        }
    }

}