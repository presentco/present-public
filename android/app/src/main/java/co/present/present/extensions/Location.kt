package co.present.present.extensions

import android.location.Location
import present.proto.Coordinates


fun Location.toCoordinates(): Coordinates = Coordinates(latitude, longitude, accuracy.toDouble())
/**
 * Model for a Present discussion group in a discovery view.
 */
fun newLocation() = Location("PresentInitializer")