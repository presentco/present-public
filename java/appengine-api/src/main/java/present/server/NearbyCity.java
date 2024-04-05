package present.server;

import present.proto.City;

/**
 * A city with a distance.
 *
 * @author Bob Lee (bob@present.co)
 */
public class NearbyCity {

  public final City city;

  /** Distance in m. */
  public final double distance;

  public NearbyCity(City city, double distance) {
    this.city = city;
    this.distance = distance;
  }
}
