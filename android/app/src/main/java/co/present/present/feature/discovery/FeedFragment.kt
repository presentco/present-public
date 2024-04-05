package co.present.present.feature.discovery

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProviders
import co.present.present.BaseFragment
import co.present.present.R
import co.present.present.UrlResolverViewModel
import co.present.present.ViewModelFactory
import co.present.present.analytics.AmplitudeEvents
import co.present.present.extensions.*
import co.present.present.feature.*
import co.present.present.location.LocationDataProvider
import io.reactivex.rxkotlin.addTo
import io.reactivex.rxkotlin.subscribeBy
import kotlinx.android.synthetic.main.fragment_feed.*
import javax.inject.Inject


open class FeedFragment : BaseFragment(), MainActivity.Stack, MainActivity.Resettable {

    private val TAG = javaClass.simpleName

    @Inject lateinit var viewModelFactory: ViewModelFactory
    private lateinit var urlResolverViewModel: UrlResolverViewModel
    private val feedViewModel: FeedViewModel by lazy {
        ViewModelProviders.of(requireActivity(), viewModelFactory).get(FeedViewModel::class.java)
    }
    private lateinit var citiesViewModel: CitiesViewModel
    @Inject lateinit var locationDataProvider: LocationDataProvider
    private val onBackClicked = View.OnClickListener { goBack() }
    private val feedTabFragment by lazy { FeedTabFragment() }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_feed, container, false)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        activityComponent.inject(this)
        setHasOptionsMenu(true)

        feedViewModel.refreshNearby().compose(applyCompletableSchedulers())
                .subscribeBy(
                        onError = {
                            Log.e(TAG, "Failed to get circles from network and save -- probably network error", it)
                        },
                        onComplete = {
                            Log.d(TAG, "Successfully updated circles from network and saved to database")
                            // Do nothing -- update will be driven from database
                        }
                ).addTo(disposable)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (childFragmentManager.findFragmentByTag(feedTabFragment.javaClass.simpleName) == null) {
            childFragmentManager.transaction {
                add(R.id.feedTab, feedTabFragment, feedTabFragment.javaClass.simpleName)
            }
        }
    }

    private lateinit var bottomNavViewModel: BottomNavViewModel

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        citiesViewModel = ViewModelProviders.of(baseActivity, viewModelFactory).get(CitiesViewModel::class.java)
        bottomNavViewModel = ViewModelProviders.of(baseActivity, viewModelFactory).get(BottomNavViewModel::class.java)

        searchView.init(feedViewModel, activity as AppCompatActivity, onBackClicked)
    }

    override fun onStart() {
        super.onStart()
        bottomNavViewModel.getBottomNavState().compose(applyFlowableSchedulers())
                .subscribeBy(
                        onError = {},
                        onNext = { state ->
                            Log.d(TAG, "Making new adapter, so recreating feed tab contents: state $state")
                            // TODO: do we still need this?
                            searchIcon.setOnClickListener { onSearchClicked(state) }
                        }
                ).addTo(disposable)
    }

    private fun onSearchClicked(state: BottomNavState) {
        analytics.log(AmplitudeEvents.HOME_TAP_SEARCH)
        if (state is BottomNavState.LoggedOut && state !is BottomNavState.LoggedOutWithLocation) {
            start<SignUpDialogActivity>()
        } else {
            feedViewModel.setSearchMode()
        }
    }

    override fun onResume() {
        super.onResume()

        feedViewModel.getState().compose(applyFlowableSchedulers())
                .subscribeBy(
                        onError = {},
                        onNext = {
                            onStateChanged(it)
                        }
                )

        urlResolverViewModel = ViewModelProviders.of(this, viewModelFactory).get(UrlResolverViewModel::class.java)
    }

    override fun canGoBack() = feedViewModel.canGoBack

    override fun goBack() {
        searchView.hideKeyboard()
        feedViewModel.goBack()
    }

    override fun resetToInitialState() {
        if (canGoBack()) goBack()
        feedViewModel.scrollToTop()
    }

    private fun onStateChanged(it: FeedViewModel.State) {
        when (it) {
            FeedViewModel.State.LocationPrompt, FeedViewModel.State.Landing -> {
                spacesToolbar.show()
                searchView.hide()
                searchIcon.show()
                searchView.hideKeyboard()
            }
            FeedViewModel.State.Search, FeedViewModel.State.Filtered -> {
                spacesToolbar.hide()
                searchView.show()
                searchIcon.hide()
                searchView.setFocus()
            }
        }
    }

    fun performInjection() {
        activityComponent.inject(this)
    }
}
