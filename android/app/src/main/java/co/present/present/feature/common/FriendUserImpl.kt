package co.present.present.feature.common

import co.present.present.db.FriendRelationshipDao
import co.present.present.db.UserDao
import co.present.present.model.FriendRelationship
import co.present.present.model.FriendshipState
import co.present.present.model.User
import co.present.present.service.rpc.addFriend
import co.present.present.service.rpc.removeFriend
import io.reactivex.Completable
import io.reactivex.Single
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.schedulers.Schedulers
import present.proto.UserService
import javax.inject.Inject


class FriendUserImpl @Inject constructor(val friendshipDao: FriendRelationshipDao,
                                         val userDao: UserDao, val userService: UserService) : FriendUser {
    private val TAG = javaClass.simpleName

    override fun changeUserFriendship(user: User, currentUserId: String): Completable {
        return friendshipDao.getFriendshipState(currentUserId, user.id).firstOrError().flatMapCompletable { state ->
            when (state) {
                FriendshipState.Accepted -> {
                    // Because you can terminate a friendship immediately,
                    // Change the value optimistically in our database
                    removeFriendshipInDatabase(user, currentUserId).toSingleDefault(state)
                            .flatMapCompletable { removeFriendshipOnServer(user) }
                            // If we couldn't update on server, set it back like it was
                            .doOnError {
                                addFriendshipInDatabaseAsync(user, currentUserId)
                            }
                }
                FriendshipState.Requested -> {
                    // Same as accepted, except if it doesn't work, we add back a request instead of
                    // a friendship
                    removeFriendshipInDatabase(user, currentUserId).toSingleDefault(state)
                            .flatMapCompletable { removeFriendshipOnServer(user) }
                            .doOnError { addFriendRequestInDatabaseAsync(user, currentUserId) }
                }
                FriendshipState.None -> {
                    // Request the friendship in database optimistically
                    addFriendRequestInDatabase(user, currentUserId).toSingleDefault(state)
                            .flatMap { addFriendshipOnServer(user) }
                            .flatMapCompletable { friendshipState ->
                                when (friendshipState) {
                                    FriendshipState.Accepted -> addFriendshipInDatabase(user, currentUserId)
                                    FriendshipState.Requested -> addFriendRequestInDatabase(user, currentUserId)
                                    FriendshipState.None -> removeFriendshipInDatabase(user, currentUserId)
                                }
                            }
                            .doOnError {
                                removeFriendshipInDatabase(user, currentUserId)
                            }
                }
            }
        }
    }

    private fun addFriendshipInDatabaseAsync(user: User, currentUserId: String) {
        addFriendshipInDatabase(user, currentUserId)
                .subscribeOn(Schedulers.io())
                .subscribeBy(onError = {}, onComplete = {})
    }

    private fun addFriendRequestInDatabaseAsync(user: User, currentUserId: String) {
        addFriendRequestInDatabase(user, currentUserId)
                .subscribeOn(Schedulers.io())
                .subscribeBy(onError = {}, onComplete = {})
    }

    /**
     * The current user is requesting to be friends with the other user.
     */
    private fun addFriendRequestInDatabase(user: User, currentUserId: String): Completable {
        val friendRequest = FriendRelationship(currentUserId, user.id, FriendshipState.Requested.value)
        return Completable.fromCallable {
            friendshipDao.insert(friendRequest)
        }
    }

    private fun addFriendshipOnServer(user: User): Single<FriendshipState> {
        return userService.addFriend(user.id)
    }

    private fun removeFriendshipOnServer(user: User): Completable {
        return userService.removeFriend(user.id)
    }

    private fun addFriendshipInDatabase(user: User, currentUserId: String): Completable {
        return Completable.fromCallable {
                userDao.insert(user)
                friendshipDao.addFriendship(currentUserId, user.id)
        }
    }

    private fun removeFriendshipInDatabase(user: User, currentUserId: String): Completable {
        return Completable.fromCallable {
            friendshipDao.deleteFriendship(currentUserId, user.id)
        }
    }
}