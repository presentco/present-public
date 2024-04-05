package present.server.tool;

import java.io.IOException;
import present.server.facebook.Facebook;
import present.server.model.user.User;
import present.server.model.user.Users;

import static com.googlecode.objectify.ObjectifyService.ofy;

public class UpdateFacebookData extends RemoteTool {

  public static void main(String[] args) throws IOException {
    against(PRODUCTION_SERVER, () -> {
      for (User user : Users.all()) {
        if (user.facebookAccessToken != null) {
          try {
            user.updateFacebook();
          } catch (Exception e) {
            e.printStackTrace();
          }
        }
      }
    });
  }
}
