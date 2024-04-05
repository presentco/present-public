package co.present.present.feature.profile.info

import android.content.Context
import android.util.AttributeSet
import android.view.View
import androidx.constraintlayout.widget.ConstraintLayout
import co.present.present.R
import co.present.present.extensions.hide
import co.present.present.extensions.show
import co.present.present.feature.common.OnUserAddFriendListener
import co.present.present.model.CurrentUser
import co.present.present.model.FriendshipState
import co.present.present.model.User
import co.present.present.model.isAlso
import kotlinx.android.extensions.LayoutContainer
import kotlinx.android.synthetic.main.profile_actions.view.*


class UserProfileButtons(context: Context, attributeSet: AttributeSet) : ConstraintLayout(context, attributeSet), LayoutContainer {
    override val containerView: View? = this

    init {
        View.inflate(context, R.layout.profile_actions, this)
    }

    fun bind(currentUser: CurrentUser, user: User, friendshipState: FriendshipState, listener: OnProfileActionClickListener, addFriendListener: OnUserAddFriendListener) {

        if (user.isAlso(currentUser)) {

            add.show()
            listOf(add, addIcon).forEach {
                it.setOnClickListener {
                    listener.onProfileInviteClicked(user, currentUser)
                }
            }

            // Edit button
            join.setText(R.string.edit_profile)
            joinIcon.setImageResource(R.drawable.ic_edit_profile)
            listOf(join, joinIcon).forEach {
                it.setOnClickListener {
                    listener.onProfileEditClicked(user, currentUser)
                }
            }

        } else {
            // TODO: Eventually this will be the "Start Convo button"
            add.hide()
            addIcon.hide()

            // Add Friend / Added button
            val text = when(friendshipState) {
                FriendshipState.Accepted -> R.string.added
                FriendshipState.Requested -> R.string.requested
                else -> R.string.add_friend
            }
            join.setText(text)
            joinIcon.setImageResource(if (friendshipState == FriendshipState.Accepted) R.drawable.ic_added_profile else R.drawable.ic_add_friend_profile)
            listOf(join, joinIcon).forEach {
                it.setOnClickListener {
                    addFriendListener.onUserAddFriendClicked(this, user, currentUser)
                }
            }
        }

        listOf(share, shareIcon).forEach {
            it.setOnClickListener {
                listener.onProfileShareClicked(user, currentUser)
            }
        }
    }

    interface OnProfileActionClickListener {
        fun onProfileShareClicked(user: User, currentUser: CurrentUser)
        fun onProfileInviteClicked(user: User, currentUser: CurrentUser)
        fun onProfileEditClicked(user: User, currentUser: CurrentUser)
    }

}