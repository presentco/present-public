package present.server.tool;

import com.google.common.base.Stopwatch;

import java.io.IOException;
import present.proto.ActivityService;
import present.proto.PastActivityRequest;
import present.proto.PastActivityResponse;
import present.proto.Platform;
import present.proto.RequestHeader;
import present.server.Uuids;
import present.server.model.Space;
import present.wire.rpc.client.RpcClient;

public class GetActivity {
  enum Env {
    STAGING("https://api.staging.present.co/api", "101ddc99-6568-4663-b842-280f1de63836"),
    PRODUCTION("https://api.present.co/api", "061c97d4-9d7c-4d38-8dae-c58343179bad");

    final String url;
    final String clientId;

    Env(String url, String clientId) {
      this.url = url;
      this.clientId = clientId;
    }
  }

  public static void main(String[] args) throws IOException {
    Env env = Env.PRODUCTION;

    ActivityService as = RpcClient.create(env.url, RequestHeader.class, ActivityService.class, i -> {
      i.setHeader(new RequestHeader(env.clientId, Uuids.newUuid(), "unused",
          Platform.IOS, 1, "1", "1", null, null, null, Space.WOMEN_ONLY.id));
      return i.proceed();
    });

    // Warm up.
    PastActivityResponse pastActivity = getPastActivity(as);
    System.out.println(pastActivity.events.size() + " events");

    Stopwatch sw = Stopwatch.createStarted();
    for (int i = 0; i < 10; i++) getPastActivity(as);
    System.out.println("10X in " + sw);

    //Gson gson = new GsonBuilder().setPrettyPrinting().create();
    //System.out.println(gson.toJson(pastActivity));
  }

  private static PastActivityResponse getPastActivity(ActivityService as) throws IOException {
    System.out.println(".");
    return as.getPastActivity(new PastActivityRequest(null, null));
  }
}
