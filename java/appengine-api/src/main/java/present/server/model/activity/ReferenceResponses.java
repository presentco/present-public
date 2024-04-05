package present.server.model.activity;

import com.google.common.base.Function;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.googlecode.objectify.Key;
import com.googlecode.objectify.Ref;
import java.util.List;
import present.proto.ReferenceResponse;
import present.server.Keys;
import present.server.model.PresentEntity;
import present.server.model.comment.Comment;
import present.server.model.group.Group;
import present.server.model.group.Groups;
import present.server.model.user.User;
import present.server.model.user.Users;
import present.wire.rpc.core.ServerException;

/**
 * @author Pat Niemeyer (pat@pat.net)
 */
public class ReferenceResponses {

  /**
   * Render an interable of entity references to short reference responses, including only the
   * entity ids.
   */
  public static List<ReferenceResponse> toShortReferenceResponses(Iterable<Ref> targetEntities) {
    return Lists.newArrayList(
        Iterables.transform(targetEntities,
            new Function<Ref, ReferenceResponse>() {
              @Override public ReferenceResponse apply(Ref entity) {
                return toShortReferenceResponse(entity);
              }
            }));
  }

  /**
   * Objectify Ref to Reference Response.
   */
  public static ReferenceResponse toShortReferenceResponse(Ref ref) {
    Key key = ref.getKey();
    ReferenceResponse.Builder resp = new ReferenceResponse.Builder();

    // User
    if (Keys.typeEquals(key, User.class)) {
      String userId = key.getName();
      return resp.userId(userId).build();
    }

    // Group
    if (Keys.typeEquals(key, Group.class)) {
      String groupId = Groups.getUuidFromId(key.getName());
      return resp.groupId(groupId).build();
    }

    // Comment
    if (Keys.typeEquals(key, Comment.class)) {
      String groupCommentId = key.getName();
      return resp.commentId(groupCommentId).build();
    }

    throw new ServerException("invalid entity type: " + key.getKind());
  }

  /**
   * Entity to Reference Response.
   */
  public static ReferenceResponse toFullReferenceResponse(PresentEntity entity) {

    ReferenceResponse.Builder resp = new ReferenceResponse.Builder();

    // User
    if (entity instanceof User) {
      User user = (User) entity;
      return resp.user(user.toResponse()).build();
    }

    // Group
    if (entity instanceof Group) {
      Group group = (Group) entity;
      return resp.group(group.toResponseFor(Users.current(false))).build();
    }

    // Comment
    if (entity instanceof Comment) {
      Comment comment = (Comment) entity;

      if (Keys.typeEquals(comment.group, Group.class)) {
        Comment groupComment = comment;
        return resp.comment(groupComment.toResponse()).build();
      }
    }

    throw new ServerException("invalid entity type: " + entity.getClass());
  }
}
