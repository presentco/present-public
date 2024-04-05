package present.server.tool;

import com.googlecode.objectify.Ref;
import present.server.AppEngineUserService;
import present.server.facebook.FacebookUserData;
import present.server.model.content.Content;
import present.server.model.user.User;
import present.server.model.user.Users;

public class CopyFacebookPhotos {
  public static void main(String[] args) {
    RemoteTool.against(RemoteTool.STAGING_SERVER, () -> {
      for (User user : Users.all()) {
        if (user.photo == null) {
          FacebookUserData facebookData = user.facebookData();
          if (facebookData != null) {
            System.out.println(user.fullName() + ":");
            Content content = AppEngineUserService.facebookPhotoToContent(facebookData, false);
            if (content != null) {
              user.photo = Ref.create(content);
              user.save();
            }
          }
        }
      }
    });
  }
}
