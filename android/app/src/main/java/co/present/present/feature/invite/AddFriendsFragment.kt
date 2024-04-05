package co.present.present.feature.invite

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.LinearLayoutManager
import co.present.present.BaseFragment
import co.present.present.R
import co.present.present.ViewModelFactory
import co.present.present.extensions.applyCompletableSchedulers
import co.present.present.extensions.applyFlowableSchedulers
import co.present.present.extensions.setVisible
import co.present.present.feature.common.OnUserAddFriendListener
import co.present.present.model.CurrentUser
import co.present.present.model.User
import com.xwray.groupie.Group
import com.xwray.groupie.GroupAdapter
import com.xwray.groupie.kotlinandroidextensions.ViewHolder
import io.reactivex.Flowable
import io.reactivex.rxkotlin.addTo
import io.reactivex.rxkotlin.subscribeBy
import kotlinx.android.synthetic.main.fragment_add_contacts_to_circle.*
import javax.inject.Inject

abstract class AddFriendsFragment: BaseFragment(), SearchFriendsEmptyView.InviteButtonClickListener,
    OnUserAddFriendListener, OnContactAddFriendListener {

    private val TAG = javaClass.simpleName

    @Inject lateinit var viewModelFactory: ViewModelFactory
    @Inject lateinit var viewModel: AddFriendsViewModel
    private val adapter = GroupAdapter<ViewHolder>()
    private val empty by lazy { view!!.findViewById<SearchFriendsEmptyView>(R.id.empty)}

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        activityComponent.inject(this)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_add_contacts_to_circle, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        viewModel = ViewModelProviders.of(baseActivity, viewModelFactory).get(AddFriendsViewModel::class.java)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        recyclerView.adapter = adapter
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
    }

    override fun onStart() {
        super.onStart()
        getItemsAndInfo().compose(applyFlowableSchedulers())
                .subscribeBy(
                        onError = { Log.e(TAG, "error", it) },
                        onNext = { (items, currentUser, search) ->
                            adapter.update(items)
                            empty.bind(currentUser, search, this)
                            recyclerView.setVisible(adapter.itemCount != 0)
                            empty.setVisible(adapter.itemCount == 0)
                        }
                ).addTo(disposable)
    }

    abstract fun getItemsAndInfo(): Flowable<Triple<List<Group>, CurrentUser, String>>

    override fun onUserAddFriendClicked(item: Any, user: User, currentUser: CurrentUser?) {
            viewModel.changeUserFriendship(user, currentUser!!.id).compose(applyCompletableSchedulers())
                    .subscribeBy(
                            onError = {},
                            onComplete = {}
                    ).addTo(disposable)
    }

    override fun onContactAddFriendClicked(item: Any, contact: Contact) {
        viewModel.contactRequested(contact)
    }

    override fun onAddFriendsButtonClicked(currentUser: CurrentUser?) {
        baseActivity.launchAddFriends()
    }

}

class AddFriendsFacebookFragment: AddFriendsFragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_add_friends_to_circle, container, false)
    }

    override fun getItemsAndInfo(): Flowable<Triple<List<Group>, CurrentUser, String>> {
        return viewModel.getFacebookFriendsItemsAndInfo(this)
    }

}

class AddFriendsContactsFragment: AddFriendsFragment() {
    private val TAG = javaClass.simpleName

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_add_contacts_to_circle, container, false)
    }

    override fun getItemsAndInfo(): Flowable<Triple<List<Group>, CurrentUser, String>> {
        return viewModel.getContactItemsAndInfo(this, this)
    }
}