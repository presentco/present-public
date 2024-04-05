package present.server.model.comment;

import com.google.common.base.Charsets;
import com.google.common.collect.Streams;
import com.google.common.hash.HashFunction;
import com.google.common.hash.Hashing;
import com.googlecode.objectify.cmd.Loader;
import com.googlecode.objectify.cmd.Query;
import java.util.Base64;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import present.proto.CommentResponse;
import present.proto.ContentResponse;
import present.server.KeysOnly;
import present.server.Time;
import present.server.model.PresentEntities;
import present.server.model.group.Group;
import present.server.model.group.Groups;
import present.server.model.user.User;
import present.wire.rpc.core.ServerException;

import static com.googlecode.objectify.ObjectifyService.ofy;

/**
 * Comment factories / utilities.
 * @author Pat Niemeyer (pat@pat.net)
 */
public class Comments {
  private static final Logger logger = LoggerFactory.getLogger(Comments.class);

  private static final int MAX_COMMENTS = 250;

  public static Query<Comment> query() { return ofy().load().type(Comment.class); }
  public static Query<Comment> active() { return PresentEntities.active(query()); }
  public static Iterable<Comment> all() { return query().iterable(); }
  public static Stream<Comment> stream() { return Streams.stream(all()); }

  public static List<Comment> thisWeek() {
    long cutoff = System.currentTimeMillis() - Time.DAY_IN_MILLIS;
    return query().filter(Comment.Fields.creationTime + " >", cutoff).list();
  }

  public static List<Comment> thisMonth() {
    long cutoff = System.currentTimeMillis() - Time.MONTH_IN_MILLIS;
    return query().filter(Comment.Fields.creationTime + " >", cutoff).list();
  }

  /**
   * Get comments for the specified user from the comment container (e.g. Group or Chat).
   * If applicable, deleted comments and comments from blocked users are removed.
   * @param user may be null for an unregistered user.
   */
  public static List<Comment> getComments(Group group, @Nullable User user, boolean transitive) {
    Predicate<Comment> filter = comment -> {
      if (comment.deleted) return false;
      User author = comment.author.get();
      if (user == null) return author.isVisible();
      return user.canSee(author);
    };
    Loader loader = ofy().load();
    if (!transitive) loader = loader.group(KeysOnly.class);
    return loader
        .type(Comment.class)
        .ancestor(group)
        // Return the latest first
        .order("-sequence")
        .limit(MAX_COMMENTS)
        .list()
        .stream()
        .filter(filter)
        .collect(Collectors.toList());
  }

  public static @Nullable Comment findByUuid(String uuid) {
    List<Comment> groups = query().filter(Comment.Fields.uuidIndex.name(), uuid).list();
    if (groups.size() > 1) {
      throw new ServerException("unexpected duplicate comments");
    }
    return groups.size() == 1 ? groups.get(0) : null;
  }

  public static CommentResponse toResponse(Comment comment) {
    String containerId = Groups.getUuidFromId(comment.group.getKey().getName());
    ContentResponse content = comment.hasContent() ? comment.contentRef.get().toResponse() : null;
    return new CommentResponse.Builder()
        .uuid(comment.uuid)
        .groupId(containerId)
        .author(comment.author.get().toResponse(false))
        .creationTime(comment.creationTime)
        .comment(trim(comment.text))
        .likes(0)
        .content(content)
        .deleted(comment.deleted)
        .index(comment.sequence)
        .build();
  }

  private static String trim(String s) {
    return s == null ? null : s.trim();
  }

  private static final byte[] PRIVATE_KEY =
      Base64.getDecoder().decode("xxx");
  private static final HashFunction hmac = Hashing.hmacSha256(PRIVATE_KEY);

  /** Hashes the text of the given comment. */
  public static String hashText(Comment c) {
    return c.text == null ? null : hashText(c.text);
  }

  /** Hashes the text of the given comment. */
  private static String hashText(String text) {
    return text == null ? null : hmac.hashString(text, Charsets.UTF_8).toString();
  }

  /**
   * To be used when determining if a duplicate comment exists.
   * This takes into account the existence of the comment.
   */
  public static boolean duplicateExists(Comment c) {
    return ofy().load().type(Comment.class).filter(Comment.Fields.hash.name(), hashText(c)).keys()
        .list().stream().filter(k -> !k.equals(c.getKey())).findFirst().isPresent();
  }

  /**
   * To be used when determining if a comment with the same text exists.
   * This assumes that the comment does not exist yet.
   */
  public static boolean textExists(String text) {
    return !ofy().load().type(Comment.class).filter(Comment.Fields.hash.name(), hashText(text)).keys().list().isEmpty();
  }
}
