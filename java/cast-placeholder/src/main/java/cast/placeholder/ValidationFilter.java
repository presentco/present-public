package cast.placeholder;

import present.proto.RequestHeader;
import present.wire.rpc.core.RpcFilter;
import present.wire.rpc.core.RpcInvocation;

/**
 * Validates RPC headers.
 *
 * @author Bob Lee (bob@present.co)
 */
public class ValidationFilter implements RpcFilter {

  @Override public Object filter(RpcInvocation invocation) throws Exception {
    final RequestHeader header = invocation.getHeader(RequestHeader.class);

    // TODO: Check authorization key.
    Uuids.validate(header.clientUuid);
    Uuids.validate(header.requestUuid);

    return invocation.proceed();
  }
}
