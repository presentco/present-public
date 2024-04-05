package present.server.email;

import com.google.common.base.MoreObjects;
import present.server.model.content.Content;
import present.server.model.group.Group;

/**
 * Invite activity summary
 *
 * @author Gabrielle Taylor (gabrielle@present.co)
 */
public class InviteSummary {

  public final Group group;

  public InviteSummary(Group group) {
    this.group = group;
  }

  public String icon() {
    if (group.coverContent == null) return SummaryEmail.PLACEHOLDER_IMAGE;
    Content content = group.coverContent.get();
    return content == null ? SummaryEmail.PLACEHOLDER_IMAGE : content.circleUrl(150);
  }

  public String summary() {
    return group.locationName;
  }

  @Override public String toString() {
    return MoreObjects.toStringHelper(this)
        .add("group", (this.group == null) ? null : group.title)
        .add("summary", this.summary())
        .toString();
  }
}
