package co.present.present.location

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import co.present.present.extensions.isCallable
import co.present.present.extensions.start
import co.present.present.model.Circle
import com.bumptech.glide.load.model.stream.BaseGlideUrlLoader




fun Activity.launchGoogleMaps(latitude: Double, longitude: Double, locationName: String?) {
    val locationUriPart = if (locationName == null) "" else "(${Uri.encode(locationName)})"
    val uri = Uri.parse("https://www.google.com/maps/search/?api=1&query=$latitude,$longitude$locationUriPart")
    fun unrestrictedIntent() = Intent(Intent.ACTION_VIEW, uri)
    val mapsIntent = unrestrictedIntent().apply { setPackage("com.google.android.apps.maps") }
    if (isCallable(mapsIntent)) {
        startActivity(mapsIntent)
    } else {
        start(unrestrictedIntent())
    }
}

class CoverPhotoUrlLoader(context: Context) : BaseGlideUrlLoader<Circle>(context) {
    override fun getUrl(circle: Circle, width: Int, height: Int): String {
        // Construct the url for the correct size here.
        return with(circle) {
            if (coverPhoto != null) {
                "${coverPhoto!!}=w$width-h$height-n-rj"
            } else {
                // "Pin" icon in our Google Cloud Storage account
                val iconUrl = "https://storage.googleapis.com/present-production/android/location-pin.png"
                "https://maps.googleapis.com/maps/api/staticmap?zoom=15" +
                        "&scale=2" +
                        "&size=${Math.round(width / 2f)}x${Math.round(height / 2f)}" +
                        "&maptype=roadmap" +
                        "&markers=${Uri.encode("icon:$iconUrl|$latitude,$longitude")}" +
                        "&center=$latitude,$longitude" +
                        "&key=AIzaSyDAkC7ZPpRvdt2Nh1NS7fKxKJis6ZTf6N4"
            }
        }
    }
}


