package present.server.tool;

import present.server.model.group.Group;
import present.server.model.PresentEntities;
import com.google.appengine.tools.remoteapi.RemoteApiInstaller;
import com.google.common.collect.Iterables;
import com.googlecode.objectify.ObjectifyService;
import com.googlecode.objectify.util.Closeable;
import java.io.IOException;

import static present.server.model.PresentEntities.active;
import static com.googlecode.objectify.ObjectifyService.ofy;

public class SetGroupEntityDeleteFlag extends RemoteTool {

  public static void main(String[] args) throws IOException
  {
    //RemoteApiInstaller installer = installRemoteAPI(DEV_SERVER);
    RemoteApiInstaller installer = installRemoteAPI(STAGING_SERVER);

    try (Closeable closeable = ObjectifyService.begin()) {
      PresentEntities.registerAll();

      // Set all bubble delete flags to false!

      int beforeActiveCount = active(ofy().load().type(Group.class)).count();
      System.out.println("beforeActiveCount = " + beforeActiveCount);

      final Iterable<Group> groups = ofy().load().type(Group.class).iterable();
      System.out.println("Updating "+Iterables.size(groups)+" groups");
      for (Group group : groups) {
        group.deleted = false;
        group.save();
      }
      ofy().flush();

      int afterActiveCount = active(ofy().load().type(Group.class)).count();
      System.out.println("afterActiveCount = " + afterActiveCount);
    }
    installer.uninstall();
  }
}
