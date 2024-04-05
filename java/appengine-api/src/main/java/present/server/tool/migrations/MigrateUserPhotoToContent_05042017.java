package present.server.tool.migrations;

import com.google.appengine.tools.remoteapi.RemoteApiInstaller;
import com.googlecode.objectify.ObjectifyService;
import com.googlecode.objectify.util.Closeable;
import java.io.IOException;
import present.proto.ContentType;
import present.server.GoogleCloudStorage;
import present.server.model.PresentEntities;
import present.server.model.content.Content;
import present.server.model.user.User;
import present.server.model.user.Users;
import present.server.tool.RemoteTool;

public class MigrateUserPhotoToContent_05042017 extends RemoteTool {

  /**
   * Ran in staging: 05/05/2017
   * Ran in production : 05/06/2017
   */
  public static void main(String[] args) throws IOException
  {
    //RemoteApiInstaller installer = installRemoteAPI(DEV_SERVER);
    //RemoteApiInstaller installer = installRemoteAPI(STAGING_SERVER);
    RemoteApiInstaller installer = installRemoteAPI(PRODUCTION_SERVER);

    try (Closeable closeable = ObjectifyService.begin()) {
      PresentEntities.registerAll();

      Iterable<User> all = Users.all();
      for (User user : all)
      {
        // Create a real reference for the implicit user path
        String USER_PROFILE_PHOTO_PATH = "profile-photos"; // TODO: Removed from User after migration
        Content content = new Content(user.uuid, ContentType.JPEG);
        content.save().now();
        user.photo = content.getRef();
        user.save().now();
        String newUrl = content.url();
        System.out.println("newUrl = " + newUrl);

        // compare with old url
        String fileName = user.uuid + ".jpeg";
        String oldUrl = GoogleCloudStorage.urlFor(GoogleCloudStorage.fileForPath("profile-photos/" + fileName));
        System.out.println("oldUrl = " + oldUrl);

      }
    }
    installer.uninstall();
  }
}
