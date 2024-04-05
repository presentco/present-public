package present.server.email;

import com.google.common.base.MoreObjects;
import present.server.model.content.Content;
import present.server.model.group.Group;

/**
 * Group activity summary
 *
 * @author Bob Lee (bob@present.co)
 */
public class GroupSummary {

  public final Group group;
  public final int comments;

  public GroupSummary(Group group, int comments) {
    this.group = group;
    this.comments = comments;
  }

  public String icon() {
    if (group.coverContent == null) return SummaryEmail.PLACEHOLDER_IMAGE;
    Content content = group.coverContent.get();
    return content.circleUrl(150);
  }

  public String summary() {
    return comments == 1 ? "1 New Comment" : comments + " New Comments";
  }

  @Override public String toString() {
    return MoreObjects.toStringHelper(this)
        .add("group", (this.group == null) ? null : group.title)
        .add("comments", this.comments)
        .add("summary", this.summary())
        .toString();
  }
}
