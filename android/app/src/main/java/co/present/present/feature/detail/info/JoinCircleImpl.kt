package co.present.present.feature.detail.info

import android.util.Log
import co.present.present.db.CircleDao
import co.present.present.db.UserDao
import co.present.present.feature.common.GetMembers
import co.present.present.model.Circle
import co.present.present.service.rpc.joinCircle
import co.present.present.service.rpc.leaveCircle
import io.reactivex.Completable
import io.reactivex.Single
import present.proto.GroupMembershipState.*
import present.proto.GroupService
import javax.inject.Inject


class JoinCircleImpl @Inject constructor(private val groupService: GroupService,
                                         private val circleDao: CircleDao,
                                         private val userDao: UserDao,
                                         private val getMembers: GetMembers): JoinCircle, GetMembers by getMembers {
    private val TAG = javaClass.simpleName

    override fun toggleCircleJoin(circle: Circle): Completable {
        return when (circle.getGroupMembershipState()) {
            NONE, UNJOINED, INVITED, REJECTED -> joinCircle(circle)
            REQUESTED, ACTIVE -> leaveCircle(circle)
        }
    }

    private fun leaveCircle(circle: Circle): Completable {
        return userDao.getCurrentUser().firstOrError().map {
            // Change the value optimistically in our database
            val newCircle = circle.copy(joined = false, membershipState = UNJOINED.value, participantCount = circle.participantCount - 1)
            circleDao.update(newCircle)
            removeFromMembersCache(circle.id, it)
            it
        }.flatMapCompletable { user ->
            groupService.leaveCircle(circle.id).onErrorResumeNext {
                // If we couldn't update on server, set it back like it was
                restoreCircleInDatabaseAsync(circle).toSingleDefault(user).map { addToMembersCache(circle.id, user) }.toCompletable()
            }
        }
    }

    private fun joinCircle(circle: Circle): Completable {
        // Optimistically request membership in the circle
        return Single.fromCallable {
            circleDao.update(circle.copy(membershipState = REQUESTED.value)); circle
        }.flatMap { groupService.joinCircle(circle.id) }
                .map {
                    // If membership is active, increment participant count
                    val partipantCount = circle.participantCount + (if (it == ACTIVE) 1 else 0)

                    circleDao.update(circle.copy(joined = it == ACTIVE, membershipState = it.value, participantCount = partipantCount))
                    if (it == ACTIVE) {
                        addToMembersCache(circle.id, userDao.getCurrentUserSync())
                    }
                }.toCompletable()
                .onErrorResumeNext {
                    Log.e(TAG, "error", it)
                    restoreCircleInDatabaseAsync(circle)
                }
    }

    private fun restoreCircleInDatabaseAsync(circle: Circle): Completable {
        return Completable.fromCallable {
            circleDao.update(circle)
        }
    }

}