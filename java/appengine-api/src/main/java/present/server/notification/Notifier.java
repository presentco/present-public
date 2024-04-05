package present.server.notification;

import com.google.common.base.Preconditions;
import com.google.common.base.Stopwatch;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import com.google.common.collect.Sets;
import com.google.common.collect.Streams;
import com.googlecode.objectify.Key;
import com.googlecode.objectify.Ref;
import com.notnoop.apns.APNS;
import com.notnoop.apns.PayloadBuilder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import present.proto.Platform;
import present.proto.UnreadCounts;
import present.proto.UnreadState;
import present.server.MoreObjectify;
import present.server.Time;
import present.server.Uuids;
import present.server.model.activity.Event;
import present.server.model.user.Client;
import present.server.model.user.Clients;
import present.server.model.user.UnreadStates;
import present.server.model.user.User;
import present.server.model.user.Users;
import present.server.notification.fcm.FcmMessage;
import present.server.notification.fcm.FcmNotification;
import present.server.notification.fcm.FcmSender;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import static com.googlecode.objectify.ObjectifyService.ofy;
import static java.util.stream.Collectors.groupingBy;
import static present.proto.Platform.ANDROID;
import static present.proto.Platform.IOS;
import static present.proto.Platform.TEST;

/**
 * Sends notifications and records {@link Event}s. If a user disables notifications (via
 * a setting, muting, etc.), we still record an event, increase badge counts, etc. Designed to
 * enable callers to asynchronously query Users and Clients in anticipation of sending
 * a notification.
 *
 * @author Bob Lee (bob@present.co)
 */
public final class Notifier {

  private static final Logger logger = LoggerFactory.getLogger(Notifier.class);

  private static final String DEFAULT_SOUND = "default";

  private static final String STAGING = "Staging: ";

  /** Initiator of the notification. */
  private User from;

  /** All notification recipients. */
  private Iterable<User> to;

  /** Everyone has notifications enabled by default. */
  private Predicate<User> enabledFilter = user -> true;

  /** Clients who will receive the notification. */
  private Iterable<NotifiableClient> clients;

  private Notifier(User from) {
    this.from = Preconditions.checkNotNull(from);
  }

  /** Creates a Notifier on behalf of the given user. */
  public static Notifier from(User user) {
    return new Notifier(user);
  }

  public static Notifier fromPresent() { return new Notifier(Users.getGenericAdminUser()); }

  /** Creates a Notifier on behalf of the current user. */
  public static Notifier create() {
    return from(Users.current());
  }

  /** Sets users that should be notified. Loads Users and Clients concurrently. */
  public Notifier toKeys(Iterable<Key<User>> userKeys) {
    // Asynchronously load users.
    this.to = Iterables.filter(MoreObjectify.load(userKeys), this::filter);
    loadClientsFor(userKeys);
    return this;
  }

  /** Sets users that should be notified. */
  public Notifier to(Iterable<User> users) {
    this.to = Iterables.filter(users, this::filter);
    loadClientsFor(Iterables.transform(users, User::getKey));
    return this;
  }

  /** Sets user that should be notified. */
  public Notifier to(User user) {
    return to(Collections.singleton(user));
  }

  /** Filters out the initiator and users who shouldn't be notified by them. */
  private boolean filter(User user) {
    return user != null && !this.from.equals(user) && this.from.canNotify(user);
  }

  private void loadClientsFor(Iterable<Key<User>> userKeys) {
    // TODO: Cache User -> List<Client> in Memcache.
    // Query Clients in parallel to Users. This loads Clients for users who have disabled
    // notifications, but they'll be in the minority.
    List<Client> allClients = Clients.query()
        .filter(Client.Fields.user.getName() + " in", userKeys)
        .filter(Client.Fields.deviceToken.getName() + " !=", null)
        .list();

    this.clients = Iterables.transform(allClients, client-> {
      assert client != null;
      User user = client.user.get();
      boolean badgeOnly = !(filter(user) && this.enabledFilter.test(user));
      return new NotifiableClient(client, badgeOnly);
    });
  }

  /** Selects users who have notifications enabled. Filters users in memory. */
  public Notifier enable(Predicate<User> notificationFilter) {
    this.enabledFilter = notificationFilter;
    return this;
  }

  /** Returns all users, with and without notifications enabled. */
  public Iterable<User> recipients() {
    return this.to;
  }

  private UnreadStates unreadStates;

  /**
   * Returns UnreadStates for the recipients of this notification. If this method isn't called,
   * the unread states will not be queried, and badge counts will not be sent with the
   * notification.
   */
  public UnreadStates unreadStates() {
    if (unreadStates == null) unreadStates = UnreadStates.loadFor(this.to);
    return this.unreadStates;
  }

  private Notification notification;
  private boolean sent;

  /**
   * Sends the notifications. Can only be called once.
   *
   * Note: You may decide not to call this in the case of a duplicate request.
   */
  public void send(Notification notification) {
    stage(notification);
    commit();
  }

  /**
   * Prepares a notification to be sent. Use this along with commit() when you don't want to send
   * a notifcation during a transaction which can be retried.
   */
  public void stage(Notification notification) {
    // Allow this to be called twice in case a transaction is retried.
    this.notification = Preconditions.checkNotNull(notification);
  }

  private final Map<User, Boolean> daytime = new HashMap<>();

  private boolean isDaytimeFor(User user) {
    Boolean result = daytime.get(user);
    if (result == null) {
      logger.error("This shouldn't happen.", new AssertionError());
      return true;
    }
    return result;
  }

  /**
   * Sends the staged notification.
   */
  public void commit() {
    Preconditions.checkState(ofy().getTransaction() == null, "Don't send"
        + " notifications during transaction--they can be retried. Use stage()/commit() instead.");
    if (notification == null) {
      logger.debug("No notification was staged.");
      return;
    }
    Preconditions.checkState(!sent, "Already sent.");
    Preconditions.checkState(this.to != null, "Call to().");
    Preconditions.checkNotNull(notification.body);
    Preconditions.checkState(ofy().getTransaction() == null,
        "Don't send notifications inside transactions.");

    // Discourage sending more than one notification.
    sent = true;

    saveEvents();

    // Determine whether or not it is daytime for a given user based on their location.
    Map<User, List<NotifiableClient>> byUser = Streams.stream(clients).collect(groupingBy(NotifiableClient::user));
    byUser.forEach((user, clients) -> {
      NotifiableClient latest = clients.stream()
          .max(Comparator.comparingLong(NotifiableClient::locationTimestamp)).get();
      daytime.put(user, Time.isDaytime(latest.client.location()));
    });

    Map<Platform, Consumer<NotifiableClient>> platformNotifiers = ImmutableMap.of(
        IOS, this::notifyIos,
        ANDROID, this::notifyAndroid,
        TEST, this::notifyTest
    );

    Map<Platform, List<NotifiableClient>> clientsByPlatform
        = Streams.stream(clients).collect(groupingBy(NotifiableClient::platform));
    for (Map.Entry<Platform, Consumer<NotifiableClient>> entry : platformNotifiers.entrySet()) {
      Stopwatch sw = Stopwatch.createStarted();
      Platform platform = entry.getKey();
      int count = 0;
      List<NotifiableClient> clients = clientsByPlatform.get(platform);
      if (clients == null) continue;
      for (NotifiableClient client : clients) {
        entry.getValue().accept(client);
        count++;
      }
      logger.info("Sent {} {} notifications in {}.", count, platform, sw);
    }
  }

  private static final FcmSender fcm = new FcmSender("AAAARH8fmtc:APA91bGTvEHYZC9OcfSF8CHCSLP3ZZ8fxwjXhdi4H65tukU1ell-dUEOtz2es1AilPULa22aOWLqYcZt0noPGpSJ7uua-r7x2ZZmiceBv4u1A12QdIa08wJxaI8WnRquwCbefU01vdSF");

  /** Sends an Android notification to the given client. */
  private void notifyAndroid(NotifiableClient notifiableClient) {
    Client client = notifiableClient.client;
    // Because Firebase notifications in the background don't give us the flexibility we want
    // (can't show as a popup), we have to send data-only messages from the server and construct
    // the Android notification locally on the client. More info: https://goo.gl/SjWbxC

    Map<String, Object> data = new HashMap<>();
    data.putAll(notification.customFields);
    if (notification.title != null) data.put("title", notification.title);
    data.put("body", notification.body);
    data.put("sound", isDaytimeFor(client.user()) ? DEFAULT_SOUND : "disabled");

    FcmMessage.MessageBuilder builder = new FcmMessage.MessageBuilder()
        .toToken(client.deviceToken)
        .addData(data);
    if (notification.url != null) builder.addData("url", notification.url);

    if (client.androidVersion() < 70) {
      // Legacy clients don't support data. Note: The user may have upgraded but not told us yet.
      // We'll find out when they open the app.
      builder.notification(new FcmNotification(notification.title, notification.body));
    }

    // We currently fire and forget. We could wait until the notification is sent.
    fcm.send(builder.build(), new Callback() {
      @Override public void onResponse(Call call, Response response) {
        logger.debug("Sent Android notification: {}", response.code());
      }
      @Override public void onFailure(Call call, Throwable t) {
        logger.warn("Failed to send Android notification.", t);
      }
    });
  }

  /** The iOS app sometimes maps the same device token to two different Clients. */
  private final Set<String> notifiedTokens = new HashSet<>();

  /** Sends an iOS notification to the given client. */
  private void notifyIos(NotifiableClient notifiableClient) {
    Client client = notifiableClient.client;

    if (!notifiedTokens.add(client.deviceToken)) {
      logger.info("Skipping notification for duplicate token: " + client.deviceToken);
      return;
    }

    PayloadBuilder builder = APNS.newPayload();

    if (!notifiableClient.badgeOnly) {
      builder.alertBody(notification.body);
      if (isDaytimeFor(client.user())) {
        builder.sound(DEFAULT_SOUND);
      }

      if (notification.title != null) builder.alertTitle(notification.title);

      if (!notification.customFields.isEmpty() || notification.url != null) {
        // Wake the app up so it can preload data.
        builder.instantDeliveryOrSilentNotification();
        builder.customFields(notification.customFields);
        if (notification.url != null) builder.customField("url", notification.url);
      }
    }

    // Set badge, if available.
    if (unreadStates != null) {
      UnreadState unreadState = unreadStates.stateFor(client.user.get());
      if (unreadState != null) {
        int badge = client.badge(unreadState);
        if (badge > -1) builder.badge(badge);
      }
    }

    notifyIos(client, builder.build());
  }

  /** Sends an iOS notification to the given client. */
  private static void notifyIos(Client client, String payload) {
    // We currently fire and forget. We could wait until the notification is sent.
    ApnsServices.push(client, payload, new ApnsServices.Callback() {
      @Override public void sent() {
        logger.debug("Sent iOS notification to: {}", client.deviceToken);
      }
      @Override public void failed(Throwable t) {
        logger.error("Failed to send notification to " + client + ".", t);
      }
    });
  }

  /** Sends an iOS notification to the given client. */
  private void notifyTest(NotifiableClient notifiableClient) {
    Client client = notifiableClient.client;
    int badge = -1;
    if (unreadStates != null) {
      UnreadCounts counts = unreadStates.countsFor(client.user.get());
      if (counts != null) badge = counts.total;
    }
    ofy().save().entity(new TestNotification(from.getRef(), Ref.create(client),
        notification.title, notification.body, notification.url, badge, notification.customFields));
  }

  /** Asynchronously saves events to the activity feed. */
  private void saveEvents() {
    // TODO: Save one Event for N users instead of one per user.
    Event prototype = notification.event;
    if (prototype == null) return;
    prototype = prototype.clone();
    prototype.icon = from.photo;
    List<Event> events = new ArrayList<>();
    Ref<User> fromRef = this.from.getRef();
    for (User user : to) {
      Event clone = prototype.clone();
      clone.uuid = Uuids.newUuid();
      clone.initiator = fromRef;
      clone.user = user.getRef();
      events.add(clone);
    }
    ofy().save().entities(events);
  }

  /** Updates the badge counts for the given user. */
  public static void sendBadgeCounts(User user, UnreadState state) {
    List<Client> allClients = Clients.query()
        .filter(Client.Fields.user.getName(), user.getKey())
        .filter(Client.Fields.deviceToken.getName() + " !=", null)
        .list();
    List<Client> iosClients = Streams.stream(allClients)
        .filter(c -> c.platform() == IOS)
        .collect(Collectors.toList());
    for (Client client : iosClients) {
      int badge = client.badge(state);
      if (badge == -1) {
        logger.info("Badge was -1");
        continue;
      }
      String payload = APNS.newPayload().badge(badge).build();
      notifyIos(client, payload);
    }
  }

  /** Holds a Client that is the target of a notification along with its mute status. */
  private class NotifiableClient {
    public Client client;
    public boolean badgeOnly;

    public NotifiableClient(Client client, boolean badgeOnly) {
      this.client = client;
      this.badgeOnly = badgeOnly;
    }

    public Platform platform() {
      return client.platform();
    }

    public User user() {
      return client.user();
    }

    public long locationTimestamp() {
      return client.locationTimestamp;
    }
  }
}
