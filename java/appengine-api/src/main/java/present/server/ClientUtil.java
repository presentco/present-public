package present.server;

import java.util.UUID;
import present.proto.Platform;
import present.proto.RequestHeader;
import present.server.model.Space;
import present.server.model.util.Coordinates;
import present.wire.rpc.client.RpcClient;
import present.wire.rpc.core.RpcFilter;
import present.wire.rpc.core.RpcProtocol;

/**
 * Support calling the server from acceptance tests and ad-hoc server tools.
 */
public class ClientUtil
{

  /**
   * Construct an rpcClient using a default header generator for the specified clientUuid.
   */
  public static <T> T rpcClient(String apiUrl, Class<T> serviceType, RpcProtocol protocol, String clientUuid, Platform platform) {
    return RpcClient.create(protocol, apiUrl, RequestHeader.class, serviceType,
        filter(new DefaultHeaderGenerator(clientUuid, platform, null)));
  }

  public static <T> T rpcClient(String apiUrl, Class<T> serviceType, RpcProtocol protocol, HeaderGenerator headerGenerator) {
    return RpcClient.create(protocol, apiUrl, RequestHeader.class, serviceType, filter(headerGenerator));
  }

  private static RpcFilter filter(HeaderGenerator headerGenerator)
  {
    return invocation -> {
      invocation.setHeader(headerGenerator.newHeader());
      return invocation.proceed();
    };
  }

  public interface HeaderGenerator {
    RequestHeader newHeader();
    void setClientUuid(String clientUuid);
  }

  public static class DefaultHeaderGenerator implements  HeaderGenerator {
    private String clientUuid;
    private Platform platform;
    private present.proto.Coordinates location;

    public DefaultHeaderGenerator(String clientUuid, Platform platform,
        present.proto.Coordinates location) {
      this.clientUuid = clientUuid;
      this.platform = platform;
      this.location = location;
    }

    public void setClientUuid(String clientUuid) {
      this.clientUuid = clientUuid;
    }

    public RequestHeader newHeader() {
      if (clientUuid == null) {
        throw new RuntimeException("clientUuid is null");
      }
      return new RequestHeader(clientUuid, UUID.randomUUID().toString(),
          "not implemented", platform, 1, "1",
          "1", location, null,
          null, Space.EVERYONE.id);
    }

  }

}
