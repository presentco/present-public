package co.present.present.feature

import android.content.SharedPreferences
import android.location.Location
import androidx.lifecycle.ViewModel
import co.present.present.analytics.Analytics
import co.present.present.db.CircleDao
import co.present.present.db.FriendRelationshipDao
import co.present.present.extensions.Optional
import co.present.present.extensions.preferences
import co.present.present.feature.common.viewmodel.GetCurrentUser
import co.present.present.feature.onboarding.OnboardingDataProvider
import co.present.present.location.LocationDataProvider
import co.present.present.model.CurrentUser
import io.reactivex.BackpressureStrategy
import io.reactivex.Flowable
import io.reactivex.rxkotlin.combineLatest
import io.reactivex.subjects.BehaviorSubject
import present.proto.GroupService
import javax.inject.Inject

class BottomNavViewModel @Inject constructor(getCurrentUser: GetCurrentUser,
                                             val circleDao: CircleDao,
                                             val friendRelationshipDao: FriendRelationshipDao,
                                             val groupService: GroupService,
                                             val locationDataProvider: LocationDataProvider,
                                             val onboardingDataProvider: OnboardingDataProvider,
                                             val sharedPreferences: SharedPreferences,
                                             val analytics: Analytics)
    : ViewModel(), GetCurrentUser by getCurrentUser {
    private val TAG = javaClass.simpleName

    private var notAWoman: Boolean by preferences(sharedPreferences, false)
    private val notAWomanSubject = BehaviorSubject.createDefault(notAWoman)

    fun setNotAWoman() {
        notAWoman = true
        notAWomanSubject.onNext(true)
    }

    fun clear() {
        notAWoman = false
    }

    var selectedTabIndex: Int = 0

    fun getProfileBadge(): Flowable<Int> {
        return currentUserOptional.flatMap {
            circleDao.getBadgedCircles(it.value?.id ?: "")
                    .map { it.size }
                    .combineLatest(getCurrentUserIncomingFriendRequests())
                    .map { it.first + it.second }
        }
    }

    private fun getCurrentUserIncomingFriendRequests(): Flowable<Int> {
        return currentUserOptional.flatMap {
            if (it.value == null) Flowable.just(0)
            else friendRelationshipDao.getInboundRequestCount(it.value.id)
        }
    }

    val bottomNavStateSubject = BehaviorSubject.create<BottomNavState>().apply {
        getBottomNavStateInternal().toObservable().subscribe(this)
    }

    private fun getBottomNavStateInternal(): Flowable<BottomNavState> {
        return getLocationOptional().combineLatest(currentUserOptional, notAWomanSubject.toFlowable(BackpressureStrategy.LATEST))
                .map { (locationOptional, currentUserOptional, notAWoman) ->
                    when {
                        locationOptional.value == null -> BottomNavState.LoggedOut(notAWoman)
                        currentUserOptional.value == null -> BottomNavState.LoggedOutWithLocation(locationOptional.value, notAWoman)
                        else -> BottomNavState.LoggedIn(currentUserOptional.value, locationOptional.value, notAWoman)
                    }
                }.doOnNext {
                    analytics.setUserProperties(locationPermission = it !is BottomNavState.LoggedOut)
                    analytics.setUser((it as? BottomNavState.LoggedIn)?.currentUser)
                }
    }

    fun isLoggedIn(): Boolean {
        return bottomNavStateSubject.hasValue() && bottomNavStateSubject.value is BottomNavState.LoggedIn
    }

    fun getBottomNavState(): Flowable<BottomNavState> {
        return bottomNavStateSubject.toFlowable(BackpressureStrategy.LATEST)
    }

    private fun getLocationOptional(): Flowable<Optional<Location>> {
        return locationDataProvider.getLocationOptional()
    }

}

sealed class BottomNavState(val hidWomenOnlyTab: Boolean) {
    class LoggedIn(val currentUser: CurrentUser, val location: Location, hidWomenOnlyTab: Boolean) : BottomNavState(hidWomenOnlyTab) {
        override fun toString(): String {
            return "LoggedIn[currentUser: $currentUser, location: , hidWomenOnlyTab: $hidWomenOnlyTab]"
        }
    }
    class LoggedOutWithLocation(val location: Location, hidWomenOnlyTab: Boolean) : LoggedOut(hidWomenOnlyTab) {
        override fun toString(): String {
            return "LoggedOutWithLocation[location: , hidWomenOnlyTab: $hidWomenOnlyTab]"
        }
    }
    open class LoggedOut(hidWomenOnlyTab: Boolean) : BottomNavState(hidWomenOnlyTab) {
        override fun toString(): String {
            return "LoggedOut[hidWomenOnlyTab: $hidWomenOnlyTab]"
        }
    }

    fun hidWomanOnlyTabChanged(other: BottomNavState): Boolean {
        return other.hidWomenOnlyTab != hidWomenOnlyTab
    }
}
