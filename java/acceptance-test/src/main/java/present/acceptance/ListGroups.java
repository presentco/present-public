package present.acceptance;

import present.proto.GroupResponse;
import present.proto.GroupService;
import present.proto.Coordinates;
import present.proto.NearbyGroupsRequest;
import present.proto.NearbyGroupsResponse;
import present.proto.Platform;
import present.proto.RequestHeader;
import present.server.model.Space;
import present.wire.rpc.client.RpcClient;
import present.wire.rpc.core.RpcFilter;
import java.io.IOException;
import java.util.UUID;

public class ListGroups {
  public static void main(String[] args) throws IOException {
    RpcFilter filter = invocation -> {
      invocation.setHeader(newHeader());
      return invocation.proceed();
    };

    GroupService bs = RpcClient.create("https://api-dot-present-production.appspot.com/api",
        RequestHeader.class, GroupService.class, filter);

    double latitude = 37.77151;
    double longitude = -122.3965447;
    NearbyGroupsResponse nearbyGroups = bs.getNearbyGroups(
        new NearbyGroupsRequest(new Coordinates(latitude, longitude, 0d), null));

    for (GroupResponse bubble : nearbyGroups.nearbyGroups) {
      double x = bubble.location.latitude - latitude;
      double y = bubble.location.longitude - longitude;
      System.out.println("\"" + bubble.title + "\", " + bubble.location.latitude
          + ", " + bubble.location.longitude);
    }
  }

  private static final String clientUuid = UUID.randomUUID().toString();

  private static RequestHeader newHeader() {
    return new RequestHeader(clientUuid, UUID.randomUUID().toString(), "not implemented",
        Platform.IOS, 1, "1", "1", null, null, null, Space.WOMEN_ONLY.id);
  }
}
