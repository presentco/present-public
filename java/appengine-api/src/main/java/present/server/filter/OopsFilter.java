package present.server.filter;

import com.google.common.cache.LoadingCache;
import java.lang.reflect.InvocationTargetException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import present.proto.Platform;
import present.proto.RequestHeader;
import present.server.Caches;
import present.server.environment.Environment;
import present.wire.rpc.client.RpcClient;
import present.wire.rpc.core.RpcFilter;
import present.wire.rpc.core.RpcInvocation;
import present.wire.rpc.core.RpcMethod;

public class OopsFilter implements RpcFilter {

  private static Logger logger = LoggerFactory.getLogger(OopsFilter.class);

  private static final ThreadLocal<RequestHeader> localHeader = new ThreadLocal<>();

  private static final RpcFilter headerFilter = invocation -> {
    invocation.setHeader(localHeader.get());
    return invocation.proceed();
  };

  private static LoadingCache<Class<?>, Object> clients = Caches.create(
      key -> RpcClient.create("https://api.present.co/api", RequestHeader.class, key,
          headerFilter));

  @Override public Object filter(RpcInvocation invocation) throws Exception {
    RequestHeader header = invocation.getHeader(RequestHeader.class);
    if (Environment.isStaging()
        && header.platform == Platform.IOS
        && "1.2b9".equals(header.clientVersion)) {
      logger.info("Forwarding request to production.");
      RpcMethod method = invocation.getMethod();
      Object service = clients.get(method.service());
      try {
        localHeader.set(header);
        return method.method().invoke(service, invocation.getArgument());
      } catch (InvocationTargetException e) {
        throw (Exception) e.getCause();
      } finally {
        localHeader.remove();
      }
    }
    return invocation.proceed();
  }
}
