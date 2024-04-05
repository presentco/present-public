package co.present.present.location

import android.content.Context
import android.location.Geocoder
import android.location.Location
import android.os.Looper
import android.util.Log
import co.present.present.PresentApplication
import co.present.present.extensions.Optional
import co.present.present.extensions.applySingleSchedulers
import co.present.present.extensions.isEmulator
import co.present.present.extensions.newLocation
import co.present.present.feature.create.ExistingPlace
import com.google.android.gms.location.*
import com.google.android.gms.location.places.Place
import com.google.android.gms.maps.model.LatLng
import io.reactivex.*
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.BehaviorSubject
import present.proto.Coordinates
import java.util.*
import javax.inject.Inject


/**
 * Abstraction for location service.
 */
val metersPerMile = 1609.34f
val permissionsRequestCodeAccessFineLocation = 3373
private val presentHq get() = newLocation().apply { latitude = 37.785834; longitude = -122.406417 }

fun Location.toLatLng(): LatLng {
    return LatLng(latitude, longitude)
}

fun Location.toCoordinates(): Coordinates {
    return Coordinates(latitude, longitude, accuracy.toDouble())
}

fun Coordinates.toLocation(): Location {
    return newLocation().apply { latitude = this@toLocation.latitude; longitude = this@toLocation.longitude }
}

fun Location.formattedDistanceTo(other: Location): String {
    val distance: Float = distanceTo(other)
    return formatDistance(distance)
}

fun formatDistance(distanceMeters: Float): String {
    val distanceMiles = distanceMeters / metersPerMile

    return when {
        distanceMiles < 0.1f -> "Here"
        distanceMiles < 1 -> String.format(Locale.getDefault(), "%.1f mi", distanceMiles)
        else -> String.format(Locale.getDefault(), "%d mi", Math.round(distanceMiles))
    }
}

class LocationDataProvider @Inject constructor(val locationPermissions: LocationPermissions) {
    private val TAG = javaClass.simpleName

    fun getCachedLocation(): Location? = locationOptionalSubject.value.value

    private val locationOptionalSubject = BehaviorSubject.createDefault<Optional<Location>>(Optional(null)).apply {
        // we just need to force fetch the location once on startup, we rely on side effects
        // TODO FIX THIS WHOLE CLASS, THIS IS NOT THE RIGHT WAY TO DO THIS
        getLocation(PresentApplication.staticAppComponent.application).compose(applySingleSchedulers())
                .subscribeBy({}, {})
    }

    fun getLocationOptional(): Flowable<Optional<Location>> {
        return locationOptionalSubject.toFlowable(BackpressureStrategy.LATEST)
    }

    /**
     * RxJava wrapper for the Android fused location provider API.
     */
    fun getLocation(context: Context): Single<Location> {
        return Single.create { emitter: SingleEmitter<Location> ->
            if (!locationPermissions.isGranted(context)) {
                emitter.onError(SecurityException("No location permission"))
                locationOptionalSubject.onNext(Optional(null))
                return@create
            }

            val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
            try {
                fusedLocationClient.lastLocation
                        .addOnFailureListener { e ->
                            emitter.onError(e)
                            locationOptionalSubject.onNext(Optional(null))
                        }
                        .addOnSuccessListener { location ->
                            if (location == null) {
                                if (isEmulator()) {
                                    Log.d(TAG, "Got null location, but we're on an emulator, so sending back HQ's coordinates")
                                    emitter.onSuccess(presentHq)
                                    locationOptionalSubject.onNext(Optional(presentHq))
                                } else {
                                    Log.d(TAG, "Got null location on a real device. Checking if location is enabled in device settings")
                                    getSettingsClient(context).checkLocationSettings(locationSettingsRequest).apply {
                                        addOnFailureListener { e ->
                                            Log.d(TAG, "Device location settings are disabled, returning an error")
                                            emitter.onError(e)
                                            locationOptionalSubject.onNext(Optional(null))
                                        }
                                        addOnSuccessListener {
                                            Log.d(TAG, "Device location settings enabled but still no location, subscribing to live updates")
                                            fusedLocationClient.requestLocationUpdates(locationRequest,
                                                    object: LocationCallback() {
                                                        override fun onLocationResult(locationResult: LocationResult) {
                                                            locationResult.mostRecent()?.let { location ->
                                                                emitter.onSuccess(location)
                                                                locationOptionalSubject.onNext(Optional(location))
                                                                fusedLocationClient.removeLocationUpdates(this)
                                                            }
                                                        }
                                                    },
                                                    Looper.getMainLooper())
                                        }
                                    }
                                }
                            } else {
                                emitter.onSuccess(location)
                                locationOptionalSubject.onNext(Optional(location))
                            }
                        }
            } catch (e: SecurityException) {
                // I'm not sure this catch block is actually working.  At least one type of SecurityException
                // is just thrown straight from Parcel and can't be caught here
                emitter.onError(e)
                locationOptionalSubject.onNext(Optional(null))
            }
        }
    }

    fun getCity(context: Context, latitude: Double, longitude: Double): Maybe<String> {
        return Maybe.create<String> { emitter ->
            val results = Geocoder(context, Locale.getDefault()).getFromLocation(latitude, longitude, 1)
            if (results != null && results.isNotEmpty()) {
                emitter.onSuccess(results[0].locality)
            }
            emitter.onComplete()
        }.subscribeOn(Schedulers.io())
    }

    fun getCurrentPlace(context: Context): Single<Optional<Place>> {
        return getLocation(context).flatMap {
            Single.create<Optional<Place>> { emitter ->
                val results = Geocoder(context, Locale.getDefault()).getFromLocation(it.latitude, it.longitude, 1)
                if (results != null && results.isNotEmpty()) {
                    val existingPlace = ExistingPlace(it.latitude, it.longitude, results[0].getAddressLine(0))
                    emitter.onSuccess(Optional(existingPlace))
                } else {
                    emitter.onSuccess(Optional(null))
                }
            }.subscribeOn(Schedulers.io())
        }
    }

    private fun getSettingsClient(context: Context) = LocationServices.getSettingsClient(context)

    private fun LocationResult.mostRecent(): Location? {
        if (locations.isEmpty()) return null
        return locations.maxBy { it.time }
    }

    private val locationRequest by lazy {
        LocationRequest().apply {
            interval = 10000
            fastestInterval = 5000
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        }
    }

    private val locationSettingsRequest: LocationSettingsRequest
        get() {
            return LocationSettingsRequest.Builder().addLocationRequest(locationRequest).build()
        }
}



