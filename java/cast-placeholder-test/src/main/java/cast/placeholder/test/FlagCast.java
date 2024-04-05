package cast.placeholder.test;

import present.proto.RequestHeader;
import present.wire.rpc.client.RpcClient;
import present.wire.rpc.core.RpcFilter;
import present.wire.rpc.core.RpcInvocation;
import cast.placeholder.proto.CastService;
import cast.placeholder.proto.Coordinates;
import cast.placeholder.proto.FlagCastRequest;
import cast.placeholder.proto.PutCastRequest;
import com.google.common.io.Resources;
import java.io.IOException;
import java.net.URL;
import java.util.UUID;
import okio.ByteString;

/**
 * @author Bob Lee (bob@present.co)
 */
public class FlagCast {

  public static void main(String[] args) throws IOException {
    String url = "https://cast-placeholder.appspot.com/api";

    RpcFilter filter = new RpcFilter() {
      @Override public Object filter(RpcInvocation invocation) throws Exception {
        invocation.setHeader(newHeader());
        return invocation.proceed();
      }
    };

    CastService cs = RpcClient.create(url, RequestHeader.class, CastService.class, filter);

    // One previous cast.
    Coordinates location = new Coordinates(1.0, 2.0, 0.0);
    cs.putCast(new PutCastRequest(UUID.randomUUID().toString(), location,
        download("https://c7.staticflickr.com/3/2598/3839030974_f55f5c9261_b.jpg"),
        "Bob's iPhone"));

    String castId = UUID.randomUUID().toString();
    cs.putCast(new PutCastRequest(castId, location,
        download("https://c8.staticflickr.com/3/2480/3838240679_60689981d0_b.jpg"),
        "Bob's iPhone"));

    cs.flagCast(new FlagCastRequest(castId));
  }

  private static ByteString download(String url) throws IOException {
    return ByteString.of(Resources.toByteArray(new URL(url)));
  }

  private static final String CLIENT_UUID = UUID.randomUUID().toString();

  private static RequestHeader newHeader() {
    return new RequestHeader(CLIENT_UUID, UUID.randomUUID().toString(), "not implemented",
        RequestHeader.Platform.IOS, 1, "1", "1");
  }
}
