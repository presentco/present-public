package present.server.model.console.whitelist.geofence;

import com.google.common.geometry.S2Cap;
import com.google.common.geometry.S2LatLng;
import present.s2.CircularRegion;
import present.server.Uuids;
import present.server.model.util.Coordinates;

/**
 * A geofence definition for the whitelist.
 * This class must remain JSON friendly.
 */
public class WhitelistGeofence {
  public String uuid = Uuids.newUuid();
  public String name;
  public String address;
  public double latitude;
  public double longitude;
  public double radius;
  public boolean whitelisted;

  public WhitelistGeofence() { }

  public WhitelistGeofence(
      String name, String address, Coordinates coordinates, double radius, boolean whitelisted) {
    this.name = name;
    this.address = address;
    this.latitude = coordinates.latitude;
    this.longitude = coordinates.longitude;
    this.radius = radius;
    this.whitelisted = whitelisted;
  }

  public boolean contains(Coordinates coordinates) {
    S2Cap region = CircularRegion.create(latitude, longitude, radius);
    S2LatLng coords = S2LatLng.fromDegrees(coordinates.latitude, coordinates.longitude);
    return region.contains(coords.toPoint());
  }
}
