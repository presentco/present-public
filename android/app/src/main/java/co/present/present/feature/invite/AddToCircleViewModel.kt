package co.present.present.feature.invite

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import co.present.present.R
import co.present.present.db.UserDao
import co.present.present.extensions.combineLatest
import co.present.present.feature.GetContacts
import co.present.present.feature.common.GetFriends
import co.present.present.feature.common.GetMembers
import co.present.present.feature.common.item.GrayCarouselItem
import co.present.present.feature.common.item.PhotoOnlyInviteItem
import co.present.present.feature.common.viewmodel.GetCurrentUser
import co.present.present.model.CurrentUser
import co.present.present.model.User
import co.present.present.service.rpc.CircleService
import com.jakewharton.rxrelay2.BehaviorRelay
import com.xwray.groupie.Group
import io.reactivex.BackpressureStrategy
import io.reactivex.Completable
import io.reactivex.Flowable
import io.reactivex.rxkotlin.combineLatest
import present.proto.Empty
import present.proto.MembersRequest
import present.proto.UserService
import javax.inject.Inject

class AddToCircleViewModel @Inject
constructor(val userDao: UserDao,
            val userService: UserService,
            val circleService: CircleService,
            getCurrentUser: GetCurrentUser,
            getMembers: GetMembers,
            getContacts: GetContacts,
            getFriends: GetFriends,
            application: Application)
    : AndroidViewModel(application), GetCurrentUser by getCurrentUser, GetMembers by getMembers,
        GetContacts by getContacts, GetFriends by getFriends {
    private val TAG = javaClass.simpleName

    val selectedInviteesEmitter = BehaviorRelay.createDefault<List<Invitee>>(listOf())
    val invitees: Flowable<List<Invitee>> = selectedInviteesEmitter.toFlowable(BackpressureStrategy.LATEST)

    val searchEmitter = BehaviorRelay.createDefault("")
    val search = searchEmitter.toFlowable(BackpressureStrategy.LATEST)

    private inner class OnSearchListener : SearchItem.OnSearchChangedListener {
        override fun onSearchChanged(searchText: String) {
            Log.d(TAG, "onSearchChanged: $searchText")
            searchEmitter.accept(searchText)
        }
    }

    fun getItems(): Flowable<List<Group>> {
        return search.combineLatest(invitees).map { (searchText, invitees) ->
            mutableListOf<Group>().apply {
                add(SearchItem(OnSearchListener(), searchText, R.string.search_for_a_friend))
                add(GrayCarouselItem(invitees.map { PhotoOnlyInviteItem(it.contact, it.user) }))
            }
        }
    }

    fun getFriendItemsAndInfo(circleId: String): Flowable<Triple<List<Group>, CurrentUser, String>> {
        return getCurrentUsersFriends().combineLatest(
                getMembers(circleId),
                search,
                invitees,
                usersWhoAreContacts
        ).map { (friends, members, searchText, invitees, usersWhoAreContacts) ->
            friends.filter { it.name.contains(searchText, true) }
                    .map { user ->
                        val contact = usersWhoAreContacts[user]
                        val isChecked: Boolean = invitees.any { it.user == user }
                        CheckableInviteItem(contact, user, members.contains(user), isChecked) as Group
                    }
        }.combineLatest(currentUser, search)
    }


    fun getFacebookFriendsItemsAndInfo(circleId: String): Flowable<Triple<List<Group>, CurrentUser, String>> {
        return facebookFriends.combineLatest(
                getMembers(circleId),
                search,
                invitees,
                usersWhoAreContacts
        ).map { (friends, members, searchText, invitees, usersWhoAreContacts) ->
            friends.filter { it.name.contains(searchText, true) }
                    .map { user ->
                        val contact = usersWhoAreContacts[user]
                        val isChecked: Boolean = invitees.any { it.user == user }
                        CheckableInviteItem(contact, user, members.contains(user), isChecked) as Group
                    }
        }.combineLatest(currentUser, search)
    }

    private fun getCurrentUsersFriends(): Flowable<List<User>> {
        return currentUser.flatMap { getFriends(it.id) }
    }

    private val facebookFriends: Flowable<List<User>> by lazy {
        Flowable.fromCallable {
            userService.getFacebookFriends(Empty())
        }.map { it.users.map { User(it) } }.replay(1).autoConnect()
    }


    fun submitButtonEnabled(): Flowable<Boolean> {
        return invitees.map { it.isNotEmpty() }
    }

    private fun getFilteredContacts(): Flowable<List<Contact>> {
        return search.combineLatest(localContacts).map { (search, contacts) -> contacts.filter { it.displayName.contains(search, ignoreCase = true) } }
    }

    fun getContactItemsAndInfo(circleId: String): Flowable<Triple<List<Group>, CurrentUser, String>> {
        return getFilteredContacts().combineLatest(
                usersWhoAreContacts,
                invitees,
                getMembers(circleId)
        ).map { (contacts, usersWhoAreContacts, invitees, circleMembers) ->
            contacts.map { contact ->
                val user = usersWhoAreContacts.keys.firstOrNull { usersWhoAreContacts[it] == contact }
                val isCircleMember = circleMembers.contains(user)
                val isChecked = invitees.any { it.contact == contact }
                CheckableInviteItem(contact, user, isCircleMember, isChecked)
            } as List<Group>
        }.combineLatest(currentUser, search)
    }

    fun inviteeSelected(contact: Contact?, user: User?) {
        inviteeSelected(Invitee(contact, user))
    }

    private fun inviteeSelected(invitee: Invitee) {
        val invitees = selectedInviteesEmitter.value.toMutableList()
        invitees.apply {
            if (contains(invitee)) this -= invitee else {
                // Add newest selected contact to front of queue so they're always visible
                this.add(0, invitee)
            }
        }
        selectedInviteesEmitter.accept(invitees)
    }

    fun addInviteesToCircle(circleId: String): Completable {
        val invitees = this.selectedInviteesEmitter.value
        val userIds = mutableListOf<String>()
        val phoneNumbers = mutableListOf<String>()
        invitees.forEach {
            if (it.user != null) userIds.add(it.user.id)
            else if (it.contact != null) phoneNumbers.addAll(it.contact.phones.values)
        }

        return Completable.fromCallable {
            circleService.addMembers(MembersRequest.Builder()
                    .groupId(circleId)
                    .userIds(userIds)
                    .phoneNumbers(phoneNumbers)
                    .build()
            )
        }

    }

}