package co.present.present.feature.common

import co.present.present.db.FriendRelationshipDao
import co.present.present.db.Database
import co.present.present.db.UserDao
import co.present.present.model.User
import co.present.present.service.rpc.getFriends
import io.reactivex.Flowable
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.schedulers.Schedulers
import present.proto.UserService
import javax.inject.Inject


class GetFriendsImpl @Inject constructor(val userService: UserService, val database: Database,
                                         val userDao: UserDao,
                                         val friendRequestDao: FriendRelationshipDao): GetFriends {

    private val friends = HashMap<String, Flowable<List<User>>>()

    private fun refreshFriendsAsync(userId: String) {
        userService.getFriends(userId).subscribeOn(Schedulers.io())
                .flatMapCompletable { users ->
                    database.persistFriends(userId, users)
                }
                .subscribeOn(Schedulers.io())
                .subscribeBy(onError = {}, onComplete = {})
    }


    override fun getFriends(userId: String): Flowable<List<User>> {
        if (!friends.contains(userId)) {
            friends[userId] = userDao.getFriends(userId).distinctUntilChanged()
                    .replay(1).autoConnect()

            // Refresh from network once, when observable is created.
            // Update will be reflected in db
            refreshFriendsAsync(userId)
        }
        return friends.getValue(userId)
    }
}