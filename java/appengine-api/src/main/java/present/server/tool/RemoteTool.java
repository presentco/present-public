package present.server.tool;

import com.google.appengine.tools.remoteapi.RemoteApiInstaller;
import com.google.appengine.tools.remoteapi.RemoteApiOptions;
import com.google.common.base.Stopwatch;
import com.googlecode.objectify.ObjectifyService;
import com.googlecode.objectify.util.Closeable;
import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import present.server.PresentObjectifyFactory;
import present.server.model.PresentEntities;
import present.server.model.user.User;

/**
 * Tools to access servers remotely.
 *
 * @author pat@pat.net
 * Date: 8/31/16
 */
public abstract class RemoteTool {

  private static final Logger logger = LoggerFactory.getLogger(RemoteTool.class);

  public final static String PRODUCTION_SERVER = "api-dot-present-production.appspot.com";
  public final static String STAGING_SERVER = "api-dot-present-staging.appspot.com";
  public final static String DEV_SERVER = "localhost";
  public final static int SERVER_PORT = 443;
  public final static int DEV_SERVER_PORT = 8081;

  public static RemoteApiInstaller installRemoteAPI(String server) throws IOException {
    RemoteApiOptions options;
    if (server.equals(DEV_SERVER)) {
      options =
          new RemoteApiOptions().server(server, DEV_SERVER_PORT).useDevelopmentServerCredential();
    } else {
      options =
          new RemoteApiOptions().server(server, SERVER_PORT).useApplicationDefaultCredential();
    }
    RemoteApiInstaller installer = new RemoteApiInstaller();
    installer.install(options);
    return installer;
  }

  public static void against(String server, Task task) {
    try {
      RemoteApiInstaller installer = installRemoteAPI(server);
      ObjectifyService.setFactory(new PresentObjectifyFactory());
      Stopwatch sw = Stopwatch.createUnstarted();
      try (Closeable closeable = ObjectifyService.begin()) {
        PresentEntities.registerAll();
        sw.start();
        task.run();
      } finally {
        logger.info("Completed in {}.", sw);
        installer.uninstall();
      }
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  public interface Task {
    void run() throws Exception;
  }
}
