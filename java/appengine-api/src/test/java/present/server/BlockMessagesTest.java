package present.server;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import present.server.model.user.User;
import present.server.model.user.UserState;
import present.server.model.util.Address;

import static org.junit.Assert.assertTrue;

public class BlockMessagesTest {

  private static final Logger logger = LoggerFactory.getLogger(BlockMessagesTest.class);

  @Test public void outsideGeofence() {
    User u = new User();
    u.state = UserState.REVIEWING;
    u.review = User.Review.OUTSIDE_GEOFENCE;
    u.signupAddress = new Address();
    u.signupAddress.city = "Cancun";
    String message = BlockMessages.forUser(u);
    logger.info(message);
    assertTrue(message.contains("Cancun"));
  }
}
