package cast.placeholder;

import present.proto.RequestHeader;
import present.wire.rpc.core.RpcInvocation;
import com.google.appengine.api.utils.SystemProperty;

import static present.proto.RequestHeader.Platform.TEST;
import static com.google.appengine.api.utils.SystemProperty.Environment.Value.Development;

/**
 * @author Bob Lee (bob@present.co)
 */
public class Environment {

  public static boolean inDevelopment() {
    return SystemProperty.environment.value() == Development;
  }

  public static boolean inTest() {
    return RpcInvocation.current().getHeader(RequestHeader.class).platform == TEST;
  }
}
