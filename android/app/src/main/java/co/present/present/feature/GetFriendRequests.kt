package co.present.present.feature

import android.util.Log
import co.present.present.db.Database
import co.present.present.db.FriendRelationshipDao
import co.present.present.db.UserDao
import co.present.present.extensions.applyCompletableSchedulers
import co.present.present.extensions.applySingleSchedulers
import co.present.present.feature.common.viewmodel.GetCurrentUser
import co.present.present.model.FriendRelationship
import co.present.present.model.FriendshipState
import co.present.present.model.User
import co.present.present.service.rpc.addFriend
import co.present.present.service.rpc.getIncomingFriendRequests
import co.present.present.service.rpc.getOutgoingFriendRequests
import co.present.present.service.rpc.removeFriend
import io.reactivex.Completable
import io.reactivex.Flowable
import io.reactivex.rxkotlin.subscribeBy
import present.proto.UserService
import javax.inject.Inject


interface GetFriendRequests {
    fun getIncomingFriendRequests(): Flowable<List<User>>
    fun removeIncomingFriendRequestOnServer(fromUserId: String): Completable
    fun removeIncomingFriendRequestLocally(fromUserId: String): Completable
    fun undoRemoveIncomingFriendRequestLocally(fromUserId: String): Completable
    fun approveIncomingFriendRequest(fromUserId: String): Completable

    fun getOutgoingFriendRequests(): Flowable<List<User>>
    //fun getFriendshipState(userId: String): Flowable<FriendshipState>
}

class GetFriendRequestsImpl @Inject constructor(
        val database: Database,
        val userService: UserService,
        val friendshipDao: FriendRelationshipDao,
        val userDao: UserDao,
        val getCurrentUser: GetCurrentUser)
    : GetFriendRequests, GetCurrentUser by getCurrentUser {

    private val TAG = javaClass.simpleName

    private var incomingFriendRequests : Flowable<List<User>>? = null
    private var outgoingFriendRequests : Flowable<List<User>>? = null


    override fun approveIncomingFriendRequest(fromUserId: String): Completable {
        // Optimistically make database changes
        return currentUser.firstOrError().map { currentUser ->
            friendshipDao.addFriendship(fromUserId, currentUser.id)
        }.flatMap { userService.addFriend(fromUserId) }
//                .map { friendshipState ->
//            if (friendshipState != FriendshipState.Accepted) {
//                    // Yikes, this is strange. The other user must have retracted their friend
//                    // request in the interim. An edge case for sure
//                    //TODO re-insert friend request in DB
//                }
//            }
//        }
        .toCompletable().doOnError {
            // Revert the entire database transaction and put the friend request back the way it was.
            undoApproveFriendRequestInDatabaseAsync(fromUserId)
        }
    }

    private fun undoApproveFriendRequestInDatabaseAsync(userId: String) {
        currentUser.firstOrError().map { currentUser ->
            database.runInTransaction {
                friendshipDao.delete(FriendRelationship(currentUser.id, userId, FriendshipState.Accepted.value))
                friendshipDao.insert(FriendRelationship(userId, currentUser.id, FriendshipState.Requested.value))
            }
        }.compose(applySingleSchedulers()).subscribeBy(
                onError = { Log.e(TAG, "Error adding back friend request to database", it)},
                onSuccess = { Log.d(TAG, "Succesfully added back friend request to database after an error connecting to server") }
        )
    }

    override fun removeIncomingFriendRequestOnServer(fromUserId: String): Completable {
        return currentUser.firstOrError()
                    .flatMapCompletable { currentUser ->
                        userService.removeFriend(fromUserId)
                            // If we couldn't remove from server, revert the database change and add friend request back
                            .onErrorResumeNext { undoRemoveIncomingFriendRequestLocally(fromUserId) }
                    }.compose(applyCompletableSchedulers())
    }

    override fun removeIncomingFriendRequestLocally(fromUserId: String): Completable {
        return currentUser.firstOrError().map { currentUser ->
            friendshipDao.delete(FriendRelationship(fromUserId, currentUser.id, FriendshipState.Requested.value))
        }.toCompletable()
    }

    override fun undoRemoveIncomingFriendRequestLocally(fromUserId: String): Completable {
        return currentUser.firstOrError().map { currentUser ->
            friendshipDao.insert(FriendRelationship(fromUserId, currentUser.id, FriendshipState.Requested.value))
        }.toCompletable()
    }

    override fun getIncomingFriendRequests(): Flowable<List<User>> {
        val incomingFriendRequests = this.incomingFriendRequests
            if (incomingFriendRequests == null) {
                val flowable = currentUser.firstOrError().toFlowable().flatMap {
                    friendshipDao.getInboundRequestedUsers(it.id)
                            .doOnNext { Log.d(TAG, "Fetched ${it.size} incoming friend requests from database") }
                            .distinctUntilChanged()
                            .replay(1).autoConnect()
                }

                // Refresh from network once, when observable is created.
                // Update will be reflected in db
                refreshIncomingFriendRequestsAsync()
                this.incomingFriendRequests = flowable
                return flowable
            }
            return incomingFriendRequests
    }

    override fun getOutgoingFriendRequests(): Flowable<List<User>> {
        val outgoingFriendRequests = this.outgoingFriendRequests
        if (outgoingFriendRequests == null) {
            val flowable = currentUser.firstOrError().toFlowable().flatMap {
                friendshipDao.getOutboundRequestedUsers(it.id)
                        .doOnNext { Log.d(TAG, "Fetched ${it.size} outgoing friend requests from database") }
                        .distinctUntilChanged()
                        .replay(1).autoConnect()
            }

            // Refresh from network once, when observable is created.
            // Update will be reflected in db
            refreshOutgoingFriendRequestsAsync()
            this.outgoingFriendRequests = flowable
            return flowable
        }
        return outgoingFriendRequests
    }

    private fun refreshIncomingFriendRequestsAsync() {
        currentUser.firstOrError().flatMap { currentUser ->
            userService.getIncomingFriendRequests().map {
                updateIncomingUserRequestsInDatabase(currentUser.id, it); it
            }
        }
                .compose(applySingleSchedulers())
                .subscribeBy(
                        onError = { Log.e(TAG, "Couldn't update incoming friend requests", it) },
                        onSuccess = { Log.d(TAG, "Successfully updated ${it.size} incoming friend requests") })
    }

    private fun updateIncomingUserRequestsInDatabase(userId: String, users: List<User>) {
        database.runInTransaction {
            friendshipDao.deleteInboundRequests(userId)
            users.forEach { userDao.insertOrPartialUpdate(it) }
            friendshipDao.insert(users.map { FriendRelationship(it.id, userId, FriendshipState.Requested.value) })
        }
    }

    private fun refreshOutgoingFriendRequestsAsync() {
        currentUser.firstOrError().flatMap { currentUser ->
            userService.getOutgoingFriendRequests().map {
                updateOutgoingUserRequestsInDatabase(currentUser.id, it); it
            }
        }
                .compose(applySingleSchedulers())
                .subscribeBy(
                        onError = { Log.e(TAG, "Couldn't update outgoing friend requests", it) },
                        onSuccess = { Log.d(TAG, "Successfully updated ${it.size} outgoing friend requests") })
    }

    private fun updateOutgoingUserRequestsInDatabase(userId: String, users: List<User>) {
        database.runInTransaction {
            friendshipDao.deleteOutboundRequests(userId)
            users.forEach { userDao.insertOrPartialUpdate(it) }
            friendshipDao.insert(users.map { FriendRelationship(userId, it.id, FriendshipState.Requested.value) })
        }
    }

}