package present.server.tool;

import java.util.List;
import java.util.stream.Collectors;
import present.server.model.Space;
import present.server.model.group.Group;
import present.server.model.group.Groups;

import static present.server.tool.RemoteTool.PRODUCTION_SERVER;
import static present.server.tool.RemoteTool.against;

public class AddSpaceIds {

  public static void main(String[] args) {
    against(PRODUCTION_SERVER, () -> {
      List<Group> all = Groups.all().stream()
          .filter(g -> g.spaceId == null)
          .collect(Collectors.toList());
      for (int i = 0; i < all.size(); i++) {
        all.get(i).inTransaction(g -> {
          g.spaceId = Space.WOMEN_ONLY.id;
          return true;
        });
        System.out.println(i * 100 / all.size() + "%");
      }
    });
  }
}
