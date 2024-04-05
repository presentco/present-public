package present.server.notification;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import present.server.model.user.Client;

import static com.googlecode.objectify.ObjectifyService.ofy;

/**
 * Notification queries and utils that utilize the server model.
 *
 * @author Pat Niemeyer (pat@pat.net)
 * @see present.server.notification (the notifications module) for the notifications service.
 */
public class Notifications {
  private static final Logger logger = LoggerFactory.getLogger(Notifications.class);

  /** Prefixes APNS tokens targeted to the sandbox environment. */
  public static String SANDBOX_TOKEN_PREFIX = "Development:";

  /**
   * Check the APNS feedback service for failed tokens and remove them from Clients so that
   * they are not tried again.
   */
  public static void clearFailedTokens() {
    logger.info("Checking notifications feedback service");

    List<String> failedTokens = new ArrayList<>();
    for (String token : ApnsServices.sandboxService.getInactiveDevices().keySet()) {
      failedTokens.add(SANDBOX_TOKEN_PREFIX + token);
    }
    failedTokens.addAll(ApnsServices.productionService.getInactiveDevices().keySet());

    // Request failed tokens.
    logger.info("Gateway returned " + failedTokens.size() + " failed tokens.");

    if (failedTokens.isEmpty()) return;

    // Find Clients for the device tokens.
    List<Client> clients =
        ofy().load().type(Client.class).filter("deviceToken in", failedTokens).list();
    logger.info("Found " + clients.size() + " Clients for failed tokens.");

    // Remove device tokens that have not been updated since the failure time.
    for (Client client : clients) {
      client.deviceToken = null;
    }
    ofy().save().entities(clients);
  }

  public static void main(String[] args) {
    java.util.logging.Logger root = java.util.logging.Logger.getLogger("");
    root.setLevel(Level.ALL);
    root.getHandlers()[0].setLevel(Level.ALL);

    ApnsServices.productionService.getInactiveDevices().forEach((k, v) -> {
      System.out.println(k);
    });
  }
}
