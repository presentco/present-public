package present.server.filter;

import present.wire.rpc.core.RpcFilterChain;
import javax.inject.Inject;

/**
 * RPC filter chain for our App Engine API.
 *
 * @author Bob Lee (bob@present.co)
 */
public class AppEngineRpcFilterChain extends RpcFilterChain {

  @Inject public AppEngineRpcFilterChain(
      OopsFilter oopsFilter,
      InternalFilter internalFilter,
      NamespaceFilter namespaceFilter,
      HeaderFilter headerFilter,
      UserFilter userFilter
  ) {
    add(oopsFilter);
    add(internalFilter);
    add(namespaceFilter);
    add(headerFilter);
    add(userFilter);
  }
}
