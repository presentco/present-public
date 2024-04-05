package co.present.present.feature.discovery

import android.app.Application
import android.location.Location
import android.util.Log
import co.present.present.config.FeatureDataProvider
import co.present.present.db.CircleDao
import co.present.present.db.Database
import co.present.present.db.SpacesDao
import co.present.present.extensions.newLocation
import co.present.present.extensions.zipWith
import co.present.present.feature.common.viewmodel.GetCurrentUser
import co.present.present.location.LocationDataProvider
import co.present.present.model.Circle
import co.present.present.model.Space
import co.present.present.service.rpc.CircleService
import co.present.present.service.rpc.getJoinedCircles
import co.present.present.service.rpc.getNearbyCircles
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.schedulers.Schedulers

class RefreshCirclesImpl(val circleDao: CircleDao,
                         val database: Database,
                         val circleService: CircleService,
                         val featureDataProvider: FeatureDataProvider,
                         val locationDataProvider: LocationDataProvider,
                         val spacesDao: SpacesDao,
                         val getCurrentUser: GetCurrentUser,
                         val application: Application): RefreshCircles, GetCurrentUser by getCurrentUser {
    private val TAG = javaClass.simpleName

    override fun clearAndRefreshCircles(): Completable {
        return Single.fromCallable{ circleDao.clear() }.subscribeOn(Schedulers.newThread())
                .flatMapCompletable { refreshCircles() }
    }

    override fun refreshCircles(): Completable {
        return refreshJoined().mergeWith(refreshNearby())
    }

    override fun clearAndRefreshNearbyCircles(): Completable {
        return Single.fromCallable{ circleDao.clearNonJoined() }.subscribeOn(Schedulers.newThread())
                .flatMapCompletable { refreshNearby() }
    }

    override fun refreshNearby(): Completable {
        return getNearbyLocationForServer().zipWith(spacesDao.getSpaces().firstOrError(), currentUserOptional.firstOrError())
                .flatMapCompletable { (location, spaces, currentUserOptional) ->
                    val spacesToFetch = if (featureDataProvider.canViewWomenOnly(currentUserOptional.value)) spaces else listOf(Space.Everyone)

                    Observable.fromIterable(spacesToFetch).flatMapSingle { space -> refreshNearby(space, location) }
                            .reduce { list1, list2 -> list1.toMutableList().apply { addAll(list2) }}
                            .flatMapCompletable { nearbyCircles -> database.persistNearby(nearbyCircles) }
                }
    }

    private fun refreshNearby(space: Space, location: Location): Single<List<Circle>> {
        return circleService.getNearbyCircles(space.id, location)
                .doOnSubscribe { Log.d(TAG, "Fetching nearby circles for space $space") }
                .subscribeOn(Schedulers.newThread())
    }

    override fun refreshJoined(): Completable {
        return currentUserOptional.firstOrError().flatMap {
            // If the user isn't logged in, don't try to hit the server.
            if (it.value == null) Single.just(listOf<Circle>())

            circleService.getJoinedCircles().subscribeOn(Schedulers.newThread())
        }
                .map { joinedCircles ->
                    Log.d(TAG, "Refreshing joined circles, got ${joinedCircles.size} circles")
                    circleDao.insertCircles(joinedCircles)
                }.toCompletable()
    }

    /**
     * Pass the user's selected location, or the actual location, to get nearby groups
     */
    private fun getNearbyLocationForServer(): Single<Location> {
        return if (featureDataProvider.overrideLocation != null) {
            Single.just(newLocation().apply { longitude = featureDataProvider.overrideLongitude; latitude = featureDataProvider.overrideLatitude })
        } else {
            locationDataProvider.getLocation(application)
        }
    }
}