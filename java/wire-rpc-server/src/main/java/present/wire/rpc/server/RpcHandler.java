package present.wire.rpc.server;

import java.io.PrintWriter;
import java.io.StringWriter;
import present.wire.rpc.core.RpcMethod;
import java.io.IOException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

interface RpcHandler {

  void handle(HttpServletRequest httpRequest, HttpServletResponse httpResponse,
      RpcService service, RpcMethod method) throws IOException;

  static String toString(Throwable t) {
    StringWriter stackTrace = new StringWriter();
    t.printStackTrace(new PrintWriter(stackTrace, true));
    return stackTrace.toString();
  }
}
