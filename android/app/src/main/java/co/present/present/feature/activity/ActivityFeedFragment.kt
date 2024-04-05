package co.present.present.feature.activity

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
import co.present.present.extensions.applySingleSchedulers
import co.present.present.extensions.setVisible
import co.present.present.extensions.snackbar
import co.present.present.extensions.start
import co.present.present.feature.MainActivity
import co.present.present.feature.detail.CircleActivity
import co.present.present.feature.profile.UserProfileActivity
import com.xwray.groupie.GroupAdapter
import com.xwray.groupie.Item
import com.xwray.groupie.OnItemClickListener
import com.xwray.groupie.ViewHolder
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.rxkotlin.subscribeBy
import kotlinx.android.synthetic.main.fragment_activity_feed.*
import present.proto.EventResponse
import javax.inject.Inject

class ActivityFeedFragment : BaseFragment(), OnItemClickListener, MainActivity.Scrollable {

    override fun scrollToTop() {
        recyclerView.smoothScrollToPosition(0)
    }

    private val TAG = javaClass.simpleName

    @Inject lateinit var viewModelFactory: ViewModelFactory
    private val adapter = GroupAdapter<ViewHolder>().apply { setOnItemClickListener(this@ActivityFeedFragment) }

    fun performInjection() {
        activityComponent.inject(this)
    }

    private lateinit var viewModel: ActivityViewModel

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_activity_feed, container, false)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        performInjection()
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        viewModel = ViewModelProviders.of(baseActivity, viewModelFactory).get(ActivityViewModel::class.java)

        recyclerView.layoutManager = LinearLayoutManager(context)
        recyclerView.adapter = adapter

        disposable += viewModel.getNotifications().compose(applySingleSchedulers())
                .doOnSubscribe {
                    spinner.visibility = View.VISIBLE
                    swipeRefreshLayout.isEnabled = false
                }
                .subscribeBy(
                        onError = {
                            Log.e(TAG, "Couldn't load activity feed.", it)
                            swipeRefreshLayout.isEnabled = true
                            spinner.visibility = View.GONE
                            snackbar("Couldn't load activity feed.")
                        },
                        onSuccess = { items ->
                            swipeRefreshLayout.isEnabled = true
                            spinner.visibility = View.GONE
                            adapter.update(items)

                            recyclerView.setVisible(items.isNotEmpty())
                            emptyText.setVisible(items.isEmpty())
                        }
                )
        swipeRefreshLayout.setOnRefreshListener { softRefresh() }
    }

    /**
     * Refreshes from network in background; shows a snackbar in both success and error cases.
     */
    private fun softRefresh() {
        disposable += viewModel.getNotifications().compose(applySingleSchedulers())
                .subscribeBy(
                        onError = {
                            Log.e(TAG, "Couldn't refresh activity feed.", it)
                            snackbar("Couldn't refresh activity feed.")
                            swipeRefreshLayout.isRefreshing = false
                        },
                        onSuccess = { items ->
                            adapter.update(items)
                            snackbar("Activity feed is up to date.")
                            swipeRefreshLayout.isRefreshing = false
                        }
                )
    }

    override fun onItemClick(item: Item<*>, view: View) {
        when (item) {
            is ActivityFeedItem -> launchTarget(item.eventResponse)
        }
    }

    private fun launchTarget(eventResponse: EventResponse) {
        // We'll never get just ids, but full objects, and don't worry about 'message'-- Source: Bob
        eventResponse.defaultTarget.apply {
            if (comment != null) {
                launchCircle(comment.groupId, comment.uuid)
            } else if (group != null) {
                launchCircle(group.uuid)
            } else if (user != null) {
                baseActivity.start(UserProfileActivity.newIntent(baseActivity, user.id))
            }
        }
    }

    private fun launchCircle(circleId: String, commentId: String? = null) {
        baseActivity.start(CircleActivity.newIntent(baseActivity, circleId, commentId))
    }
}