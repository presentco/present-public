package present.server.filter;

import com.googlecode.objectify.Key;
import com.googlecode.objectify.Ref;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import present.proto.RequestHeader;
import present.server.GsonLogging;
import present.server.RequestHeaders;
import present.server.Uuids;
import present.server.model.user.Client;
import present.server.model.user.Clients;
import present.server.model.user.User;
import present.server.model.user.Users;
import present.server.model.util.Coordinates;
import present.wire.rpc.core.RpcFilter;
import present.wire.rpc.core.RpcInvocation;

/**
 * Validates RPC headers.
 *
 * @author Bob Lee (bob@present.co)
 */
public class UserFilter implements RpcFilter {

  private static Logger logger = LoggerFactory.getLogger(UserFilter.class);

  @Override public Object filter(RpcInvocation invocation) throws Exception {
    final RequestHeader header = invocation.getHeader(RequestHeader.class);

    Client client = Clients.getOrCreate(header.clientUuid);
    client.updateWith(header);

    logger.info("RPC:\nclient: {}\n", client.toString());
    try {
      User user = client.user();
      if (user != null) {
        Users.setCurrent(user);
        user.updateMobileClient(client);
      }
      return invocation.proceed();
    } finally {
      Users.setCurrent(null);
    }
  }
}
