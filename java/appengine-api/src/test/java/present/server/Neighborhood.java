package present.server;

import present.server.model.util.Coordinates;

/**
 * A neighborhood comprised of a name and location
 */
public class Neighborhood {

  private final String name;
  private final Coordinates location;

  /**
   * Constructs a new neighborhood
   *
   * @param name of the neighborhood
   * @param location of the neighborhood
   */
  public Neighborhood(String name, Coordinates location) {
    this.name = name;
    this.location = location;
  }

  /** Returns the name of this neighborhood. */
  public String name() {
    return name;
  }

  /** Returns the location of this neighborhood. */
  public Coordinates location() {
    return location;
  }
}
