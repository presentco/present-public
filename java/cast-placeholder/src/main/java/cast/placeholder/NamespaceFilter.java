package cast.placeholder;

import present.proto.RequestHeader;
import present.wire.rpc.core.RpcFilter;
import present.wire.rpc.core.RpcInvocation;
import com.google.appengine.api.NamespaceManager;

/**
 * Sets the Datastore namespace.
 *
 * @author Bob Lee (bob@present.co)
 */
public class NamespaceFilter implements RpcFilter {

  @Override public Object filter(RpcInvocation invocation) throws Exception {
    RequestHeader header = invocation.getHeader(RequestHeader.class);
    if (header.platform == RequestHeader.Platform.TEST) {
      String oldNamespace = NamespaceManager.get();
      NamespaceManager.set("test");
      try {
        return invocation.proceed();
      } finally {
        NamespaceManager.set(oldNamespace);
      }
    } else {
      return invocation.proceed();
    }
  }
}
