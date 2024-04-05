package present.server.tool;

import java.io.IOException;
import java.util.ArrayList;
import present.proto.GroupService;
import present.proto.NearbyGroupsRequest;
import present.proto.NearbyGroupsResponse;
import present.proto.Platform;
import present.proto.UserService;
import present.server.Protos;
import present.server.model.user.Client;
import present.server.model.user.Clients;
import present.server.model.user.User;
import present.server.model.user.Users;
import present.server.model.util.Coordinates;

import static present.server.model.user.UserState.MEMBER;

public class GetNearbyGroups {

  public static void main(String[] args) throws IOException {
    RemoteTool.against(RemoteTool.PRODUCTION_SERVER, () -> {
      User user = ProductionUsers.erinFromCnet();
      GroupService gs = user.rpcClient(GroupService.class);
      NearbyGroupsResponse groups =
          gs.getNearbyGroups(new NearbyGroupsRequest(new present.proto.Coordinates(38.1884705, -85.9576449, 0.0), null));
      System.out.println(groups.nearbyGroups.size());
    });
  }
}
