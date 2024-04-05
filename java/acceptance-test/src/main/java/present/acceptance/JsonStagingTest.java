package present.acceptance;

import present.wire.rpc.core.RpcProtocol;

public class JsonStagingTest extends StagingTest {
  @Override RpcProtocol protocol() {
    return RpcProtocol.JSON;
  }
}
