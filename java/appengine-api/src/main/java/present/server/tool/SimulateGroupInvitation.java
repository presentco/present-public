package present.server.tool;

import java.io.IOException;
import java.util.Collections;

import present.proto.GroupService;
import present.proto.InviteFriendsRequest;
import present.proto.Platform;
import present.proto.RequestHeader;
import present.server.Uuids;
import present.server.model.Space;
import present.wire.rpc.client.RpcClient;

public class SimulateGroupInvitation {
  enum Env {
    STAGING(
        "https://api.staging.present.co/api",
        "101ddc99-6568-4663-b842-280f1de63836",
        null,
        null
    ),

    PRODUCTION(
        "https://api.present.co/api",
        "061c97d4-9d7c-4d38-8dae-c58343179bad", // Janete
        "a75182f6-e86f-4985-8075-f618ce7f3d45",
        "02E9E738-3165-4214-8E5C-056F39945A98"
    );

    final String url;
    final String clientId;
    final String bob;
    final String group;

    Env(String url, String clientId, String bob, String group) {
      this.url = url;
      this.clientId = clientId;
      this.bob = bob;
      this.group = group;
    }
  }

  public static void main(String[] args) throws IOException {
    Env env = Env.PRODUCTION;

    GroupService gs = RpcClient.create(env.url, RequestHeader.class, GroupService.class, i -> {
      i.setHeader(new RequestHeader(env.clientId, Uuids.newUuid(), "unused",
          Platform.IOS, 1, "1", "1", null, null, null, Space.WOMEN_ONLY.id));
      return i.proceed();
    });

    gs.inviteFriends(new InviteFriendsRequest(env.group, Collections.singletonList(env.bob)));
  }
}
