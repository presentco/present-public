package present.server.notification;

import com.google.common.base.MoreObjects;
import com.google.common.base.Objects;
import com.google.common.base.Preconditions;
import com.googlecode.objectify.Key;
import com.googlecode.objectify.Ref;
import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Id;
import com.googlecode.objectify.annotation.Index;
import java.util.List;
import java.util.Map;
import present.proto.Platform;
import present.proto.RequestHeader;
import present.server.RequestHeaders;
import present.server.model.user.Client;
import present.server.model.user.User;

import static com.googlecode.objectify.ObjectifyService.ofy;

/** A notification sent during a test. */
@Entity public class TestNotification {

  @Id private Long id;
  @Index Ref<User> from;
  @Index public Ref<Client> to;
  public String title;
  public String body;
  public String url;
  public int badge = -1;
  public Map<String, Object> customFields;

  public TestNotification() {}

  public TestNotification(Ref<User> from, Ref<Client> to, String title, String body,
      String url, int badge, Map<String, Object> customFields) {
    RequestHeader header = RequestHeaders.current();
    Preconditions.checkState(header == null || header.platform == Platform.TEST);

    this.from = from;
    this.to = to;
    this.title = title;
    this.body = body;
    this.customFields = customFields;
    this.badge = badge;
  }

  /** Loads test notifications from the given user. */
  public static List<TestNotification> from(User user) {
    return ofy().load().type(TestNotification.class).filter("from", user).list();
  }

  /** Loads test notifications to the given client. */
  public static List<TestNotification> to(Client client) {
    return ofy().load().type(TestNotification.class).filter("to", client).list();
  }

  /** Loads test notifications to the given client. */
  public static List<TestNotification> to(String clientUuid) {
    return ofy().load().type(TestNotification.class)
        .filter("to", Key.create(Client.class, clientUuid)).list();
  }

  /** Loads all test notifications. */
  public static List<TestNotification> all() {
    return ofy().load().type(TestNotification.class).list();
  }

  /** Loads all test notifications. */
  public static void dump() {
    for (TestNotification notification : all()) {
      System.out.println(notification.toString());
    }
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .add("from", from)
        .add("to", to)
        .add("title", title)
        .add("body", body)
        .add("customFields", customFields)
        .toString();
  }
}
