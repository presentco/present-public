package co.present.present.feature.discovery

import android.app.Application
import co.present.present.R
import co.present.present.config.FeatureDataProvider
import co.present.present.db.UserDao
import co.present.present.extensions.string
import co.present.present.feature.GetJoined
import co.present.present.feature.common.item.SmallCircleItem
import co.present.present.feature.common.viewmodel.GetCurrentUser
import co.present.present.feature.detail.info.JoinCircle
import co.present.present.location.LocationDataProvider
import com.xwray.groupie.Group
import io.reactivex.Flowable
import io.reactivex.rxkotlin.combineLatest
import javax.inject.Inject


open class JoinedCirclesViewModel @Inject constructor(refreshCircles: RefreshCircles,
                                                      locationDataProvider: LocationDataProvider,
                                                      featureDataProvider: FeatureDataProvider,
                                                      joinCircle: JoinCircle,
                                                      getCurrentUser: GetCurrentUser,
                                                      val userDao: UserDao,
                                                      getJoined: GetJoined,
                                                      searchable: Searchable,
                                                      application: Application)
    : CirclesViewModel(locationDataProvider, featureDataProvider, joinCircle, getCurrentUser, refreshCircles, application),
        Searchable by searchable, GetJoined by getJoined {
    private val TAG = javaClass.simpleName

    fun getItems(userId: String): Flowable<List<Group>> {
        return getJoinedCircles(userId).combineLatest(currentUserOptional)
                .map { (circles, currentUserOptional) ->
                    val currentUser = currentUserOptional.value
                    circles.map { circle ->
                        SmallCircleItem(currentUser, circle, listener = null)
                    }
                }
    }

    fun getTitle(userId: String): Flowable<String> {
        return userDao.getUser(userId).map { it.firstName }.distinctUntilChanged()
                .map { string(R.string.joined_circles_title_template, it) }
    }
}
