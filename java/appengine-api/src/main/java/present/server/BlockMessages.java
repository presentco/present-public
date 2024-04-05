package present.server;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import present.server.model.user.User;

/**
 * Messages displayed to users when they're prohibited from accessing the application.
 *
 * @author Bob Lee (bob@present.co)
 */
public class BlockMessages {

  private static final Logger logger = LoggerFactory.getLogger(BlockMessages.class);

  private static final String REVIEWING = "Thanks for requesting access to Present! We're reviewing your request. In the meantime, please invite more extraordinary people to join Present!";

  private static final String MAN = "Thanks for requesting access to Present! We're reviewing your request.";

  private static final String DEFAULT = "Thanks for requesting access to Present! We're reviewing your request.";

  /** Returns the message to display to a user on the block screen. */
  public static String forUser(User user) {
    switch (user.state) {
      case REVIEWING:
      case WAITING:
        if (user.review == User.Review.OUTSIDE_GEOFENCE) {
          return "Thanks for requesting access to Present! "
              + "Present isn't available in " + city(user) + " yet. "
              + "Please invite more extraordinary people to join Present so we can expand there "
              + "sooner!";
        }

        return REVIEWING;
      case REJECTED:
      case SUSPENDED:
      case SUPPRESSED:
        return user.review == User.Review.MAN
            ? MAN
            : DEFAULT;
      case DELETED:
        return "This account has been deleted.";
    }

    logger.error("Oops.", new AssertionError("Unexpected state: " + user.state));
    return DEFAULT;
  }

  public static String city(User user) {
    if (user.signupAddress != null && user.signupAddress.city != null) {
      return user.signupAddress.city;
    }
    return "your city";
  }
}
