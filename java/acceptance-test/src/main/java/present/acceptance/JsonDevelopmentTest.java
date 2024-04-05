package present.acceptance;

import present.wire.rpc.core.RpcProtocol;

public class JsonDevelopmentTest extends DevelopmentTest {

  @Override RpcProtocol protocol() {
    return RpcProtocol.JSON;
  }
}
