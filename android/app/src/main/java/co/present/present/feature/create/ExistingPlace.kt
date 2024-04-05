package co.present.present.feature.create

import android.net.Uri
import co.present.present.model.Circle
import com.google.android.gms.location.places.Place
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import java.util.*


/**
 * Our own implementation of the Place interface from Google Maps, so an existing circle location
 * can be streamed together with a new user-chosen location.
 *
 * We shouldn't be doing this though -- we should instead create a wrapper encapsulating the
 * important info from both and transform both Place and the existing circle info into that
 */
class ExistingPlace(val latitude: Double, val longitude: Double, val locationName: String) : Place {

    constructor(circle: Circle) : this(circle.latitude, circle.longitude, circle.locationName)

    override fun getName(): CharSequence = locationName
    override fun getLatLng(): LatLng = LatLng(latitude, longitude)
    override fun getAddress(): CharSequence = ""

    override fun getViewport(): LatLngBounds = error("not implemented")
    override fun isDataValid(): Boolean = error("not implemented")
    override fun getRating(): Float = error("not implemented")
    override fun getPriceLevel(): Int = error("not implemented")
    override fun getId(): String = error("not implemented")
    override fun getLocale(): Locale = error("not implemented")
    override fun getWebsiteUri(): Uri = error("not implemented")
    override fun freeze(): Place = error("not implemented")
    override fun getAttributions(): CharSequence = error("not implemented")
    override fun getPlaceTypes(): MutableList<Int> = error("not implemented")
    override fun getPhoneNumber(): CharSequence = error("not implemented")

}