package present.server.tool;

import present.server.model.group.Group;
import present.server.model.group.Groups;

public class DeleteOrphanedGroups {
  public static void main(String[] args) {
    RemoteTool.against(RemoteTool.STAGING_SERVER, () -> {
      for (Group group : Groups.all()) {
        if (group.owner.get() == null) {
          group.deleteHard();
        }
      }
    });
  }
}
