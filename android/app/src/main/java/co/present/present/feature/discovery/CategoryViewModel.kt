package co.present.present.feature.discovery

import android.app.Application
import android.util.Log
import co.present.present.config.FeatureDataProvider
import co.present.present.db.CircleDao
import co.present.present.feature.common.item.OnCircleJoinClickListener
import co.present.present.feature.common.viewmodel.GetCurrentUser
import co.present.present.feature.detail.info.JoinCircle
import co.present.present.location.LocationDataProvider
import com.xwray.groupie.Group
import io.reactivex.BackpressureStrategy
import io.reactivex.Flowable
import io.reactivex.rxkotlin.combineLatest
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.subjects.BehaviorSubject
import javax.inject.Inject


class CategoryViewModel @Inject constructor(val circleDao: CircleDao,
                                            locationDataProvider: LocationDataProvider,
                                            featureDataProvider: FeatureDataProvider,
                                            joinCircle: JoinCircle,
                                            getCurrentUser: GetCurrentUser,
                                            val searchable: Searchable,
                                            refreshCircles: RefreshCircles,
                                            application: Application)
    : CirclesViewModel(locationDataProvider, featureDataProvider, joinCircle, getCurrentUser, refreshCircles, application), Searchable by searchable {
    private val TAG = javaClass.simpleName

    fun getItems(category: String, onCircleJoinClickListener: OnCircleJoinClickListener): Flowable<List<Group>> {
        return circleDao.getNearbyCircles()
                .doOnNext { Log.d(TAG, "Got nearby circles, ${it.size}") }
                .filter { it.isNotEmpty() }
                .combineLatest(getSearchTerm(), Flowable.just(category))
                .compose(toItems(onCircleJoinClickListener, featureDataProvider))
    }

    private val searchModeSubject: BehaviorSubject<Boolean> = BehaviorSubject.createDefault(false)

    private val stateSubject: BehaviorSubject<CategoryViewModel.State> by lazy {
        BehaviorSubject.createDefault<CategoryViewModel.State>(CategoryViewModel.State.Normal)
    }

    private val stateFlowable by lazy {
        searchModeSubject.toFlowable(BackpressureStrategy.LATEST)
                .map { inSearchMode ->
                    if (inSearchMode) CategoryViewModel.State.Search else CategoryViewModel.State.Normal
                }
                .subscribeBy(onError = {}, onNext = { stateSubject.onNext(it) })
        stateSubject.toFlowable(BackpressureStrategy.LATEST)
    }

    fun getState(): Flowable<CategoryViewModel.State> {
        return stateFlowable
    }

    fun goBack() = clearSearchMode()

    fun setSearchMode() {
        searchModeSubject.onNext(true)
    }

    private fun clearSearchMode() {
        searchModeSubject.onNext(false)

        // Clear search for next time we launch search mode
        searchable.searchChanged("")
    }

    sealed class State {
        // App landing page: feed view
        object Normal: State()

        // Search is visible (whether term is empty or not)
        object Search: State()

    }

}


