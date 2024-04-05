package present.server.tool;

import com.google.common.base.Charsets;
import com.google.common.geometry.S2LatLng;
import com.google.common.io.Files;
import com.opencsv.CSVReader;
import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import present.server.Uuids;
import present.server.model.group.WelcomeGroup;
import present.server.model.util.Coordinates;

public class ChooseWelcomeGroupCities {

  /*
   * 1. Start with all cities with population > 100k.
   * 2. Add largest cities for Deleware, Maine, Vermont, West Virginia, Wyoming.
   * 3. Remove cities within MIN_DISTANCE_MILES of a larger city.
   */

  private static final int MIN_DISTANCE_MILES = 60;

  private static final int MIN_DISTANCE_METERS = MIN_DISTANCE_MILES * 1609;

  public static void main(String[] args) throws IOException {
    String csv = Files.asCharSource(
        new File("./java/etc/largest-cities.csv"), Charsets.UTF_8).read();
    CSVReader reader = new CSVReader(new StringReader(csv));
    Pattern locationPattern = Pattern.compile("^(\\d+\\.\\d+).* (\\d+\\.\\d+).*");
    boolean header = true;
    List<City> cities = new ArrayList<>();
    for (String[] row : reader) {
      if (header) {
        header = false;
        continue;
      }
      String city = row[1];
      String state = row[2];
      int population = Integer.parseInt(row[3].replace(",",""));
      Matcher matcher = locationPattern.matcher(row[4]);
      if (!matcher.matches()) throw new IllegalArgumentException(row[4]);
      double latitude = Double.parseDouble(matcher.group(1));
      double longitude = -Double.parseDouble(matcher.group(2));
      cities.add(new City(city, state, population, S2LatLng.fromDegrees(latitude, longitude)));
    }

    List<City> selected = new ArrayList<>();
    outer: for (City current : cities) {
      for (City other : cities) {
        if (other != current) {
          if (other.overrides(current)) {
            System.out.println(current + " overridden by " + other);
            continue outer;
          }
        }
      }
      selected.add(current);
    }

    System.out.println();

    System.out.println(selected.size());
    for (City city : selected) {
      System.out.printf("new WelcomeGroup(\"%s\", \"%s\", \"%s\", new Coordinates(%s, %s)),\n",
          city.uuid(), city.name, city.state, city.location.latDegrees(), city.location.lngDegrees());
    }
  }

  static class City {

    final String name;
    final String state;
    final int population;
    final S2LatLng location;

    City(String name, String state, int population, S2LatLng location) {
      this.name = name;
      this.state = state;
      this.population = population;
      this.location = location;
    }

    String uuid() {
      return Uuids.fromName("Welcome to " + name + "!");
    }

    public boolean overrides(City other) {
      if (other.population > population) return false;
      return location.getEarthDistance(other.location) < MIN_DISTANCE_METERS;
    }

    @Override public String toString() {
      return name + ", " + state;
    }
  }
}
