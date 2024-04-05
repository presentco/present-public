package present.server.tool;

import com.google.appengine.tools.remoteapi.RemoteApiInstaller;
import com.googlecode.objectify.ObjectifyService;
import com.googlecode.objectify.util.Closeable;
import java.io.IOException;
import java.util.List;
import present.server.model.PresentEntities;
import present.server.model.console.whitelist.WhitelistedUser;

public class ReindexWhitelist extends RemoteTool {

  /**
   * Ran in staging: 06/22/2017
   * Ran in production: 06/22/2017
   */
  public static void main(String[] args) throws IOException {
    //RemoteApiInstaller installer = installRemoteAPI(DEV_SERVER);
    //RemoteApiInstaller installer = installRemoteAPI(STAGING_SERVER);
    RemoteApiInstaller installer = installRemoteAPI(PRODUCTION_SERVER);

    try (Closeable closeable = ObjectifyService.begin()) {
      //NamespaceManager.set("test");
      PresentEntities.registerAll();

      int count = 0;
      List<WhitelistedUser> whitelistedUsers = WhitelistedUser.query().list();
      for (WhitelistedUser whitelistedUser : whitelistedUsers) {
        whitelistedUser.save().now();
        count++;
      }
      System.out.println("count = " + count);

      installer.uninstall();
    }
  }
}
