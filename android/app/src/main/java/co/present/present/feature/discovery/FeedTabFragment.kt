package co.present.present.feature.discovery

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.ViewModelProviders
import co.present.present.BaseFragment
import co.present.present.R
import co.present.present.ViewModelFactory
import co.present.present.extensions.applyFlowableSchedulers
import co.present.present.extensions.setVisible
import co.present.present.extensions.transaction
import co.present.present.feature.CitiesViewModel
import co.present.present.location.BaseLocationPermissionFragment
import io.reactivex.rxkotlin.addTo
import io.reactivex.rxkotlin.subscribeBy
import kotlinx.android.synthetic.main.fragment_feed_tab.*
import javax.inject.Inject


open class FeedTabFragment : BaseFragment() {

    private val TAG = javaClass.simpleName

    @Inject lateinit var viewModelFactory: ViewModelFactory
    private lateinit var feedViewModel: FeedViewModel
    private lateinit var citiesViewModel: CitiesViewModel

    private val circlesFragment by lazy { FeedFilterFragment() }
    private val locationPromptFragment by lazy { BaseLocationPermissionFragment() }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_feed_tab, container, false)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        activityComponent.inject(this)
        setHasOptionsMenu(true)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        explore.initiate(baseActivity)

        if (childFragmentManager.findFragmentByTag(circlesFragment.javaClass.simpleName) == null) {
            childFragmentManager.transaction {
                add(R.id.filter, circlesFragment, circlesFragment.javaClass.simpleName)
                add(R.id.locationPrompt, locationPromptFragment, locationPromptFragment.javaClass.simpleName)
            }
        }
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        citiesViewModel = ViewModelProviders.of(baseActivity, viewModelFactory).get(CitiesViewModel::class.java)
        feedViewModel = ViewModelProviders.of(baseActivity, viewModelFactory).get(FeedViewModel::class.java)
    }

    override fun onStart() {
        super.onStart()

        citiesViewModel.getSelectedCity().distinctUntilChanged().compose(applyFlowableSchedulers())
                .subscribeBy(
                        onError = { e -> Log.e(TAG, "Error in getSelectedCity()", e) },
                        onNext = { hardRefreshWebviews() }
                ).addTo(disposable)

        feedViewModel.getState().compose(applyFlowableSchedulers())
                .subscribeBy(
                        onError = {},
                        onNext = { onStateChanged(it) }
                ).addTo(disposable)

        feedViewModel.getScrollToTopEvents().subscribeBy(
                onError = {},
                onNext = { scrollWebviewsToTop() }
        ).addTo(disposable)
    }

    private fun scrollWebviewsToTop() {
        explore.scrollToTop()
    }

    private fun hardRefreshWebviews() {
        explore.hardRefresh()
    }

    private fun showOnly(view: View) {
        listOf(explore, filter, locationPrompt).forEach {
            it.setVisible(it == view)
        }
    }

    private fun onStateChanged(it: FeedViewModel.State) {
        when (it) {
            FeedViewModel.State.LocationPrompt -> {
                showOnly(locationPrompt)
            }
            FeedViewModel.State.Landing -> {
                showOnly(explore)
            }
            FeedViewModel.State.Search -> {
                showOnly(explore)
            }
            FeedViewModel.State.Filtered -> {
                showOnly(filter)
                circlesFragment.scrollToTop()
            }
        }
    }

    fun performInjection() {
        activityComponent.inject(this)
    }
}
