package co.present.present.feature.detail.info

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import co.present.present.R
import co.present.present.db.FriendRelationshipDao
import co.present.present.db.UserDao
import co.present.present.extensions.application
import co.present.present.extensions.applyCompletableSchedulers
import co.present.present.extensions.combineLatest
import co.present.present.extensions.string
import co.present.present.feature.common.*
import co.present.present.feature.common.item.GrayHeaderItem
import co.present.present.feature.common.item.TextItem
import co.present.present.feature.common.item.UserItem
import co.present.present.feature.common.viewmodel.GetCurrentUser
import co.present.present.feature.detail.GetMemberRequests
import co.present.present.model.*
import com.xwray.groupie.Group
import com.xwray.groupie.Item
import com.xwray.groupie.Section
import io.reactivex.Flowable
import io.reactivex.rxkotlin.subscribeBy
import javax.inject.Inject


class MembersViewModel @Inject constructor(private val getFriends: GetFriends,
                                           val userDao: UserDao,
                                           val getCurrentUser: GetCurrentUser,
                                           val getMemberRequests: GetMemberRequests,
                                           val friendshipDao: FriendRelationshipDao,
                                           application: Application,
                                           friendUser: FriendUser,
                                           getMembers: GetMembers,
                                           getCircle: GetCircle)
    : AndroidViewModel(application), GetCurrentUser by getCurrentUser, FriendUser by friendUser, GetFriends by getFriends,
        GetMembers by getMembers, GetCircle by getCircle, GetMemberRequests by getMemberRequests {

    // This class is just here to capture the circleId
    inner class OnUserApproveListener(val circleId: String): UserRequestItem.OnUserApproveListener {
        override fun onApproveClicked(item: Item<*>, user: User, currentUser: CurrentUser) {
            approveMember(user, circleId).doOnSubscribe { addToMembersCache(circleId, user) }.compose(applyCompletableSchedulers())
                    .subscribeBy(onError = {}, onComplete = {})
        }
    }

    fun getFriendRelationships(): Flowable<List<FriendRelationship>> {
        return currentUser.flatMap { friendshipDao.getRelationships(it.id) }
    }

    fun getItems(circleId: String, onUserAddFriendListener: OnUserAddFriendListener): Flowable<List<Group>> {
        return getFriendRelationships().combineLatest(
                getMembers(circleId),
                currentUser,
                getMemberRequests(circleId),
                getCircle(circleId))
                .map { (currentUserRelationships, members, currentUser, memberRequests, circle) ->
                    val groups = mutableListOf<Group>()

                    fun User.toItem(): UserItem {
                        return UserItem(this, currentUser,
                                currentUserRelationships.find { it.otherUserId == this.id }?.friendshipState ?: FriendshipState.None,
                                onUserAddFriendListener)
                    }

                    val creator: User? = members.find { it.id == circle.ownerId }

                    if (creator != null) {
                        groups += GrayHeaderItem(R.string.creator)
                        groups += creator.toItem()
                    }

                    if (currentUser.isAdmin || members.any { it.isAlso(currentUser) }) {
                        val requestsSection = Section(MemberRequestsHeader(memberRequests.size)).apply {
                            addAll(memberRequests.map { UserRequestItem(it.user, currentUser, it.isApproved, OnUserApproveListener(circleId)) })
                            setPlaceholder(TextItem(string(R.string.no_member_requests)))
                        }
                        groups += requestsSection
                    }

                    val membersSection = Section().apply {
                        if (groups.isNotEmpty()) {
                            // We only need a header on this section if there are others.
                            // Otherwise the page header will suffice
                            setHeader(MembersHeader(circle))
                            setHideWhenEmpty(true)
                        }
                        addAll(members.map { it.toItem() })
                    }
                    groups += membersSection

                    groups


                }
    }

    fun onRequestRemoved(item: Item<*>, circleId: String, user: User) {
        removeRequest(user, circleId).compose(applyCompletableSchedulers()).subscribeBy(onError = {}, onComplete = {})
    }

    inner class MembersHeader(val circle: Circle) : GrayHeaderItem(
            string = application.resources.getQuantityString(R.plurals.members_w_num_template, circle.participantCount, circle.participantCount)
    )

    inner class MemberRequestsHeader(numMemberRequests: Int) : GrayHeaderItem(
            string = if (numMemberRequests == 0) string(R.string.member_requests) else string(R.plurals.member_requests_w_num_template, numMemberRequests, numMemberRequests)
    )
}