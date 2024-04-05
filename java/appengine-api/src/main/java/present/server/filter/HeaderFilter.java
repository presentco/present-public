package present.server.filter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import present.proto.RequestHeader;
import present.server.GsonLogging;
import present.server.RequestHeaders;
import present.server.Uuids;
import present.wire.rpc.core.ClientException;
import present.wire.rpc.core.RpcFilter;
import present.wire.rpc.core.RpcInvocation;

/**
 * Sets the Header namespace.
 *
 * @author Gabrielle Taylor (gabrielle@present.co)
 */
public class HeaderFilter implements RpcFilter {

  private static Logger logger = LoggerFactory.getLogger(HeaderFilter.class);

  // TODO: add implemntation of HeaderFilter

  @Override public Object filter(RpcInvocation invocation) throws Exception {
    final RequestHeader header = invocation.getHeader(RequestHeader.class);

    Uuids.validate(header.clientUuid);
    Uuids.validate(header.requestUuid);

    String headersJson = GsonLogging.toJson(invocation.getHeader());
    String argumentJson = GsonLogging.toJson(invocation.getArgument());
    logger.info("RPC:\nheaders: {}\n{}({})", headersJson, invocation.getMethod(), argumentJson);
    try {
      if (header == null) {
        throw new ClientException("Header provided was null.");
      }
      RequestHeaders.setCurrent(header);
      return invocation.proceed();
    } finally {
      RequestHeaders.setCurrent(null);
    }
  }
}
