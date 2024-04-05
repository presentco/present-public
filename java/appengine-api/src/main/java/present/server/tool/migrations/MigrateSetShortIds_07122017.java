package present.server.tool.migrations;

import com.google.appengine.tools.remoteapi.RemoteApiInstaller;
import com.google.common.collect.Iterables;
import com.googlecode.objectify.ObjectifyService;
import com.googlecode.objectify.util.Closeable;
import java.io.IOException;
import present.server.model.BasePresentEntity;
import present.server.model.PresentEntities;
import present.server.model.group.Group;
import present.server.model.group.Groups;
import present.server.model.user.User;
import present.server.model.user.Users;
import present.server.tool.RemoteTool;

public class MigrateSetShortIds_07122017 extends RemoteTool {

  /**
   * Ran in staging: 07/12/17
   * Ran in production: 07/13/17
   */
  public static void main(String[] args) throws IOException
  {
    //RemoteApiInstaller installer = installRemoteAPI(DEV_SERVER);
    //RemoteApiInstaller installer = installRemoteAPI(STAGING_SERVER);
    RemoteApiInstaller installer = installRemoteAPI(PRODUCTION_SERVER);

    try (Closeable closeable = ObjectifyService.begin()) {
      PresentEntities.registerAll();
      for (BasePresentEntity<?> entity : Iterables.concat(Groups.all(), Users.all())) {
        entity.setShortId();
        entity.save();
      }
    }
    installer.uninstall();
  }
}
