package present.server.tool;

import com.googlecode.objectify.Key;
import com.googlecode.objectify.Ref;
import java.util.List;
import present.server.model.group.Group;
import present.server.model.group.Groups;
import present.server.model.user.User;

import static com.googlecode.objectify.ObjectifyService.ofy;

public class ChangeCircleOwner {

  public static void main(String[] args) {

    RemoteTool.against(RemoteTool.PRODUCTION_SERVER, () -> {
      Key<User> keyFrom = ProductionUsers.chauntie().getKey();
      Key<User> keyTo = ProductionUsers.janete().getKey();

      List<Group> groups = Groups.query().filter("owner", keyFrom).list();

      System.out.println("Found " + groups.size() + " groups for user " + User.get(keyFrom) + "\nMigrating to " + User.get(keyTo));

      for (Group g : groups) {
        ofy().transact(() -> {
          g.reload();
          System.out.println("Moving group: " + g.title + "\n\tOwner: " + g.owner.get());
          g.owner = Ref.create(keyTo);
          g.save();
        });
      }

    });
  }
}


