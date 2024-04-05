package present.server.model.util;

import com.google.common.geometry.S2Cell;
import com.google.common.geometry.S2CellId;
import com.google.common.geometry.S2LatLng;
import javax.annotation.Nullable;

/**
 * A location on the globe.
 *
 * @author Bob Lee (bob@present.co)
 */
public class Coordinates {

  public static Coordinates PRESENT_COMPANY = new Coordinates(37.7904209, -122.405975);
  public static Coordinates SAN_FRANCISCO = new Coordinates(37.7625244, -122.4449224);
  public static Coordinates BIRMINGHAM = new Coordinates(33.522317, -86.810055);
  public static Coordinates CUPERTINO = new Coordinates(37.7625244, -122.4449224);
  public static Coordinates NEW_YORK = new Coordinates(40.7828647, -73.9675438);
  public static Coordinates LOS_ANGELES = new Coordinates(34.0413083, -118.2494922);
  public static Coordinates ST_LOUIS = new Coordinates(38.6461, -90.3251517);
  public static Coordinates CHICAGO = new Coordinates(41.8735735, -87.6826679);
  public static Coordinates SEATTLE = new Coordinates(47.6116557, -122.3262789);
  public static Coordinates PORTLAND = new Coordinates(45.5240667,-122.6477124);
  public static Coordinates DETROIT = new Coordinates(42.3526896, -83.1694157);
  public static Coordinates WASHINGTON_DC = new Coordinates(38.8976763, -77.0387238);
  public static Coordinates ATLANTA = new Coordinates(33.7618207, -84.3945295);
  public static Coordinates DENVER = new Coordinates(39.7619, -104.8811);
  public static Coordinates AUSTIN = new Coordinates(30.2672, -97.7431);
  public static Coordinates NONE = new Coordinates(0.0, 0.0, 0.0);

  /** In degrees. */
  public double latitude;

  /** In degrees. */
  public double longitude;

  /** In m. 0 if the coordinates are 100% accurate. */
  public double accuracy;

  public Coordinates() {}

  public Coordinates(present.proto.Coordinates proto) {
    this(proto.latitude, proto.longitude, proto.accuracy);
  }

  public Coordinates(double latitude, double longitude, double accuracy) {
    this.latitude = latitude;
    this.longitude = longitude;
    this.accuracy = accuracy;
  }

  public Coordinates(double latitude, double longitude) {
    this(latitude, longitude, 0);
  }

  public static @Nullable Coordinates fromProto(@Nullable present.proto.Coordinates proto) {
    return proto != null ? new Coordinates(proto) : null;
  }

  public present.proto.Coordinates toProto() {
    return new present.proto.Coordinates(latitude, longitude, 0.0);
  }

  public S2LatLng toS2LatLng() {
    return S2LatLng.fromDegrees(latitude, longitude);
  }

  public long toS2CellId() {
    return S2CellId.fromLatLng(toS2LatLng()).id();
  }

  public double distanceTo(Coordinates other) {
    return toS2LatLng().getEarthDistance(other.toS2LatLng());
  }

  @Override public String toString() {
    return "Coordinates{" +
        "latitude=" + latitude +
        ", longitude=" + longitude +
        ", accuracy=" + accuracy +
        '}';
  }

  public static Coordinates fromS2Cell(long location) {
    S2CellId cellId = new S2CellId(location);
    S2LatLng latLng = cellId.toLatLng();
    return new Coordinates(latLng.latDegrees(), latLng.lngDegrees());
  }
}
