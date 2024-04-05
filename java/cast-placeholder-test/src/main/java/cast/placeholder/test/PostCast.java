package cast.placeholder.test;

import present.proto.RequestHeader;
import present.wire.rpc.client.RpcClient;
import present.wire.rpc.core.RpcFilter;
import present.wire.rpc.core.RpcInvocation;
import cast.placeholder.proto.CastService;
import cast.placeholder.proto.Coordinates;
import cast.placeholder.proto.PutCastRequest;
import com.google.common.io.Resources;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.util.UUID;
import okio.ByteString;

/**
 * @author Bob Lee (bob@present.co)
 */
public class PostCast {

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
    Coordinates location = new Coordinates(37.7793568, -122.4218067, 0.0);
    String castId = UUID.randomUUID().toString();
    cs.putCast(new PutCastRequest(castId, location,
        ByteString.of(Files.readAllBytes(new File("/tmp/team.jpg").toPath())),
        "Bob's iPhone"));
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
