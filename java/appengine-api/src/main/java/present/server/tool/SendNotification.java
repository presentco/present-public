package present.server.tool;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import present.proto.Platform;
import present.server.model.user.Client;
import present.server.model.user.Users;
import present.server.notification.Notification;
import present.server.notification.Notifier;

import static present.server.tool.RemoteTool.PRODUCTION_SERVER;
import static present.server.tool.RemoteTool.against;

/**
 * @author Bob Lee
 */
public class SendNotification {

  private static final Logger logger = LoggerFactory.getLogger(SendNotification.class);

  public static void main(String[] args) throws InterruptedException {
    //java.util.logging.Logger root = java.util.logging.Logger.getLogger("");
    //root.setLevel(Level.ALL);
    //root.getHandlers()[0].setLevel(Level.ALL);

    SendNotification.logger.info("Starting...");
    against(PRODUCTION_SERVER, () -> {
      Notifier.fromPresent().to(ProductionUsers.bob()).send(new Notification()
        .body("Hello, Bobby!"));
      SendNotification.logger.info("Sent notification.");
    });
  }
}
