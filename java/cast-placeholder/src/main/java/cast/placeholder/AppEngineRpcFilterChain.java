package cast.placeholder;

import present.wire.rpc.core.RpcFilterChain;
import javax.inject.Inject;

/**
 * RPC filter chain for our App Engine API.
 *
 * @author Bob Lee (bob@present.co)
 */
public class AppEngineRpcFilterChain extends RpcFilterChain {

  @Inject public AppEngineRpcFilterChain(
      NamespaceFilter namespaceFilter,
      ValidationFilter validationFilter
  ) {
    add(namespaceFilter);
    add(validationFilter);
  }
}
