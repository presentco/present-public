package co.present.present.feature.common

import co.present.present.feature.common.viewmodel.GetCurrentUser
import co.present.present.model.User
import co.present.present.service.rpc.CircleService
import co.present.present.service.rpc.getMembers
import com.jakewharton.rxrelay2.BehaviorRelay
import io.reactivex.BackpressureStrategy
import io.reactivex.Flowable
import io.reactivex.schedulers.Schedulers
import javax.inject.Inject


interface GetMembers {
    fun getMembers(circleId: String): Flowable<List<User>>
    fun addToMembersCache(circleId: String, user: User)
    fun removeFromMembersCache(circleId: String, user: User)
}

class GetMembersImpl @Inject constructor(getCircle: GetCircle, val circleService: CircleService,
                                         val getCurrentUser: GetCurrentUser): GetMembers,
        GetCircle by getCircle, GetCurrentUser by getCurrentUser {

    override fun addToMembersCache(circleId: String, user: User) {
        val subject = getMembersCache(circleId)
        val members = subject.value
        val newMembers = members.toMutableList().apply { add(user) }
        subject.accept(newMembers)
    }

    override fun removeFromMembersCache(circleId: String, user: User) {
        val subject = membersCache.getValue(circleId)
        val members = subject.value
        val newMembers = members.toMutableList().apply { remove(user) }
        subject.accept(newMembers)
    }

    private val membersCache = HashMap<String, BehaviorRelay<List<User>>>()

    override fun getMembers(circleId: String): Flowable<List<User>> {
        return getMembersCache(circleId).toFlowable(BackpressureStrategy.LATEST)
    }

    private fun getMembersCache(circleId: String): BehaviorRelay<List<User>> {
        if (!membersCache.containsKey(circleId)) {

            val subject = BehaviorRelay.createDefault(listOf<User>())
            // This call won't succeed if user isn't logged in, so gate it on the current user object
            val flowable = currentUser.flatMapSingle { _ ->
                circleService.getMembers(circleId).subscribeOn(Schedulers.io()).map { groupMembersResponse ->
                    groupMembersResponse.members.map { userResponse -> User(userResponse) }
                }.onErrorReturnItem(listOf())
            }
            flowable.toObservable().subscribe(subject)


            membersCache[circleId] = subject
        }
        return membersCache.getValue(circleId)
    }

}