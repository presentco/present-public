package present.server.model.util;

import com.google.common.base.MoreObjects;

/**
 * @author Pat Niemeyer (pat@pat.net)
 * Date: 11/9/17
 */
public class Address {

  /** Corresponds to Google Geocoding API Administrative Area Leve 1 (state, province) */
  public String state;

  /** Corresponds to Google Geocoding API Locality (city) */
  public String city;

  /** Corresponds to Google Geocoding API Locality (country) */
  public String country;

  public boolean isComplete() {
    return state != null && city != null && country != null;
  }

  @Override public String toString() {
    return MoreObjects.toStringHelper(this).add("state", state).add("city", city).toString();
  }

  public String niceString() {
    if (city == null) {
      if (state == null) {
        return "[unknown]";
      } else {
        return state;
      }
    } else {
      if (state == null) {
        return city;
      } else {
        return city + ", " + state;
      }
    }
  }
}
