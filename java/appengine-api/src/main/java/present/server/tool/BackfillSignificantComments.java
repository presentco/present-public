package present.server.tool;

import com.google.common.collect.Lists;
import java.util.List;
import java.util.concurrent.TimeUnit;
import present.server.model.comment.Comment;
import present.server.model.comment.Comments;
import present.server.model.group.Group;
import present.server.model.group.Groups;
import present.server.model.user.Users;

import static com.googlecode.objectify.ObjectifyService.ofy;
import static present.server.tool.RemoteTool.PRODUCTION_SERVER;
import static present.server.tool.RemoteTool.STAGING_SERVER;
import static present.server.tool.RemoteTool.against;

/**
 * Tool to backfill significant comments.
 *
 * @author Gabrielle A. Taylor {gabrielle@present.co}
 */
public class BackfillSignificantComments {
  public static void main(String[] args) {
    //against(PRODUCTION_SERVER, () -> {
    against(STAGING_SERVER, () -> {
      List<Group> groups = Groups.all();
      System.out.println("Setting significant comments for all groups.\n");
      groups.stream().forEach(g -> {
        g.updateLastSignificantComment();
        System.out.print(".");
      });
      System.out.println("\nSignificant comments set for " + groups.size() + "groups.");
    });
  }
}
