package present.server.model.comment;

import com.googlecode.objectify.Key;
import com.googlecode.objectify.Ref;
import com.googlecode.objectify.annotation.Cache;
import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Id;
import com.googlecode.objectify.annotation.Index;
import com.googlecode.objectify.annotation.Load;
import com.googlecode.objectify.annotation.OnSave;
import com.googlecode.objectify.annotation.Parent;
import javax.annotation.Nullable;
import present.proto.CommentResponse;
import present.server.KeysOnly;
import present.server.model.BasePresentEntity;
import present.server.model.content.Content;
import present.server.model.group.Group;
import present.server.model.user.User;
import present.server.model.util.Coordinates;

import static com.googlecode.objectify.ObjectifyService.ofy;

/**
 * A comment in a group or chat.
 *
 * @author Bob Lee (bob@present.co)
 * @author Pat Niemeyer (pat@present.co)
 */
@Entity @Cache public class Comment extends BasePresentEntity<Comment> {

  // Fields that may be used in filter queries as strings
  public enum Fields { uuid, uuidIndex, author, creationTime, hash }

  @Id public String uuid;

  // A redundant copy of the uuid used to look up comments without using the parent entity.
  // This is maintained by the @OnSave below.
  @Index public String uuidIndex;

  @Parent public Ref<Group> group;

  @Load(unless = KeysOnly.class) @Index public Ref<User> author;

  // TODO: This is redundant with createdTime in the base present entity, however that value is not indexed.
  // TODO: Is there a way we can add the index explicitly for this subclass and use that value?
  // TODO: If so let's remove this (holdover from before the base class).
  // TODO: I don't think any migration would be needed: all comments should have the base value now.
  @Index public long creationTime = System.currentTimeMillis();

  public String text;

  public Coordinates location;

  /** An attached medium item (image or video) or null if no content is attached. */
  @Load(unless = KeysOnly.class) public Ref<Content> contentRef;

  /** The sequence number, a.k.a. "index", of this comment within its container. */
  @Index public int sequence;

  // Hash of comment contents
  @Index public String hash;

  public boolean hasContent() {
    return contentRef != null;
  }

  // Constant value of minimum length for a comment to be deemed significant.
  final int MIN_SIGNIFICANT_LENGTH = 15;

  public Group group() {
    return group.get();
  }

  // If the comment has content or the comment is long enough, the comment is significant
  // Exists is true if the the same comment has been saved, determined by hashing
  public boolean isSignificant(boolean exists) {
    // Take into account unicode characters which may be more than one.
    if ((this.text.codePointCount(0, this.text.length()) > MIN_SIGNIFICANT_LENGTH && !exists) || this.hasContent()) {
      return true;
    }
    return false;
  }

  public static Comment get(Group container, String commentId) {
    return get(container.getKey(), commentId);
  }

  public static Comment get(Key<? extends Group> containerKey, String commentId) {
    Key<Comment> commentKey = Key.create(containerKey, Comment.class, commentId);
    return ofy().load().key(commentKey).now();
  }

  // Return the comment author or null if unavailable
  public @Nullable User getAuthor() {
    if (author != null) {
      return author.get();
    }
    return null;
  }

  @Override @OnSave public void onSave() {
    super.onSave();
    this.uuidIndex = this.uuid;
    // Set hash for comment.
    this.hash = Comments.hashText(this);
  }

  /** Returns true if this comment is visible. Doesn't account for blocking. */
  public boolean visible() {
    if (deleted) return false;
    User author = getAuthor();
    if (author == null) return false;
    return author.isVisible();
  }

  public static Comment get(Key<Comment> userKey) {
    return ofy().load().key(userKey).now();
  }

  public CommentResponse toResponse() {
    return Comments.toResponse(this);
  }

  @Override public String toString() {
    return "Comment{"
        + "uuid='"
        + uuid
        + '\''
        + ", container="
        + group
        + ", author="
        + author
        + ", creationTime="
        + creationTime
        + ", index="
        + sequence
        + ", text='"
        + text
        + '\''
        + ", location="
        + location
        + ", deleted="
        + deleted
        + '}';
  }

  @Override protected Comment getThis() {
    return this;
  }
}
