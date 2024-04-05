package co.present.present.feature.discovery

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.LinearLayoutManager
import co.present.present.BaseFragment
import co.present.present.R
import co.present.present.ViewModelFactory
import co.present.present.config.FeatureDataProvider
import co.present.present.extensions.*
import co.present.present.feature.MainActivity
import co.present.present.feature.common.item.OnCircleJoinClickListener
import co.present.present.location.LocationDataProvider
import com.google.android.material.snackbar.Snackbar
import com.xwray.groupie.Group
import com.xwray.groupie.GroupAdapter
import com.xwray.groupie.OnItemClickListener
import com.xwray.groupie.kotlinandroidextensions.ViewHolder
import io.reactivex.Flowable
import io.reactivex.rxkotlin.addTo
import io.reactivex.rxkotlin.subscribeBy
import kotlinx.android.synthetic.main.fragment_display_circles.*
import javax.inject.Inject

/**
 * Generic fragment to a list of circles.
 */
abstract class CircleListFragment : BaseFragment(), OnItemClickListener, MainActivity.Scrollable {

    private val TAG = javaClass.simpleName

    @Inject lateinit var locationDataProvider: LocationDataProvider
    @Inject lateinit var featureDataProvider: FeatureDataProvider
    @Inject lateinit var viewModelFactory: ViewModelFactory
    lateinit var viewModel: CirclesViewModel
    private val adapter = GroupAdapter<ViewHolder>().apply {
        setOnItemClickListener(this@CircleListFragment)
    }
    private lateinit var circleJoinHandler: CircleJoinHandler

    protected abstract fun performInjection()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        performInjection()
        viewModel = ViewModelProviders.of(baseActivity, viewModelFactory).get(CirclesViewModel::class.java)
        circleJoinHandler = CircleJoinHandler(viewModel, activity as AppCompatActivity)
    }

    protected abstract fun getItems(onCircleJoinClickListener: OnCircleJoinClickListener): Flowable<List<Group>>

    override fun onStart() {
        super.onStart()

        getItems(circleJoinHandler)
                .compose(applyFlowableSchedulers())
                .doOnSubscribe {
                    spinner.visibility = View.VISIBLE
                }.subscribeBy(
                        onError = { e ->
                            Log.e(TAG, "Error getting circles and location", e)
                            if (spinner.visibility == View.VISIBLE) {
                                spinner.visibility = View.GONE
                                refreshLayout.isRefreshing = false
                                errorView.visibility = View.VISIBLE
                            }
                        },
                        onNext = { items ->
                            spinner.visibility = View.GONE
                            refreshLayout.isRefreshing = false
                            adapter.update(items)
                            configureEmptyView(adapter.itemCount)
                        }
                ).addTo(disposable)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_display_circles, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        cardList.setHasFixedSize(true)
        cardList.layoutManager = LinearLayoutManager(context)
        cardList.adapter = adapter

        refreshLayout.setOnRefreshListener { softRefresh() }
    }

    /**
     * Refreshes from network in background; shows a snackbar in both success and error cases.
     */
    private fun softRefresh() {
        // TODO: This shouldn't refresh *everything*. It should be specific to the list of circles displayed
        viewModel.refreshCircles().compose(applyCompletableSchedulers())
                .subscribeBy(
                        onError = { e ->
                            Log.d(TAG, "Error refreshing from network", e)
                            snackbar("Couldn't refresh circles.", Snackbar.LENGTH_LONG)
                        },
                        onComplete = {
                            snackbar("All circles successfully updated", Snackbar.LENGTH_LONG)
                        }
                )
    }

    private fun configureEmptyView(numCircles: Int) {
        errorView.visibility = View.GONE
        if (numCircles <= 0) {
            emptyView.show()
            cardList.hide()
            refreshLayout.hide()
        } else {
            emptyView.hide()
            cardList.show()
            refreshLayout.show()
        }
    }

    override fun scrollToTop() {
        cardList.smoothScrollToPosition(0)
    }
}
