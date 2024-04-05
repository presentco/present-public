package present.server.notification;

import com.googlecode.objectify.Ref;
import java.util.HashMap;
import java.util.Map;
import present.proto.ActivityType;
import present.server.model.PresentEntity;
import present.server.model.activity.Event;
import present.server.model.comment.Comment;
import present.server.model.group.Group;
import present.server.model.user.User;

/**
 * A notification.
 */
public class Notification {

  String title;
  String body;
  final Map<String, Object> customFields = new HashMap<>();
  String url;
  Event event;

  /** Sets the notification title. */
  public Notification title(String title) {
    this.title = title;
    return this;
  }

  /** Sets the notification body. */
  public Notification body(String body) {
    this.body = body;
    return this;
  }

  /** Adds a custom field. */
  public Notification put(String key, Object value) {
    customFields.put(key, value);
    return this;
  }

  /** Adds a comment ID. */
  public Notification put(Comment comment) {
    put(PayloadNames.COMMENT_ID, comment.uuid);
    put(comment.group.get());
    return this;
  }

  /** Adds a group ID. */
  public Notification put(Group group) {
    put(PayloadNames.GROUP_ID, group.uuid());
    if (this.url == null) this.url = group.shortLink();
    return this;
  }

  /** Adds a user ID. */
  public Notification put(User user) {
    put(PayloadNames.USER_ID, user.uuid);
    if (this.url == null) this.url = user.shortLink();
    return this;
  }

  /** Attaches a URL. */
  public Notification url(String url) {
    this.url = url;
    return this;
  }

  /** Attaches a prototype event. Notifier will clone the event and fill in the users. */
  public Notification event(Event event) {
    this.event = event;
    return this;
  }

  /** Attaches a prototype event. Notifier will clone the event and fill in the users. */
  public Notification event(ActivityType type, String summary,
      Ref<? extends PresentEntity<?>> defaultTarget) {
    return event(new Event(type, summary, defaultTarget));
  }

  /** Attaches a prototype event. Notifier will clone the event and fill in the users. */
  public Notification event(ActivityType type, String summary,
      PresentEntity<?> defaultTarget) {
    return event(type, summary, defaultTarget.getRef());
  }
}
