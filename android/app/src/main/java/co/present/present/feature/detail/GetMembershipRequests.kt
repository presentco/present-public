
package co.present.present.feature.detail

import co.present.present.feature.common.viewmodel.GetCurrentUser
import co.present.present.model.MemberRequest
import co.present.present.model.User
import co.present.present.service.rpc.CircleService
import co.present.present.service.rpc.approveMember
import co.present.present.service.rpc.getMemberRequests
import co.present.present.service.rpc.removeMember
import com.jakewharton.rxrelay2.BehaviorRelay
import io.reactivex.BackpressureStrategy
import io.reactivex.Completable
import io.reactivex.Flowable
import io.reactivex.schedulers.Schedulers
import javax.inject.Inject


interface GetMemberRequests {
    fun getMemberRequests(circleId: String): Flowable<List<MemberRequest>>
    fun approveMember(user: User, circleId: String): Completable
    fun removeRequest(user: User, circleId: String): Completable
}

class GetMembershipRequestsImpl @Inject constructor(val circleService: CircleService,
                                                    val getCurrentUser: GetCurrentUser)
    : GetMemberRequests, GetCurrentUser by getCurrentUser {
    override fun removeRequest(user: User, circleId: String): Completable {
        return circleService.removeMember(user.id, circleId).doOnSubscribe {
            // Optimistically dispatch a new list of the previous requests, in which this member is removed
            removeRequestFromList(circleId, user)
        }
    }

    private fun removeRequestFromList(circleId: String, user: User) {
        val subject = memberRequestsMap.getValue(circleId)
        val members = subject.value

        val newMembers = members.toMutableList().filter { it.user.id != user.id }
        subject.accept(newMembers)
    }

    override fun approveMember(user: User, circleId: String): Completable {
        return circleService.approveMember(user.id, circleId).doOnSubscribe {
            // Optimistically dispatch a new list of the previous requests, in which this member is removed
            removeRequestFromList(circleId, user)
        }
    }

    private val memberRequestsMap = hashMapOf<String, BehaviorRelay<List<MemberRequest>>>()

    override fun getMemberRequests(circleId: String): Flowable<List<MemberRequest>> {
        if (!memberRequestsMap.containsKey(circleId)) {

            val subject = BehaviorRelay.createDefault(listOf<MemberRequest>())
            // This call won't succeed if user isn't logged in, so gate it on the current user object
            val flowable = currentUser.flatMapSingle {
                circleService.getMemberRequests(circleId).subscribeOn(Schedulers.io())
            }
            flowable.toObservable().onErrorReturn { listOf() }.subscribe(subject)

            memberRequestsMap[circleId] = subject
        }
        return memberRequestsMap.getValue(circleId).toFlowable(BackpressureStrategy.LATEST)
    }

}