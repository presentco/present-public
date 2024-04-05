package co.present.present.feature.common

import co.present.present.model.User
import io.reactivex.Completable


interface FriendUser {
    fun changeUserFriendship(user: User, currentUserId: String): Completable
}