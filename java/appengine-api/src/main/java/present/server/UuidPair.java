package present.server;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import java.util.List;
import java.util.Set;

/**
 * ID with the format "[UUID A]:[UUID B]".
 *
 * @author Bob Lee (bob@present.co)
 */
public class UuidPair {

  private static final char DELIMETER = ':';

  private final String ida;
  private final String idb;

  private UuidPair(String ida, String idb) {
    Uuids.validate(ida);
    Uuids.validate(idb);

    this.ida = ida;
    this.idb = idb;
  }

  public String a() {
    return ida;
  }

  public String b() {
    return idb;
  }

  /** Returns the IDs as a set. */
  public Set<String> toSet() {
    return ImmutableSet.of(ida, idb);
  }

  /** Returns the IDs as a list. */
  public List<String> toList() {
    return ImmutableList.of(ida, idb);
  }

  /** Returns whichever ID is different from the given ID. */
  public String otherThan(String id) {
    if (ida.equals(id)) return idb;
    if (!idb.equals(id)) throw new IllegalArgumentException("Expected " + id + " in " + this + ".");
    return ida;
  }

  /** Returns the formatted ID. */
  @Override public String toString() {
    return ida + DELIMETER + idb;
  }

  @Override public int hashCode() {
    return ida.hashCode() * 31 + idb.hashCode();
  }

  @Override public boolean equals(Object other) {
    return other instanceof UuidPair && equals((UuidPair) other);
  }

  private boolean equals(UuidPair other) {
    return ida.equals(other.ida) && idb.equals(other.idb);
  }

  /** Sorts the IDs lexicographically and constructs a UuidPair. */
  public static UuidPair sort(String ida, String idb) {
    return (ida.compareTo(idb) <= 0) ? create(ida, idb) : create(idb, ida);
  }

  /** Constructs a UuidPair with the IDs in the given order. */
  public static UuidPair create(String ida, String idb) {
    return new UuidPair(ida, idb);
  }

  /** Parses IDs of the form "[UUID A]:[UUID B]". */
  public static UuidPair parse(String id) {
    if (id.length() != Uuids.LENGTH * 2 + 1) {
      throw new IllegalArgumentException("Unexpected length: " + id.length());
    }
    if (id.charAt(Uuids.LENGTH) != DELIMETER) {
      throw new IllegalArgumentException("Invalid format: " + id);
    }
    String a = id.substring(0, Uuids.LENGTH);
    String b = id.substring(Uuids.LENGTH + 1);
    return new UuidPair(a, b);
  }
}
