package present.server.email;

import com.google.common.base.MoreObjects;
import present.server.model.content.Content;
import present.server.model.group.Group;

/**
 * Join activity summary
 *
 * @author Gabrielle Taylor (gabrielle@present.co)
 */
public class JoinSummary {

  public final Group group;
  public final int joins;

  public JoinSummary(Group group, int joins) {
    this.group = group;
    this.joins = joins;
  }

  public String icon() {
    if (group.coverContent == null) return SummaryEmail.PLACEHOLDER_IMAGE;
    Content content = group.coverContent.get();
    return content == null ? SummaryEmail.PLACEHOLDER_IMAGE : content.circleUrl(150);
  }

  public String summary() {
    return joins == 1 ? "1 Join" : joins + " Joins";
  }

  @Override public String toString() {
    return MoreObjects.toStringHelper(this)
        .add("group", (this.group == null) ? null : group.title)
        .add("joins", this.joins)
        .add("summary", this.summary())
        .toString();
  }
}
