package present.server.email;

import com.google.common.base.MoreObjects;
import java.util.concurrent.TimeUnit;
import present.server.Time;
import present.server.model.activity.Event;
import present.server.model.content.Content;
import present.server.model.user.User;

/**
 * Friend activity summary
 *
 * @author Gabrielle Taylor (gabrielle@present.co)
 */
public class FriendSummary {

  public final User user;

  private final long eventTime;

  public FriendSummary(Event event) {
    this.user = event.initiator.get();
    this.eventTime = event.createdTime;
  }

  public String icon() {
    if (user.photo == null) return SummaryEmail.PLACEHOLDER_IMAGE;
    Content content = user.photo.get();
    return content.circleUrl(150);
  }

  public String summary() {
    // TODO: Return "today" or "yesterday."
    long duration = (System.currentTimeMillis()) - eventTime;
    return "Joined " + Time.describeDuration(duration, TimeUnit.MILLISECONDS) + " ago";
  }

  @Override public String toString() {
    return MoreObjects.toStringHelper(this).add("user", this.user).toString();
  }
}
