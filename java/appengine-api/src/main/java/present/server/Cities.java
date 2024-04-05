package present.server;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import present.proto.City;
import present.server.model.user.User;
import present.server.model.user.Users;
import present.server.model.util.Coordinates;

/**
 * City utilities.
 *
 * @author Bob Lee (bob@present.co)
 */
public class Cities {

  public static final City SAN_FRANCISCO = new City("San Francisco", Coordinates.SAN_FRANCISCO.toProto(), 25_000d);
  public static final City NEW_YORK = new City("New York", Coordinates.NEW_YORK.toProto(), 25_000d);
  public static final City LOS_ANGELES = new City("Los Angeles", Coordinates.LOS_ANGELES.toProto(), 25_000d);
  public static final City WASHINGTON_DC = new City("Washington, D.C.", Coordinates.WASHINGTON_DC.toProto(), 25_000d);
  public static final City ST_LOUIS = new City("St. Louis", Coordinates.ST_LOUIS.toProto(), 25_000d);
  public static final City CHICAGO = new City("Chicago", Coordinates.CHICAGO.toProto(), 25_000d);
  public static final City SEATTLE = new City("Seattle", Coordinates.SEATTLE.toProto(), 25_000d);
  public static final City PORTLAND = new City("Portland", Coordinates.PORTLAND.toProto(), 25_000d);
  public static final City ATLANTA = new City("Atlanta", Coordinates.ATLANTA.toProto(), 25_000d);
  public static final City DENVER = new City("Denver", Coordinates.DENVER.toProto(), 25_000d);
  public static final City AUSTIN = new City("Austin", Coordinates.AUSTIN.toProto(), 25_000d);
  public static final City FARGO = new City("Fargo",
      new present.proto.Coordinates(46.8541125d,-96.9685969d, 0d), 25_000d);

  private static final List<City> ALL = ImmutableList.of(
      AUSTIN,
      CHICAGO,
      LOS_ANGELES,
      NEW_YORK,
      PORTLAND,
      SAN_FRANCISCO,
      SEATTLE,
      ST_LOUIS,
      WASHINGTON_DC,
      ATLANTA
  );

  private static final List<City> FOR_ADMINS = ImmutableList.<City>builder()
      .addAll(ALL)
      .add(FARGO)
      .build();

  public static List<City> all() {
    User user = Users.current(false);
    return user != null && user.isAdmin() ? FOR_ADMINS : ALL;
  }

  public static NearbyCity nearestTo(Coordinates location) {
    Iterator<City> cities = ALL.iterator();
    City nearest = cities.next();
    double smallestDistance = distanceBetween(nearest, location);
    while (cities.hasNext()) {
      City city = cities.next();
      double distance = distanceBetween(city, location);
      if (distance < smallestDistance) {
        nearest = city;
        smallestDistance = distance;
      }
    }
    return new NearbyCity(nearest, smallestDistance);
  }

  private static double distanceBetween(City city, Coordinates location) {
    return Coordinates.fromProto(city.location).distanceTo(location);
  }
}

