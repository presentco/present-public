package co.present.present.feature

import androidx.lifecycle.ViewModel
import android.content.SharedPreferences
import android.util.Log
import co.present.present.config.FeatureDataProvider
import co.present.present.db.CityDao
import co.present.present.extensions.Optional
import co.present.present.feature.discovery.RefreshCircles
import co.present.present.model.City
import co.present.present.service.rpc.getCities
import io.reactivex.BackpressureStrategy
import io.reactivex.Flowable
import io.reactivex.rxkotlin.combineLatest
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.PublishSubject
import present.proto.GroupService
import javax.inject.Inject


class CitiesViewModel @Inject constructor(val groupService: GroupService,
                                          val cityDao: CityDao,
                                          val featureDataProvider: FeatureDataProvider,
                                          val preferences: SharedPreferences,
                                          refreshCircles: RefreshCircles)
    : ViewModel(), RefreshCircles by refreshCircles {

    private val TAG = javaClass.simpleName

    private val citySubject = PublishSubject.create<Optional<String>>()

    fun getCities(): Flowable<List<City>> {
        return cityDao.getCities()
                .doOnNext { cities ->
                    Log.d(TAG, "Fetched ${cities.size} cities from database")
                    if (cities.isEmpty()) getFromNetworkAndSaveAsync()
                }
    }

    fun getCitiesAndSelectedCity(): Flowable<Pair<List<City>, String?>> {
        return getCities()
                .combineLatest(getCurrentCity())
                .map { (first, second) -> Pair(first, second.value) }
    }

    fun getFromNetworkAndSaveAsync() {
        groupService.getCities()
                .doOnSubscribe { Log.d(TAG, "Hitting network to update cities") }
                .map { cityResponses -> cityResponses.map { City(it) } }
                .doOnSuccess { cities ->
                    cityDao.dropTableAndInsertAll(cities)
                }
                .subscribeOn(Schedulers.io())
                .subscribeBy(
                        onError = {
                            Log.e(TAG, "Network error to updating cities", it)
                        },
                        onSuccess = {
                            Log.d(TAG, "Successfully updated cities, ${it.size}")
                        }
                )
    }

    fun saveCity(city: City?) {
        featureDataProvider.overrideLocation = city?.name

        // We never read these if there's no city name
        featureDataProvider.overrideLatitude = city?.latitude ?: 0.0
        featureDataProvider.overrideLongitude = city?.longitude ?: 0.0

        clearAndRefreshNearbyCircles()
        citySubject.onNext(Optional(city?.name))
    }

    //
    /**
     * TODO: Should change this to a real City object, stored in database
     *
     * Returns *updates only* to the selected city (e.g. user-initiated city switching).
     * Doesn't dispatch the current city.
     */
    fun getSelectedCity(): Flowable<Optional<String>> {
        return citySubject.toFlowable(BackpressureStrategy.LATEST)
    }

    /**
     * Returns the currently selected city, and all future updates to the selected city
     */
    fun getCurrentCity(): Flowable<Optional<String>> {
        return getSelectedCity().startWith(Optional(featureDataProvider.overrideLocation))
    }

}