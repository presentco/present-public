package present.server.model.user;

import com.googlecode.objectify.Key;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import present.server.model.comment.Comment;
import present.server.model.group.Group;
import present.wire.rpc.core.ClientException;

import static com.googlecode.objectify.ObjectifyService.ofy;

/**
 * @author Pat Niemeyer (pat@present.co)
 * Date: 8/21/17
 */
public class Privileges
{
  private static final Logger logger = LoggerFactory.getLogger(Privileges.class);

  public static void assertUserOwns(Comment comment) {
    if (!userOwns(comment) && !isAdmin(comment)) {
      throw new ClientException("User does not own comment: " + comment.uuid);
    }
  }

  public static void assertUserOwns(Key<Comment> commentKey) {
    if (!userOwns(commentKey) && !isAdmin(commentKey) ) {
      throw new ClientException("User does not own comment: " + commentKey.getName());
    }
  }

  public static void assertUserOwns(Group group) {
    if (!userOwns(group) && !isAdmin(group)) {
      throw new ClientException("User does not own group: " + group.uuid());
    }
  }

  private static boolean userOwns(Group group) {
    return group.owner.equals(Users.current().getRef());
  }

  private static boolean userOwns(Comment comment) {
    return comment.author.getKey().getName().equals(Users.current().uuid);
  }

  private static boolean userOwns(Key<Comment> commentKey) {
    Comment comment = ofy().load().key(commentKey).now();
    return userOwns(comment);
  }

  // Note: test isAdmin last if logging requests
  private static boolean isAdmin(Object forEntity) {
    logger.info("Administrator " + Users.current() + " requested ownership for: "+forEntity);
    return Users.current().privileges.isAdmin;
  }

}
