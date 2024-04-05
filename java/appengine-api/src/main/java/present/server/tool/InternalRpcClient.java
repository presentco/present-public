package present.server.tool;

import java.util.UUID;
import present.proto.Platform;
import present.proto.RequestHeader;
import present.server.Internal;
import present.server.Uuids;
import present.server.environment.Environment;
import present.server.model.Space;
import present.wire.rpc.client.RpcClient;
import present.wire.rpc.core.RpcFilter;
import present.wire.rpc.core.RpcProtocol;

/**
 * Creates internal RPC clients.
 *
 * @author Bob Lee (bob@present.co)
 */
public class InternalRpcClient {

  private static RpcFilter filter = invocation -> {
    invocation.setHeader(newHeader());
    return invocation.proceed();
  };

  public static <T> T create(String apiUrl, Class<T> serviceType) {
    return RpcClient.create(RpcProtocol.PROTO, apiUrl, RequestHeader.class, serviceType, filter);
  }

  public static <T> T create(Class<T> serviceType) {
    return create(Environment.current().apiUrl(), serviceType);
  }

  static RequestHeader newHeader() {
    return new RequestHeader(Uuids.NULL, UUID.randomUUID().toString(), Internal.AUTHORIZATION_KEY,
        Platform.INTERNAL,1, "1", "1", null, null, null,
        Space.WOMEN_ONLY.id);
  }
}
