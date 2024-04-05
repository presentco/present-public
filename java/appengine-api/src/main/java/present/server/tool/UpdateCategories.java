package present.server.tool;

import java.util.List;
import java.util.stream.Collectors;
import present.server.KeysOnly;
import present.server.model.group.Category;
import present.server.model.group.Group;

import static com.googlecode.objectify.ObjectifyService.ofy;
import static present.server.tool.RemoteTool.PRODUCTION_SERVER;
import static present.server.tool.RemoteTool.STAGING_SERVER;
import static present.server.tool.RemoteTool.against;

public class UpdateCategories {
  public static void main(String[] args) {
    against(STAGING_SERVER, () -> {
      List<Group> groups = ofy().load().group(KeysOnly.class).type(Group.class).list();
      for (Group group : groups) {
        List<String> updated = group.categories.stream()
            .map(Category::map)
            .filter(Category::isValid)
            .distinct()
            .collect(Collectors.toList());
        if (!updated.equals(group.categories)) {
          System.out.println(group.title + " " + group.categories + " -> " + updated);
          group.inTransaction(g -> {
            g.categories = updated;
          });
        }
      }
    });
  }
}
