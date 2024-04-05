package present.jetty;

import java.io.IOException;
import java.net.ServerSocket;

/**
 * Server utilities.
 *
 * @author Bob Lee (bob@present.co)
 */
public class Servers {

  private Servers() {}

  /** Finds an open port. */
  public static int randomPort() {
    try (ServerSocket socket = new ServerSocket(0)) {
      return socket.getLocalPort();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
}
