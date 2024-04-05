package present.web;

import java.io.IOException;
import java.util.UUID;
import present.proto.Platform;
import present.proto.RequestHeader;
import present.proto.ResolveUrlRequest;
import present.proto.ResolveUrlResponse;
import present.proto.UrlResolverService;
import present.server.environment.Environment;
import present.wire.rpc.client.RpcClient;

/**
 * Resolves URLs. Note: Resolving URLs doesn't require auth.
 *
 * @author Bob Lee (bob@present.co)
 */
public class UrlResolvers {

  private static final String CLIENT_ID = "00000000-0000-0000-0000-000000000000";

  private static final UrlResolverService urlResolver = RpcClient.create(
      Environment.current().apiUrl(),
      RequestHeader.class,
      UrlResolverService.class,
      invocation -> {
        invocation.setHeader(new RequestHeader.Builder()
            .clientUuid(CLIENT_ID)
            .requestUuid(UUID.randomUUID().toString())
            .authorizationKey("TODO")
            .platform(Platform.INTERNAL)
            .apiVersion(0)
            .build()
        );
        return invocation.proceed();
      }
  );

  public static ResolveUrlResponse resolve(String url) throws IOException {
    return urlResolver.resolveUrl(new ResolveUrlRequest(url));
  }
}
