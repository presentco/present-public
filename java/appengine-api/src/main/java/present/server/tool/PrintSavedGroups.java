package present.server.tool;

import com.google.common.base.Stopwatch;
import com.googlecode.objectify.Ref;
import java.util.Objects;
import present.proto.GroupService;
import present.server.model.group.JoinedGroups;

public class PrintSavedGroups {
  public static void main(String[] args) {
    RemoteTool.against(RemoteTool.PRODUCTION_SERVER, () -> {
      JoinedGroups sg = JoinedGroups.getOrCreate(ProductionUsers.janete());
      System.out.println(
          sg.groups.stream().map(Ref::get).filter(Objects::nonNull)
              .filter(g -> !g.deleted).count());
      System.out.println();

      Stopwatch sw = Stopwatch.createStarted();
      GroupService gs =
          ProductionUsers.janete().virtualClient().rpcSimulator(GroupService.class);
      gs.getJoinedGroups(null);
      System.out.println(sw);
    });
  }
}
