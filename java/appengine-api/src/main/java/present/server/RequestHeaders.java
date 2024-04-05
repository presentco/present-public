package present.server;

import present.proto.Platform;
import present.proto.RequestHeader;
import present.server.model.util.Coordinates;
import present.wire.rpc.core.RpcInvocation;

/**
 * RequestHeader utility.
 *
 * @author Bob Lee (bob@present.co)
 */
public class RequestHeaders {

  private static ThreadLocal<RequestHeader> localRequestHeader = new ThreadLocal<>();

  /**
   * Gets the request header for the current thread.
   * @throws NullPointerException if we are not in the context of an RPC call.
   */
  public static RequestHeader current() {
    return localRequestHeader.get();
  }

  public static boolean test;

  /**
   * Return true if there is a current RPC contex and it is from the TEST platform.
   */
  public static boolean isTest() {
    if (test) return true;
    RpcInvocation invocation = RpcInvocation.current(false);
    RequestHeader header = invocation != null ? invocation.getHeader(RequestHeader.class) : null;
    return header != null && header.platform == Platform.TEST;
  }

  /**
   * Sets the current ThreadLocal request header.
   * @param requestHeader current request header
   */
  public static void setCurrent(RequestHeader requestHeader) {
    localRequestHeader.set(requestHeader);
  }

  /** Returns the user's selected or current location (in that order). */
  public static Coordinates location() {
    RequestHeader current = current();
    if (current.selectedLocation != null) return Coordinates.fromProto(current.selectedLocation);
    return Coordinates.fromProto(current.location);
  }

  /** Sets the location in the request headers. */
  public static void setLocation(Coordinates location) {
    setCurrent(RequestHeaders.current().newBuilder().selectedLocation(location.toProto()).build());
  }
}
