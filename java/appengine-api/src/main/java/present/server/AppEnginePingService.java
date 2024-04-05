package present.server;

import com.google.inject.Inject;
import java.io.IOException;
import present.proto.Empty;
import present.proto.PingRequest;
import present.proto.PingResponse;
import present.proto.PingService;
import present.proto.VersionCheckResult;
import present.server.model.user.Clients;

/**
 * App Engine implementation of PingService.
 *
 * @author Bob Lee (bob@present.co)
 */
public class AppEnginePingService implements PingService {
  private final RequestHeaders requestHeaders;
  private final Clients clients;

  @Inject public AppEnginePingService(RequestHeaders requestHeaders, Clients clients) {
    this.requestHeaders = requestHeaders;
    this.clients = clients;
  }

  @Override public PingResponse ping(PingRequest request) throws IOException {
    return new PingResponse(request.value);
  }

  @Deprecated @Override public VersionCheckResult versionCheck(Empty empty) throws IOException {
    return new Version(requestHeaders.current(), clients.current()).versionCheck();
  }
}
