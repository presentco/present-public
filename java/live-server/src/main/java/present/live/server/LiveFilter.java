package present.live.server;

import present.proto.InternalHeader;
import present.wire.rpc.core.ClientException;
import present.wire.rpc.core.RpcFilter;
import present.wire.rpc.core.RpcInvocation;

/**
 * @author Bob Lee (bob@present.co)
 */
public class LiveFilter implements RpcFilter {
  @Override public Object filter(RpcInvocation invocation) throws Exception {
    String key = invocation.getHeader(InternalHeader.class).authorizationKey;
    if (!LiveServer.PRIVATE_KEY.equals(key)) throw new ClientException("Invalid key.");

    return invocation.proceed();
  }
}
