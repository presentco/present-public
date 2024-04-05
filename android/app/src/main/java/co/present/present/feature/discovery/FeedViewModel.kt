package co.present.present.feature.discovery

import android.app.Application
import android.util.Log
import co.present.present.analytics.AmplitudeEvents
import co.present.present.analytics.AmplitudeKeys
import co.present.present.analytics.Analytics
import co.present.present.config.FeatureDataProvider
import co.present.present.db.CircleDao
import co.present.present.feature.BottomNavState
import co.present.present.feature.common.item.OnCircleJoinClickListener
import co.present.present.feature.common.viewmodel.GetCurrentUser
import co.present.present.feature.detail.info.JoinCircle
import co.present.present.location.LocationDataProvider
import co.present.present.model.Category
import co.present.present.model.Space
import com.jakewharton.rxrelay2.BehaviorRelay
import com.xwray.groupie.Group
import io.reactivex.BackpressureStrategy
import io.reactivex.Flowable
import io.reactivex.rxkotlin.combineLatest
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.subjects.BehaviorSubject
import io.reactivex.subjects.PublishSubject
import present.proto.Gender
import javax.inject.Inject
import javax.inject.Named


/**
 * Combines a default-state webview (Feed), a search empty state webview (Explore), and filtering
 * by category and/or search.
 */
class FeedViewModel @Inject constructor(val circleDao: CircleDao,
                                        locationDataProvider: LocationDataProvider,
                                        featureDataProvider: FeatureDataProvider,
                                        joinCircle: JoinCircle,
                                        getCurrentUser: GetCurrentUser,
                                        @Named("feedSearch") val searchable: Searchable,
                                        refreshCircles: RefreshCircles,
                                        val analytics: Analytics,
                                        application: Application)
    : CirclesViewModel(locationDataProvider, featureDataProvider, joinCircle, getCurrentUser, refreshCircles, application), Searchable by searchable {
    private val TAG = javaClass.simpleName

    fun getItems(onCircleJoinClickListener: OnCircleJoinClickListener): Flowable<List<Group>> {
        return circleDao.getNearbyCircles()
                .filter { it.isNotEmpty() }
                .combineLatest(getSearchTerm(), getCategory())
                .compose(toItems(onCircleJoinClickListener, featureDataProvider))
    }

    fun shouldHideWomenOnlyContent(state: BottomNavState): Boolean {
        return state.hidWomenOnlyTab || (state is BottomNavState.LoggedIn &&
                (if (featureDataProvider.shouldOverrideGender) featureDataProvider.overrideGender == Gender.MAN.value
                        else state.currentUser.isMan))
    }

    private val searchModeSubject: BehaviorSubject<Boolean> = BehaviorSubject.createDefault(false)

    private val stateSubject: BehaviorRelay<State> by lazy {
        BehaviorRelay.createDefault<State>(State.Landing)
    }

    private val stateFlowable by lazy {
                searchModeSubject.toFlowable(BackpressureStrategy.LATEST).combineLatest(
                        getSearchTerm(),
                        locationDataProvider.getLocationOptional())
                .map { (inSearchMode, searchTerm, locationOptional) ->
                    if (locationOptional.value == null) State.LocationPrompt
                    else if (!inSearchMode) State.Landing
                    else if (searchTerm.isEmpty()) State.Search
                    else State.Filtered
                }
                .doOnSubscribe { analytics.log(AmplitudeEvents.HOME_FEED_VIEW, AmplitudeKeys.SPACE_ID, Space.everyoneId) }
                .subscribeBy(
                        onError = {},
                        onNext = {
                            stateSubject.accept(it)
                            logAnalytics(it)
                        })
        stateSubject.toFlowable(BackpressureStrategy.LATEST)
    }

    private fun logAnalytics(it: State) {
        if (it == State.Filtered) {
            analytics.log(AmplitudeEvents.HOME_SEARCH_VIEW)
        } else if (it == State.Landing) {
            analytics.log(AmplitudeEvents.HOME_EXPLORE_VIEW)
        }
    }

    fun getState(): Flowable<State> {
        return stateFlowable
    }

    val state: State get() = stateSubject.value

    val canGoBack: Boolean
        get() {
            Log.d(TAG, "canGoBack: ${searchModeSubject.value}")
            return searchModeSubject.value
        }

    fun goBack() = clearSearchMode()

    fun setSearchMode() {
        searchModeSubject.onNext(true)
    }

    fun clearSearchMode() {
        searchModeSubject.onNext(false)

        // Clear search and category for next time we launch search mode
        searchable.searchChanged("")
        categorySubject.onNext(Category.NONE)
    }


    val categorySubject: BehaviorSubject<String> = BehaviorSubject.createDefault(Category.NONE)

    fun getCategory(): Flowable<String> = categorySubject.toFlowable(BackpressureStrategy.LATEST)

    fun setCategory(category: String) {
        categorySubject.onNext(category)
    }

    fun scrollToTop() {
        scrollToTopSubject.onNext(true)
    }

    val scrollToTopSubject = PublishSubject.create<Boolean>()

    fun getScrollToTopEvents(): Flowable<Boolean> {
        return scrollToTopSubject.toFlowable(BackpressureStrategy.LATEST)
    }

    fun onHomeTabSelected() {
        logAnalytics(state)
    }

    enum class State {
        LocationPrompt,

        // App landing page: feed view
        Landing,

        // "Explore" view, search empty state
        Search,

        // Filtered by category or search term
        Filtered
    }

}
