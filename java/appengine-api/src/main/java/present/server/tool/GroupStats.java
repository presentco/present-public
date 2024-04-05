package present.server.tool;

import com.google.appengine.tools.remoteapi.RemoteApiInstaller;
import com.googlecode.objectify.ObjectifyService;
import com.googlecode.objectify.util.Closeable;
import java.io.IOException;
import present.server.Time;
import present.server.model.PresentEntities;
import present.server.model.group.Groups;

@SuppressWarnings("ALL") public class GroupStats extends RemoteTool {

  public static void main(String[] args) throws IOException
  {
    //RemoteApiInstaller installer = installRemoteAPI(DEV_SERVER);
    //RemoteApiInstaller installer = installRemoteAPI(STAGING_SERVER);
    RemoteApiInstaller installer = installRemoteAPI(PRODUCTION_SERVER);
    try (Closeable closeable = ObjectifyService.begin())
    {
      //NamespaceManager.set("test");
      PresentEntities.registerAll();


      System.out.printf("\n%s, %s, %s, %s, %s, %s, %s, %s",
        "Title", "Created", "Updated", "Owner", "Members", "Comments", "Last Comment", "Link"
      );

      Groups.stream().forEach(group-> {
        System.out.printf("\n%s, %s, %s, %s, %s, %s, %s, %s",
            group.title,
            Time.format_yyyy_MM_dd(group.createdTime),
            Time.format_yyyy_MM_dd(group.updatedTime),
            group.owner.get().publicName(),
            group.memberCount,
            group.activeComments,
            Time.format_yyyy_MM_dd(group.lastCommentTime),
            group.shortLink()
        );
      });

    } finally {
      installer.uninstall();
    }
  }
}
