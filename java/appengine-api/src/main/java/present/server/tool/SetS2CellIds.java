package present.server.tool;

import present.server.model.group.Group;
import com.google.appengine.api.datastore.QueryResultIterable;
import com.google.appengine.tools.remoteapi.RemoteApiInstaller;
import com.google.common.geometry.S2CellId;
import com.google.common.geometry.S2LatLng;
import com.googlecode.objectify.ObjectifyService;
import com.googlecode.objectify.util.Closeable;
import java.io.IOException;

import static com.googlecode.objectify.ObjectifyService.ofy;

public class SetS2CellIds {

  public static void main(String[] args) throws IOException {
    RemoteApiInstaller installer = RemoteTool.installRemoteAPI(RemoteTool.STAGING_SERVER);

    try (Closeable closeable = ObjectifyService.begin()) {
      ObjectifyService.register(Group.class);
      QueryResultIterable<Group> groups = ofy().load().type(Group.class).iterable();
      for (Group group : groups) {
        if (group.s2CellId == 0) {
          System.out.println("Updating cell ID on " + group.uuid() + ".");
          S2CellId cellId = S2CellId.fromLatLng(
              S2LatLng.fromDegrees(group.location().latitude, group.location().longitude));
          group.s2CellId = cellId.id();
          ofy().save().entity(group);
        }
      }
    }
    installer.uninstall();
  }
}
