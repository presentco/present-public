package co.present.present.feature.invite

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import co.present.present.R
import co.present.present.db.FriendRelationshipDao
import co.present.present.db.UserDao
import co.present.present.extensions.combineLatest
import co.present.present.extensions.string
import co.present.present.feature.GetContacts
import co.present.present.feature.GetFriendRequests
import co.present.present.feature.common.FriendUser
import co.present.present.feature.common.GetFriends
import co.present.present.feature.common.GetMembers
import co.present.present.feature.common.OnUserAddFriendListener
import co.present.present.feature.common.item.HeaderItem
import co.present.present.feature.common.item.TextItem
import co.present.present.feature.common.item.UserItem
import co.present.present.feature.common.viewmodel.GetCurrentUser
import co.present.present.model.*
import co.present.present.service.rpc.CircleService
import co.present.present.service.rpc.addFriend
import com.jakewharton.rxrelay2.BehaviorRelay
import com.xwray.groupie.Group
import com.xwray.groupie.Section
import io.reactivex.BackpressureStrategy
import io.reactivex.Completable
import io.reactivex.Flowable
import io.reactivex.rxkotlin.combineLatest
import present.proto.Empty
import present.proto.UserService
import javax.inject.Inject

class AddFriendsViewModel @Inject
constructor(val userDao: UserDao,
            val userService: UserService,
            val circleService: CircleService,
            val friendshipDao: FriendRelationshipDao,
            getCurrentUser: GetCurrentUser,
            getMembers: GetMembers,
            getContacts: GetContacts,
            getFriends: GetFriends,
            getFriendRequests: GetFriendRequests,
            friendUser: FriendUser,
            application: Application)
    : AndroidViewModel(application), GetCurrentUser by getCurrentUser, GetMembers by getMembers,
        GetContacts by getContacts, GetFriends by getFriends, GetFriendRequests by getFriendRequests,
        FriendUser by friendUser {
    private val TAG = javaClass.simpleName

    private val searchEmitter: BehaviorRelay<String> = BehaviorRelay.createDefault("")
    private val search: Flowable<String> = searchEmitter.toFlowable(BackpressureStrategy.LATEST)

    // ONLY for contacts who are NOT ALSO USERS on Present already
    private val requestedContactEmitter = BehaviorRelay.createDefault<List<Contact>>(listOf())
    private val requestedContacts = requestedContactEmitter.toFlowable(BackpressureStrategy.LATEST)

    private inner class OnSearchListener : SearchItem.OnSearchChangedListener {
        override fun onSearchChanged(searchText: String) {
            Log.d(TAG, "onSearchChanged: $searchText")
            searchEmitter.accept(searchText)
        }
    }

    fun getItems(): Flowable<List<Group>> {
        return search.map { listOf(TextItem(string(R.string.add_friends_prompt)), SearchItem(OnSearchListener(), it, R.string.search_for_a_friend)) }
    }

    fun getFriendRelationships(): Flowable<List<FriendRelationship>> {
        return currentUser.flatMap { friendshipDao.getRelationships(it.id) }
    }

    fun getFacebookFriendsItemsAndInfo(onUserAddFriendListener: OnUserAddFriendListener): Flowable<Triple<List<Group>, CurrentUser, String>> {
        return facebookFriends.combineLatest(
                currentUser,
                getFriendRelationships(),
                search
        ).map { (friends, currentUser, currentUserRelationships, searchText) ->
            friends.filter { it.name.contains(searchText, true) }
                    .sortedBy { it.name }
                    .map { user ->
                        makeUserItem(user, currentUser, currentUserRelationships, onUserAddFriendListener)
                    }
        }.combineLatest(currentUser, search)
    }

    private fun makeUserItem(user: User,
                             currentUser: CurrentUser,
                             currentUserRelationships: List<FriendRelationship>,
                             onUserAddFriendListener: OnUserAddFriendListener): Group {
        return UserItem(user,
                currentUser,
                currentUserRelationships.find { it.otherUserId == user.id }?.friendshipState
                        ?: FriendshipState.None,
                onUserAddFriendListener)
    }

    private val facebookFriends: Flowable<List<User>> by lazy {
        Flowable.fromCallable {
            userService.getFacebookFriends(Empty())
        }.map { it.users.map { User(it) } }.replay(1).autoConnect()
    }

    private fun getFilteredContacts(): Flowable<List<Contact>> {
        return search.combineLatest(localContacts).map { (search, contacts) -> contacts.filter { it.displayName.contains(search, ignoreCase = true) } }
    }

    fun getContactItemsAndInfo(onUserAddFriendListener: OnUserAddFriendListener, onContactAddFriendListener: OnContactAddFriendListener): Flowable<Triple<List<Group>, CurrentUser, String>> {
        return getFilteredContacts().combineLatest(
                currentUser,
                getFriendRelationships(),
                usersWhoAreContacts,
                requestedContacts,
                search
        ).map { (contacts, currentUser, friendRelationships, usersWhoAreContacts, requestedContacts, search) ->
            mutableListOf<Group>().apply {

                val pending = Section(HeaderItem(R.string.pending)).apply {
                    setHideWhenEmpty(true)
                }

                contacts.forEach {
                    val user = usersWhoAreContacts.keyForValue(it)
                    val friendshipState = friendRelationships.find { it.otherUserId == user?.id }?.friendshipState ?: FriendshipState.None

                    if (user == null || !user.member) {
                        val contactFriendshipState = if (requestedContacts.contains(it)) FriendshipState.Requested else friendshipState
                        val item = ContactItem(it, currentUser, contactFriendshipState, onContactAddFriendListener)
                        when (friendshipState) {
                            FriendshipState.None -> add(item)
                            FriendshipState.Requested -> pending.add(item)
                        }
                    } else if (user.isNot(currentUser)) {
                        val item = UserItem(user, currentUser, friendshipState, onUserAddFriendListener)
                        when (friendshipState) {
                            FriendshipState.None -> add(item)
                            FriendshipState.Requested -> pending.add(item)
                        }
                    }
                }
                add(pending)

            } as List<Group>

        }.combineLatest(currentUser, search)
    }

    private fun Map<User, Contact?>.keyForValue(value: Contact): User? {
        return this.keys.firstOrNull { this[it] == value }
    }

    fun contactRequested(contact: Contact): Completable {
        val requestedContacts = requestedContactEmitter.value.toMutableList()
        requestedContacts.apply {
            if (!contains(contact)) this += contact
        }
        requestedContactEmitter.accept(requestedContacts)
        return userService.addFriend(phoneNumber = contact.phones.values.first())
                .doOnError {
                    requestedContacts -= contact
                    requestedContactEmitter.accept(requestedContacts)
                }
                .toCompletable()
    }

}