package present.server.tool;

import com.googlecode.objectify.Key;
import com.googlecode.objectify.Ref;
import java.io.IOException;
import present.server.model.comment.Comments;
import present.server.model.group.Groups;
import present.server.model.group.JoinedGroups;
import present.server.model.user.User;

/**
 * Migrates content from one account to another.
 *
 * @author Bob Lee (bob@present.co)
 */
public class MigrateAccount {
  public static void main(String[] args) throws IOException {
    String kassiaFrom = "c9eb6c36-b7c2-43e7-851f-6b07d8b84b50";
    String kassiaTo = "e3d4a3aa-9fcf-4680-b9b6-7553309d1ed0";

    String chantelleFrom = "a1e7b795-6b24-4203-a35a-b8cfde7f7f17";
    String chantelleTo = "a078ff7d-f088-4894-b2fc-01f630c8ff2e";

    RemoteTool.against(RemoteTool.PRODUCTION_SERVER, () -> {
      Key<User> from = User.keyFor(chantelleFrom);
      Key<User> to = User.keyFor(chantelleTo);

      Groups.query().filter("owner", from).list().forEach(g -> {
        System.out.println("Moving group: " + g);
        g.owner = Ref.create(to);
        g.save();
      });

      Comments.query().filter("author", from).list().forEach(c -> {
        System.out.println("Moving comment: " + c);
        c.author = Ref.create(to);
        c.save();
      });

      JoinedGroups joinedGroups = JoinedGroups.getOrCreate(User.get(from));
      if (joinedGroups != null) {
        System.out.println("Deleting saved groups: " + joinedGroups);
        joinedGroups.deleteHard();
      }
    });
  }
}
