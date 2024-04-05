package cast.placeholder;

import present.proto.RequestHeader;
import present.wire.rpc.server.RpcServlet;
import cast.placeholder.proto.CastService;
import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * @author Bob Lee (bob@present.co)
 */
@Singleton public class AppEngineRpcServlet extends RpcServlet {

  @Inject public AppEngineRpcServlet(AppEngineCastService castService,
      AppEngineRpcFilterChain filter) {
    service(RequestHeader.class, CastService.class, castService, filter);
  }
}
