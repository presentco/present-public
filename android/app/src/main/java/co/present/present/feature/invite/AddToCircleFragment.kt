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
import co.present.present.extensions.applyFlowableSchedulers
import co.present.present.extensions.setVisible
import co.present.present.model.Circle
import co.present.present.model.CurrentUser
import com.xwray.groupie.Group
import com.xwray.groupie.GroupAdapter
import com.xwray.groupie.Item
import com.xwray.groupie.kotlinandroidextensions.ViewHolder
import io.reactivex.Flowable
import io.reactivex.rxkotlin.addTo
import io.reactivex.rxkotlin.subscribeBy
import kotlinx.android.synthetic.main.fragment_add_contacts_to_circle.*
import javax.inject.Inject

abstract class AddToCircleFragment: BaseFragment(), SearchFriendsEmptyView.InviteButtonClickListener {
    private val TAG = javaClass.simpleName

    @Inject lateinit var viewModelFactory: ViewModelFactory
    @Inject lateinit var viewModel: AddToCircleViewModel
    private val adapter = GroupAdapter<ViewHolder>().apply { setOnItemClickListener(this@AddToCircleFragment) }
    private val circleId: String by lazy { arguments!!.getString(Circle.ARG_CIRCLE) }
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
        viewModel = ViewModelProviders.of(baseActivity, viewModelFactory).get(AddToCircleViewModel::class.java)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        recyclerView.adapter = adapter
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
    }

    override fun onStart() {
        super.onStart()
        getItemsAndInfo(circleId).compose(applyFlowableSchedulers())
                .subscribeBy(
                        onError = { Log.e(TAG, "error", it) },
                        onNext = { (items, currentUser, search) ->
                            adapter.update(items)
                            empty.bind(currentUser, search, this)
                            recyclerView.setVisible(items.isNotEmpty())
                            empty.setVisible(items.isEmpty())
                        }
                ).addTo(disposable)
    }

    abstract fun getItemsAndInfo(circleId: String): Flowable<Triple<List<Group>, CurrentUser, String>>

    override fun onItemClick(item: Item<*>, view: View) {
        when (item) {
            is CheckableInviteItem -> {
                viewModel.inviteeSelected(item.contact, item.user)
            }
        }
    }

    override fun onAddFriendsButtonClicked(currentUser: CurrentUser?) {
        baseActivity.launchAddFriends()
    }

}