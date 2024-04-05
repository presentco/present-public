package present.server.model.console.whitelist;

import java.util.regex.Pattern;
import javax.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import present.phone.PhoneNumbers;
import present.proto.Platform;
import present.server.RequestHeaders;
import present.server.environment.Environment;
import present.server.model.ZipCode;
import present.server.model.geocoding.Geocoding;
import present.server.model.user.Client;
import present.server.model.user.Clients;
import present.server.model.user.IosVersion;
import present.server.model.user.User;
import present.server.model.util.Address;
import present.server.model.util.Coordinates;
import present.server.model.console.whitelist.geofence.WhitelistGeofences;

/**
 * @author Pat Niemeyer (pat@pat.net)
 */
public class Whitelist {
  private static final Logger logger = LoggerFactory.getLogger(Whitelist.class);

  public static final String TEST_ZIP = "94108";

  // Override all whitelist logic and let everyone in.
  private static boolean ALLOW_ALL = false;

  // Tester fake numbers.
  // Twenty numbers: +1(415)555-0100 to +1(415)555-0119
  private static final Pattern TESTER_PHONE_RANGE_PATTERN = Pattern.compile("^\\+141555501[0-1][0-9]$");

  public static boolean userIsWhitelisted(User user) {
    return userIsWhitelisted(user.phoneNumber, user.email());
  }

  /**
   * True if the user is whitelisted by either phone or email.
   */
  public static boolean userIsWhitelisted(String phoneNumber, String email) {
    return phoneNumberIsWhitelisted(phoneNumber) || emailIsWhitelisted(email);
  }

  public static boolean zipCodeIsWhitelisted(User user, String zip) {
    if (zip == null) return false;
    ZipCode.validate(zip);
    return isTestZipCode(zip) || locationIsWhitelisted(user, Geocoding.geocodeZipCode(zip));
  }

  private static boolean isTestZipCode(String zip) {
    return RequestHeaders.isTest() && zip.equals(TEST_ZIP);
  }

  public static boolean locationIsWhitelisted(User user, Coordinates coordinates) {
    if (coordinates == null) {
      Client client = Clients.current();
      if (client.isIos() && client.iosVersion().compareTo(IosVersion.V4_3_B6) <= 0) {
        logger.warn("Allowing legacy client to proceed without location.");
        return true;
      }

      return false;
    }

    // Allow anyone in the U.S. to sign up in production.
    if (Environment.isProduction() && (inUnitedStates(user.signupAddress)
          || inUnitedStates(Geocoding.reverseGeocodeNow(coordinates)))) {
      return true;
    }

    // Fall back to geofences.
    return WhitelistGeofences.load().contains(coordinates);
  }

  /** Returns true if this address is in the United States. */
  public static boolean inUnitedStates(Address address) {
    return address != null && "United States".equals(address.country);
  }

  public static boolean phoneNumberIsWhitelisted(@Nullable String phone) {
    if (ALLOW_ALL) {
      return true;
    }
    if (phone == null || phone.isEmpty()) {
      return false;
    }
    PhoneNumbers.validateUsPhone(phone);
    if (isTesterReservedNumber(phone)) {
      return true;
    }
    WhitelistedUser wu = WhitelistedUser.findByPhone(phone);
    return wu != null && wu.whitelisted;
  }

  public static boolean emailIsWhitelisted(@Nullable String email) {
    logger.info("Checking for email in whitelist: "+email);
    if (ALLOW_ALL) {
      return true;
    }
    if (email == null || email.isEmpty()) {
      return false;
    }
    WhitelistedUser wu = WhitelistedUser.findByEmail(email);
    return wu != null && wu.whitelisted;
  }

  public static boolean isTesterReservedNumber(String phone) {
    return TESTER_PHONE_RANGE_PATTERN.matcher(phone).matches();
  }
}
