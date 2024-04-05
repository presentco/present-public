package present.server.notification;

import com.google.common.collect.MapMaker;
import com.notnoop.apns.APNS;
import com.notnoop.apns.ApnsDelegate;
import com.notnoop.apns.ApnsNotification;
import com.notnoop.apns.ApnsService;
import com.notnoop.apns.DeliveryError;
import com.notnoop.apns.EnhancedApnsNotification;
import com.notnoop.apns.PayloadBuilder;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import present.server.model.user.Client;

/**
 * Manages APNS connections.
 *
 * @author Bob Lee (bob@present.co)
 */
public class ApnsServices {

  private static final Logger logger = LoggerFactory.getLogger(ApnsServices.class);

  enum Type {
    SANDBOX("/present_apns_sandbox", 1),
    PRODUCTION("/present_apns_production", 25);

    private final String cert;
    private final int connections;

    Type(String cert, int connections) {
      this.cert = cert;
      this.connections = connections;
    }
  }

  private static Map<ApnsNotification, Callback> callbacks = new MapMaker().weakKeys().makeMap();

  private static final ApnsDelegate delegate = new ApnsDelegate() {
    @Override public void messageSent(ApnsNotification message, boolean resent) {
      Callback callback = callbacks.remove(message);
      if (callback != null) callback.sent();
    }

    @Override public void messageSendFailed(ApnsNotification notification, Throwable e) {
      if (notification == null) {
        logger.warn("Missing notification.", e);
        return;
      }
      Callback callback = callbacks.remove(notification);
      if (callback != null) callback.failed(e);
    }

    @Override public void connectionClosed(DeliveryError e, int messageIdentifier) {
      logger.warn("Connection closed: {}", e);
    }

    @Override public void cacheLengthExceeded(int newCacheLength) {
      logger.info("Expanding resend cache to {}.", newCacheLength);
    }

    @Override public void notificationsResent(int resendCount) {
      logger.info("Resent {} notifications.", resendCount);
    }
  };

  private static ApnsService newService(Type type) {
    return APNS.newService()
        .withCert(ApnsServices.class.getResourceAsStream(type.cert), "xxx")
        .asPool(type.connections)
        .withDelegate(delegate)
        .withAppleDestination(type == Type.PRODUCTION)
        .build();
  }

  static ApnsService sandboxService = newService(Type.SANDBOX);
  static ApnsService productionService = newService(Type.PRODUCTION);

  /** Returns the appropriate APNS connection for the given token. */
  private static ApnsService serviceFor(String token) {
    if (token.startsWith(Notifications.SANDBOX_TOKEN_PREFIX)) return sandboxService;
    return productionService;
  }

  /**
   * A pool with one thread per connection because we can only write to one connection at a time.
   */
  private static final Executor executor = Executors.newFixedThreadPool(
      Arrays.stream(Type.values()).mapToInt(t -> t.connections).sum());

  /** Asynchronously pushes a notification. */
  public static void push(Client client, String payload, Callback callback) {
    push(client.deviceToken, payload, callback);
  }

  /** Asynchronously pushes a notification. */
  public static void push(String token, String payload, Callback callback) {
    String actualToken = token;
    if (actualToken.startsWith(Notifications.SANDBOX_TOKEN_PREFIX)) {
      actualToken = token.substring(Notifications.SANDBOX_TOKEN_PREFIX.length());
    }
    EnhancedApnsNotification notification =
        new EnhancedApnsNotification(EnhancedApnsNotification.INCREMENT_ID(),
            EnhancedApnsNotification.MAXIMUM_EXPIRY, actualToken, payload);
    if (callback != null) callbacks.put(notification, callback);
    ApnsService service = serviceFor(token);

    // ApnsService.push() blocks until the notification is flushed to the kernel. Using an
    // executor here means we enqueue the push to an unbounded queue and return
    // immediately instead of blocking. Ideally, ApnsService would buffer and flush multiple
    // notifications at once, increasing throughput, but that's a pretty big change to the
    // underlying library. Long term, we should at least bound this queue to add some back
    // pressure. This is fine for now though!
    executor.execute(() -> {
      service.push(notification);
    });
  }

  /** Handles results of sending APNS notifications. */
  public interface Callback {

    /** Notification was sent successfully. */
    void sent();

    /** Failed to send notification. */
    void failed(Throwable t);
  }

  public static void main(String[] args) {
    String pat = "xxx";
    String bob = "xxx";
    String token = pat;

    ApnsService service = ApnsServices.productionService;
    String payload = APNS.newPayload().alertBody("Hello, World!").sound("default").build();
    EnhancedApnsNotification notification =
        new EnhancedApnsNotification(EnhancedApnsNotification.INCREMENT_ID(),
            EnhancedApnsNotification.MAXIMUM_EXPIRY, token, payload);
    service.push(notification);
  }
}
