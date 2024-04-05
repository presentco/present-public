package present.server.tool.migrations;

import com.google.appengine.tools.remoteapi.RemoteApiInstaller;
import com.googlecode.objectify.ObjectifyService;
import com.googlecode.objectify.util.Closeable;
import java.io.IOException;
import present.server.model.PresentEntities;
import present.server.model.user.User;
import present.server.model.user.Users;
import present.server.tool.RemoteTool;

public class MigrateNotificationSettings_06132017 extends RemoteTool {

  /**
   * Ran in staging: 06/14/2017
   * Ran in production: 06/19/2017
   */
  public static void main(String[] args) throws IOException
  {
    //RemoteApiInstaller installer = installRemoteAPI(DEV_SERVER);
    //RemoteApiInstaller installer = installRemoteAPI(STAGING_SERVER);
    RemoteApiInstaller installer = installRemoteAPI(PRODUCTION_SERVER);

    try (Closeable closeable = ObjectifyService.begin()) {
      //NamespaceManager.set("test");

      PresentEntities.registerAll();

      Iterable<User> users = Users.all();
      int count = 0;
      for (User user : users) {
        //user.notificationSettings.userCommentsOnJoinedGroup = user.notifyFavoritedGroups;
        System.out.println("Updated user: "+count++);
        user.save().now();
      }
    }
    installer.uninstall();
  }
}
