package present.live.client;

import present.proto.InternalHeader;
import present.proto.LiveService;
import present.wire.rpc.client.RpcClient;
import present.wire.rpc.core.RpcFilter;
import present.wire.rpc.core.RpcInvocation;

/**
 * Connects to remote LiveService providers.
 *
 * @author Bob Lee (bob@present.co)
 */
public class InternalLiveClient {

  /** Must match LiveServer#PRIVATE_KEY. */
  private static final String PRIVATE_KEY = "xxx";

  public static final LiveService connectTo(String url) {
    return RpcClient.create(url, InternalHeader.class, LiveService.class, new RpcFilter() {
      @Override public Object filter(RpcInvocation invocation) throws Exception {
        invocation.setHeader(new InternalHeader(PRIVATE_KEY));
        return invocation.proceed();
      }
    });
  }
}
