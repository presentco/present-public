package present.server.filter;

import present.proto.RequestHeader;
import present.server.Internal;
import present.wire.rpc.core.ClientException;
import present.wire.rpc.core.RpcFilter;
import present.wire.rpc.core.RpcInvocation;

/**
 * Restricts access to internal RPC methods.
 *
 * @author Bob Lee (bob@present.co)
 */
public class InternalFilter implements RpcFilter {

  @Override public Object filter(RpcInvocation invocation) throws Exception {
    if (invocation.getImplementationMethod().getAnnotation(Internal.class) != null) {
      RequestHeader header = (RequestHeader) invocation.getHeader();
      if (!Internal.AUTHORIZATION_KEY.equals(header.authorizationKey)) {
        throw new ClientException("Unauthorized");
      }
    }
    return invocation.proceed();
  }
}
