package co.present.present.feature.detail.chat

import android.content.Context
import android.util.AttributeSet
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.lifecycle.LifecycleObserver
import co.present.present.R
import co.present.present.extensions.hide
import co.present.present.extensions.show
import co.present.present.model.CurrentUser
import io.reactivex.disposables.CompositeDisposable
import kotlinx.android.synthetic.main.chat_empty.view.*
import present.proto.GroupMembershipState


class ChatEmptyView(context: Context, attrSet: AttributeSet) : ConstraintLayout(context, attrSet), LifecycleObserver {

    val disposable = CompositeDisposable()

    init {
        inflate(getContext(), R.layout.chat_empty, this)
    }

    fun bind(state: ChatViewModel.State, inviteButtonClickListener: InviteButtonClickListener) {
        if (state is ChatViewModel.State.CanReadCanPost) {
            // No chats!
            emptyText.setText(R.string.empty_chat)
            button.show()
        } else {
            // If they can't see the chats, they can't add members, either.
            button.hide()

            val groupMembershipState = state.groupMembershipState ?: GroupMembershipState.NONE
            when (groupMembershipState) {
                GroupMembershipState.NONE, GroupMembershipState.UNJOINED -> emptyText.setText(R.string.join_circle_to_see_posts)
                GroupMembershipState.REQUESTED, GroupMembershipState.REJECTED -> emptyText.setText(R.string.circle_membership_requested)
                else -> emptyText.setText(R.string.empty_chat)
            }
        }
        button.setOnClickListener { inviteButtonClickListener.onInviteButtonClicked(state.currentUser) }
    }

    interface InviteButtonClickListener {
        fun onInviteButtonClicked(currentUser: CurrentUser?)
    }


}

