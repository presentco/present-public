package co.present.present.feature.profile.info

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import co.present.present.db.*
import co.present.present.extensions.applyCompletableSchedulers
import co.present.present.feature.GetFriendRequests
import co.present.present.feature.GetJoined
import co.present.present.feature.common.FriendUser
import co.present.present.feature.common.GetFriends
import co.present.present.feature.common.viewmodel.GetCurrentUser
import co.present.present.feature.detail.info.JoinCircle
import co.present.present.feature.discovery.RefreshCircles
import co.present.present.feature.profile.GetBlocked
import co.present.present.model.CurrentUser
import co.present.present.model.FriendshipState
import co.present.present.model.User
import co.present.present.model.isAlso
import co.present.present.service.rpc.CircleService
import co.present.present.service.rpc.getUser
import io.reactivex.Flowable
import io.reactivex.rxkotlin.combineLatest
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.schedulers.Schedulers
import present.proto.UserService
import javax.inject.Inject

class UserProfileViewModel @Inject constructor(val database: Database,
                                               val userDao: UserDao,
                                               val friendshipDao: FriendRelationshipDao,
                                               val circleDao: CircleDao,
                                               val blockedDao: BlockedDao,
                                               val getBlocked: GetBlocked,
                                               val circleService: CircleService,
                                               val userService: UserService,
                                               val getCurrentUser: GetCurrentUser,
                                               val refreshCircles: RefreshCircles,
                                               private val joinCircle: JoinCircle,
                                               private val friendUser: FriendUser,
                                               private val getFriends: GetFriends,
                                               private val getJoined: GetJoined,
                                               private val getFriendRequests: GetFriendRequests,
                                               application: Application)
    : AndroidViewModel(application), GetCurrentUser by getCurrentUser, JoinCircle by joinCircle,
        FriendUser by friendUser, GetFriends by getFriends, GetBlocked by getBlocked,
        GetJoined by getJoined, GetFriendRequests by getFriendRequests, RefreshCircles by refreshCircles {
    private val TAG: String = javaClass.simpleName

    init {
        refreshJoined().compose(applyCompletableSchedulers()).subscribeBy(
                onError = { Log.e(TAG, "Error refreshing joined circles", it) },
                onComplete = { }
        )
    }


    val userMap = mutableMapOf<String, Flowable<User>>()

    fun getUser(userId: String): Flowable<User> {
        if (!userMap.contains(userId)) {
            userMap.put(userId, userDao.getUser(userId)
                    .doOnSubscribe { getUserFromNetworkAsync(userId) }
                    .replay(1)
                    .autoConnect())
        }
        return userMap.getValue(userId)
    }

    fun isUserBlockedOrCurrentUser(userId: String): Flowable<Triple<Boolean, Boolean, User>> {
        return isBlocked(userId).combineLatest(
                currentUser.map { it.id == userId },
                getUser(userId))
    }

    fun getCircleUnreadBadgeCount(userId: String): Flowable<Int> {
        return currentUser.flatMap {
            if (userId == it.id) {
                circleDao.getBadgedCircles(it.id).map { it.size }
            } else {
                // Don't badge other users' profiles
                Flowable.just(0)
            }
        }
    }

    private fun getUserFromNetworkAsync(userId: String) {
        userService.getUser(userId).flatMapCompletable { database.persistUser(it) }.subscribeOn(Schedulers.io()).subscribeBy(
                onError = {
                    Log.e(TAG, "Network error getting user $userId from network")
                },
                onComplete = {
                    Log.d(TAG, "Updated user $userId from network")
                }
        )
    }

    fun getUserProfileInfo(userId: String): Flowable<Triple<User, CurrentUser, FriendshipState>> {
        return getUser(userId).combineLatest(currentUser, getFriendshipState(userId)).distinctUntilChanged()
    }

    fun getFriendshipState(userId: String): Flowable<FriendshipState> {
        return currentUser.flatMap { friendshipDao.getFriendshipState(it.id, userId) }
    }

    fun getFriendRequestBadgeCount(userId: String): Flowable<Int> {
        return getUser(userId).combineLatest(currentUser).flatMap { (user, currentUser) ->
            if (user.isAlso(currentUser))  getIncomingFriendRequests()
            else Flowable.just(listOf())
        }.map { it.size }
    }

}
