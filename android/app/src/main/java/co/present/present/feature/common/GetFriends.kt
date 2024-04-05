package co.present.present.feature.common

import co.present.present.model.User
import io.reactivex.Flowable


interface GetFriends {
    fun getFriends(userId: String): Flowable<List<User>>
}