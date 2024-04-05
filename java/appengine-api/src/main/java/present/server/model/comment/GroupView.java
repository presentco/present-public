package present.server.model.comment;

import com.googlecode.objectify.Ref;
import com.googlecode.objectify.annotation.AlsoLoad;
import com.googlecode.objectify.annotation.Cache;
import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Id;
import com.googlecode.objectify.annotation.Index;
import com.googlecode.objectify.annotation.Parent;
import com.googlecode.objectify.cmd.Query;
import com.googlecode.objectify.condition.IfTrue;
import javax.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import present.server.model.BasePresentEntity;
import present.server.model.group.Group;
import present.server.model.user.UnreadStates;
import present.server.model.user.User;

import static com.googlecode.objectify.ObjectifyService.ofy;

/**
 * A user's view of a group.
 *
 * @author Pat Niemeyer (pat@pat.net)
 */
@Entity(name="CommentContainerView") @Cache
public class GroupView extends BasePresentEntity<GroupView> {

  // Field names that may be used in filter queries as strings
  public enum Fields {
    uuid, container, muted
  }

  private static final Logger logger = LoggerFactory.getLogger(GroupView.class);

  public static Query<GroupView> query() {
    return ofy().load().type(GroupView.class);
  }

  /** Group UUID. */
  @Id public String uuid;

  /** The viewing user. */
  @Parent public Ref<User> user;

  /** The viewed group. */
  @Index public Ref<Group> container;

  /** Index of the last comment read. Null if nothing read. -1 if the group has been opened. */
  @AlsoLoad("readMarker") @Nullable public Integer lastRead;

  /** The user has muted the group. No notifications should be sent. */
  @Index(IfTrue.class) public boolean muted = false;

  public GroupView() {}

  public GroupView(User user, Group group) {
    this.uuid = group.uuid();
    this.user = user.getRef();
    this.container = group.getRef();
  }

  /** Index of the last comment read. Returns -1 if the user has read no comments. */
  public int lastRead() {
    if (lastRead == null) return -1;
    return lastRead;
  }

  public boolean isRead() {
    if (lastRead == null) return false;
    return lastRead >= container.get().lastCommentIndex();
  }

  /**
   * Called when the members of a group change as this can affect whether or not the group shows
   * up as unread.
   */
  public void membersChanged() {
    Group group = container.get();

    if (group.owner.equals(user.get()) && group.hasJoinRequests()) {
      // Keep unread.
      return;
    }

    if (isRead()) {
      UnreadStates.markGroupRead(user.get(), group.id);
    }
  }

  /** Returns an unread count for a user in this group. */
  public int unreadCount() throws IllegalArgumentException {
    Group container = this.container.get();

    if (this.lastRead == null) {
      // If user has no view, or user has not read any comments,
      // return highest sequence number + 1 (sequence number zero indexed)
      return container.activeComments;
    }

    return container.lastCommentIndex() - this.lastRead();
  }

  public GroupView markAsRead() {
    Group commentContainer = container.get();
    // Container is null if this is the first comment.
    lastRead = commentContainer == null ? 0 : commentContainer.lastCommentIndex();
    logger.debug("reset sentSinceLastReadCount");
    return this;
  }

  public GroupView mute() {
    muted = true;
    return this;
  }

  public GroupView unmute() {
    muted = false;
    return this;
  }

  @Override protected GroupView getThis() {
    return this;
  }
}

