package present.server.tool;

import java.util.List;
import present.server.model.group.Group;
import present.server.model.group.Groups;

import static present.server.tool.RemoteTool.STAGING_SERVER;
import static present.server.tool.RemoteTool.against;

public class ReindexGroups {

  public static void main(String[] args) {
    against(STAGING_SERVER, () -> {
      List<Group> all = Groups.all();
      int count = 0;
      for (Group group : all) {
        System.out.println(count++ + " of " + all.size());
        group.inTransaction(g -> {});
      }
    });
  }
}
