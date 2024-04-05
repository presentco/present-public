package co.present.present.feature.common

import co.present.present.model.CurrentUser
import co.present.present.model.User

interface OnUserAddFriendListener {
    fun onUserAddFriendClicked(item: Any, user: User, currentUser: CurrentUser?)
}