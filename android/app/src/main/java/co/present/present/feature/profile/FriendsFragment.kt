package co.present.present.feature.profile

import android.app.Application
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.AndroidViewModel
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import co.present.present.BaseFragment
import co.present.present.R
import co.present.present.ViewModelFactory
import co.present.present.db.FriendRelationshipDao
import co.present.present.db.UserDao
import co.present.present.extensions.*
import co.present.present.feature.GetFriendRequests
import co.present.present.feature.SignUpDialogActivity
import co.present.present.feature.common.FriendUser
import co.present.present.feature.common.GetFriends
import co.present.present.feature.common.OnUserAddFriendListener
import co.present.present.feature.common.item.GrayHeaderItem
import co.present.present.feature.common.item.TextItem
import co.present.present.feature.common.item.UserItem
import co.present.present.feature.common.viewmodel.GetCurrentUser
import co.present.present.feature.detail.info.UserRequestItem
import co.present.present.model.*
import co.present.present.view.DeleteTouchCallback
import com.google.android.material.snackbar.Snackbar
import com.xwray.groupie.Group
import com.xwray.groupie.GroupAdapter
import com.xwray.groupie.Item
import com.xwray.groupie.Section
import com.xwray.groupie.groupiex.plusAssign
import com.xwray.groupie.kotlinandroidextensions.ViewHolder
import io.reactivex.Flowable
import io.reactivex.rxkotlin.addTo
import io.reactivex.rxkotlin.combineLatest
import io.reactivex.rxkotlin.subscribeBy
import kotlinx.android.synthetic.main.fragment_community.*
import javax.inject.Inject

open class FriendsFragment : BaseFragment(), OnUserAddFriendListener, UserRequestItem.OnUserApproveListener {
    val TAG = javaClass.simpleName

    val userId: String by lazy { arguments!!.getString(User.USER) }

    lateinit var viewModel: FriendsViewModel
    @Inject
    lateinit var viewModelFactory: ViewModelFactory
    private val touchCallback: DeleteTouchCallback by lazy {
        object : DeleteTouchCallback(requireContext()) {

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val item = adapter.getItem(viewHolder.adapterPosition)
                if (item is UserRequestItem) {
                    onFriendRequestRemoved(item.user)
                }
            }
        }
    }

    private fun onFriendRequestRemoved(user: User) {
        viewModel.removeIncomingFriendRequestLocally(user.id)
                .compose(applyCompletableSchedulers())
                .subscribeBy(
                        onError = { Log.e(TAG, "Error removing friend request locally from DB, this shouldn't happen", it) },
                        onComplete = {
                            // No need to do anything to update the list, the database update
                            // will trigger a UI update.

                            val callback = object: Snackbar.Callback() {
                                override fun onDismissed(transientBottomBar: Snackbar?, event: Int) {
                                    // When dismissed for any reason (manual or timeout), the user
                                    // can't press Undo anymore, so we're good to go to remove this
                                    // friend request on the server.
                                    onFriendRequestRemoveConfirmed(user)
                                }
                            }
                            snackbar(R.string.friend_request_removed,
                                    callback = callback,
                                    actionStringRes = R.string.undo) {
                                onUndoFriendRequestRemoved(user)
                            }
                        })
                .addTo(disposable)
    }

    private fun onFriendRequestRemoveConfirmed(user: User) {
        viewModel.removeIncomingFriendRequestOnServer(user.id)
                .compose(applyCompletableSchedulers())
                .subscribeBy(
                        onError = { Log.e(TAG, "Error removing incoming friend request on server", it) },
                        onComplete = {})
                .addTo(disposable)
    }

    private fun onUndoFriendRequestRemoved(user: User) {
        viewModel.undoRemoveIncomingFriendRequestLocally(user.id)
                .compose(applyCompletableSchedulers())
                .subscribeBy(
                        onError = { Log.e(TAG, "Error undoing removal of friend request locally in DB, this shouldn't happen", it) },
                        onComplete = {
                            // No need to do anything to update the list, the database update
                            // will trigger a UI update and restore the friend request
                        })
                .addTo(disposable)
    }

    private val adapter = GroupAdapter<ViewHolder>().apply {
        setOnItemClickListener(this@FriendsFragment)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_community, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        recyclerView.setHasFixedSize(true)
        recyclerView.layoutManager = LinearLayoutManager(context)
        recyclerView.adapter = adapter
        ItemTouchHelper(touchCallback).attachToRecyclerView(recyclerView)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        activityComponent.inject(this)
        viewModel = viewModelFactory.create(FriendsViewModel::class.java)
    }

    override fun onApproveClicked(item: Item<*>, user: User, currentUser: CurrentUser) {
        viewModel.approveIncomingFriendRequest(user.id).compose(applyCompletableSchedulers())
                .subscribeBy(
                        onError = { snackbar(R.string.network_error) },
                        onComplete = { } // Do nothing; UI will update automatically
                ).addTo(disposable)
    }

    override fun onResume() {
        super.onResume()
        viewModel.getItems(userId, this, this).compose(applyFlowableSchedulers())
                .subscribeBy(
                        onError = { Log.d(TAG, "error", it) },
                        onNext = {
                            adapter.update(it)
                        }
                ).addTo(disposable)
    }

    override fun onUserAddFriendClicked(item: Any, user: User, currentUser: CurrentUser?) {
        if (currentUser == null) {
            start<SignUpDialogActivity>()
        } else {
            viewModel.changeUserFriendship(user, currentUser.id).compose(applyCompletableSchedulers())
                    .subscribeBy(
                            onError = {},
                            onComplete = {}
                    ).addTo(disposable)
        }
    }

    companion object {
        fun newInstance(userId: String): FriendsFragment {
            return FriendsFragment().apply {
                arguments = Bundle().apply {
                    putString(User.USER, userId)
                }
            }
        }
    }
}

class FriendsViewModel @Inject constructor(val userDao: UserDao,
                                           val getCurrentUser: GetCurrentUser,
                                           val friendshipDao: FriendRelationshipDao,
                                           val getFriendRequests: GetFriendRequests,
                                           val getFriends: GetFriends,
                                           friendUser: FriendUser,
                                           application: Application)
    : AndroidViewModel(application), GetCurrentUser by getCurrentUser, FriendUser by friendUser,
        GetFriendRequests by getFriendRequests, GetFriends by getFriends {

    fun getUserAndCurrentUser(userId: String): Flowable<Pair<CurrentUser, User>> {
        return currentUser.combineLatest(userDao.getUser(userId))
    }

    fun getFriendRequests(userId: String): Flowable<List<User>> {
        return currentUser.flatMap {
            if (it.id == userId) getIncomingFriendRequests()
            else Flowable.just(listOf())
        }
    }

    fun getFriendRelationships(): Flowable<List<FriendRelationship>> {
        return currentUser.flatMap { friendshipDao.getRelationships(it.id) }
    }

    fun getItems(userId: String, onUserAddFriendListener: OnUserAddFriendListener,
                 onUserApproveListener: UserRequestItem.OnUserApproveListener): Flowable<List<Group>> {
        return getFriendRelationships().combineLatest(
                getFriends(userId),
                getFriendRequests(userId),
                currentUser
        )
                .map { (currentUserRelationships, friends, friendRequests, currentUser) ->
                    mutableListOf<Group>().apply {
                        val friendsSection = Section().apply {
                            addAll(friends.map { user ->
                                UserItem(user,
                                        currentUser,
                                        currentUserRelationships.find { it.otherUserId == user.id }?.friendshipState ?: FriendshipState.None,
                                        onUserAddFriendListener)
                            })
                            setPlaceholder(TextItem(string(R.string.no_friends)))
                        }

                        if (currentUser.id == userId) {
                            add(Section(GrayHeaderItem(R.string.pending_friend_requests)).apply {
                                this += friendRequests.map { UserRequestItem(it, currentUser, false, onUserApproveListener) }
                                setPlaceholder(TextItem(string(R.string.no_friend_requests)))
                            })

                            friendsSection.setHeader(GrayHeaderItem(R.string.friends))
                        }

                        add(friendsSection)
                    }
                }
    }

    fun getTitle(userId: String): Flowable<String> {
        return getUserAndCurrentUser(userId)
                .map { (currentUser, user) ->
                    if (user.isAlso(currentUser)) {
                        string(R.string.current_user_friends_title_template)
                    } else {
                        string(R.string.friends_title_template, user.firstName)
                    }
                }.distinctUntilChanged()
    }
}