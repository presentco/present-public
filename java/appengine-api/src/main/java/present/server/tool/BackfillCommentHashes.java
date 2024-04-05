package present.server.tool;

import com.google.common.collect.Streams;
import java.util.List;
import present.server.model.comment.Comments;
import present.server.model.group.Group;
import present.server.model.group.Groups;

import static present.server.tool.RemoteTool.STAGING_SERVER;
import static present.server.tool.RemoteTool.against;

/**
 * Tool to backfill hashes for all comments.
 *
 * @author Gabrielle A. Taylor {gabrielle@present.co}
 */
public class BackfillCommentHashes {
  public static void main(String[] args) {
    //against(PRODUCTION_SERVER, () -> {
    against(STAGING_SERVER, () -> {
      List<Group> groups = Groups.all();
      System.out.println("Setting hashes for all comments for all groups.\n");
      groups.stream().forEach(g -> {
        Streams.stream(g.activeComments(false))
            .filter(c -> c.hash == null)
            .forEach(c ->
                c.inTransaction(comment -> {
                  comment.hash = Comments.hashText(comment);
                  System.out.print(".");
                  return true;
                })
        );
      });
      System.out.println("\nSignificant comments set for " + groups.size() + "groups.");
    });
  }
}
