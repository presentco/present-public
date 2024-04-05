package present.server.notification;

import com.google.common.base.Ascii;
import present.proto.ActivityType;
import present.server.model.activity.Event;
import present.server.model.comment.Comment;
import present.server.model.group.Group;

/**
 * Creates group comment notifications.
 *
 * @author Bob Lee (bob@present.co)
 */
public class CommentNotifications extends Notification {

  private final Comment comment;

  private CommentNotifications(Comment comment) {
    this.comment = comment;
  }

  public static Notification create(Comment comment) {
    return new CommentNotifications(comment).toNotification();
  }

  private Notification toNotification() {
    return new Notification()
        .title(title())
        .body(body())
        .put(comment)
        .event(new Event(ActivityType.USER_COMMENTED_ON_GROUP, eventMessage(), comment.getRef()));
  }

  private Group group() {
    return (Group) comment.group.get();
  }

  private String eventMessage() {
    StringBuilder message = bodyBuilder();
    message.append(" in '")
        .append(title())
        .append("'");
    return message.toString();
  }

  private String title() {
    return Ascii.truncate(group().title.trim(), 100, "...");
  }

  private StringBuilder bodyBuilder() {
    StringBuilder message = new StringBuilder(comment.author.get().firstName.trim());
    if (comment.contentRef != null) {
      // TODO: Attach image to notification.
      switch (comment.contentRef.get().type) {
        case JPEG:
          message.append(" shared an image");
          break;
        case MP4:
          message.append(" shared a video");
          break;
        default:
          throw new AssertionError();
      }
    } else {
      message.append(" said \"")
          .append(Ascii.truncate(comment.text.trim(), 100, "..."))
          .append("\"");
    }
    return message;
  }

  public String body() {
    return bodyBuilder().toString();
  }
}
