package present.server.tool;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import present.server.KeysOnly;
import present.server.model.comment.Comment;
import present.server.model.group.Group;
import present.server.model.group.Groups;

import static com.googlecode.objectify.ObjectifyService.ofy;

public class FixLastComments {
  public static void main(String[] args) {
    RemoteTool.against(RemoteTool.STAGING_SERVER, () -> {
      List<Group> all = Groups.all();
      System.out.println("Loaded " + all.size() + " groups.");

      // Kick off comment queries in parallel.
      Map<Group, List<Comment>> byGroup = new HashMap<>();
      for (Group group : all) {
        System.out.println("Querying comments for '" + group.title + "'...");
        List<Comment> comments = ofy().load()
            .type(Comment.class)
            .ancestor(group)
            // Return the latest first
            .order("-sequence")
            .list();
        byGroup.put(group, comments);
      }

      for (int i = 0; i < all.size(); i++) {
        int percent = i * 100 / all.size();
        Group group = all.get(i);
        List<Comment> comments = byGroup.get(group);
        int activeCount = (int) comments.stream().filter(Comment::visible).count();
        int lastCommentIndex = -1;
        long lastCommentTime = group.createdTime;
        for (Comment comment : comments) {
          if (comment.visible()) {
            lastCommentIndex = comment.sequence;
            lastCommentTime = comment.createdTime;
            break;
          }
        }
        int finalLastCommentIndex = lastCommentIndex;
        long finalLastCommentTime = lastCommentTime;
        System.out.print(percent + "% ");
        if (group.activeComments == activeCount
            && group.lastCommentTime == finalLastCommentTime
            && group.lastCommentIndex == finalLastCommentIndex) {
          System.out.println("Skipped '" + group.title + "'");
        } else {
          group.inTransaction(g -> {
            g.activeComments = activeCount;
            g.lastCommentIndex = finalLastCommentIndex;
            g.lastCommentTime = finalLastCommentTime;
          });
          System.out.println("Updated '" + group.title + "'");
        }
      }
    });
  }
}
