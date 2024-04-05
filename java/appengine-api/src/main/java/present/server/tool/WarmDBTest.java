package present.server.tool;

import present.server.model.group.Group;
import present.server.model.user.Client;
import present.server.model.comment.Comment;
import present.server.model.PresentEntities;
import present.server.model.group.JoinedGroups;
import present.server.model.user.User;
import com.google.appengine.tools.remoteapi.RemoteApiInstaller;
import com.googlecode.objectify.ObjectifyService;
import com.googlecode.objectify.util.Closeable;
import java.io.IOException;

import static present.server.model.PresentEntities.loadRandomSubset;

public class WarmDBTest extends RemoteTool {

  public static void main(String[] args) throws IOException
  {
    RemoteApiInstaller installer = installRemoteAPI(STAGING_SERVER);

    try (Closeable closeable = ObjectifyService.begin()) {
      PresentEntities.registerAll();

      loadRandomSubset(Client.class, 5);
      loadRandomSubset(User.class, 5);
      loadRandomSubset(Group.class, 5);
      loadRandomSubset(JoinedGroups.class, 5);
      loadRandomSubset(Comment.class, 5);
    }
    installer.uninstall();
  }

}
