package present.live.server;

import present.proto.InternalHeader;
import present.proto.LiveService;
import present.wire.rpc.server.RpcServlet;
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton public class LiveServiceServlet extends RpcServlet {
  @Inject public LiveServiceServlet(LiveService liveService, LiveFilter filter) {
    service(InternalHeader.class, LiveService.class, liveService, filter);
  }
}
