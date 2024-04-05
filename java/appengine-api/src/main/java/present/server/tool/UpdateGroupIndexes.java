package present.server.tool;

import com.google.appengine.tools.remoteapi.RemoteApiInstaller;
import com.google.common.collect.Iterables;
import com.googlecode.objectify.ObjectifyService;
import com.googlecode.objectify.util.Closeable;
import java.io.IOException;
import present.server.model.PresentEntities;
import present.server.model.group.Group;
import present.server.model.group.Groups;

import static com.googlecode.objectify.ObjectifyService.ofy;
import static present.server.model.PresentEntities.active;

public class UpdateGroupIndexes extends RemoteTool {

  public static void main(String[] args) throws IOException
  {
    //RemoteApiInstaller installer = installRemoteAPI(DEV_SERVER);
    RemoteApiInstaller installer = installRemoteAPI(STAGING_SERVER);

    try (Closeable closeable = ObjectifyService.begin()) {
      PresentEntities.registerAll();

      Iterable<Group> groups = Groups.all();
      System.out.println("Updating "+Iterables.size(groups)+" groups");
      for (Group group : groups) {
        ofy().save().entity(group).now(); // save individually
      }
    }
    installer.uninstall();
  }
}
