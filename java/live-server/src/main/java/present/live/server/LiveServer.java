package present.live.server;

import com.google.inject.Guice;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;
import org.eclipse.jetty.server.Server;
import org.slf4j.LoggerFactory;
import present.jetty.ServerBuilder;

/**
 * Real time chat server.
 *
 * @author Bob Lee
 */
public class LiveServer {

  static {
    Logger.getLogger("present").setLevel(Level.ALL);
    Logger root = Logger.getLogger("");
    root.getHandlers()[0].setLevel(Level.ALL);
  }

  /** Key used to authorize access for internal Present servers. */
  public static final String PRIVATE_KEY = "xxx";

  public static final int DEFAULT_PORT = 8888;

  public static final String WEB_SOCKET_PATH = "/comments";

  private LiveServer() {}

  /** Creates a new server. */
  public static Server newInstance(int port) {
    return Guice.createInjector(new LiveModule())
        .getInstance(ServerBuilder.class)
        .httpsPort(port)
        .build();
  }

  /** Creates a new server. */
  public static Server newInstance() {
    return newInstance(DEFAULT_PORT);
  }

  public static void main(String[] args) throws Exception {
    LoggerFactory.getLogger(LiveServer.class).debug("Starting LiveServer...");
    Server server = newInstance();
    server.start();
    server.join();
  }
}
