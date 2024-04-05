package present.server.tool;

import present.server.facebook.FacebookUserData;
import present.server.model.user.User;
import present.server.model.user.UserState;
import present.server.model.user.Users;

/**
 * @author Bob Lee (bob@present.co)
 */
public class PrintFacebookFriendCounts {

  public static void main(String[] args) {
    RemoteTool.against(RemoteTool.PRODUCTION_SERVER, () -> {
      for (User user : Users.all()) {
        System.out.printf("%s %s\t", user.firstName, user.lastName);
        FacebookUserData facebook = user.facebookData();
        if (facebook == null) {
          System.out.println("null facebook");
          continue;
        }
        FacebookUserData.Friends friends = facebook.friends;
        if (friends == null) {
          System.out.println("null friends");
          continue;
        }
        FacebookUserData.Friends.Summary summary = friends.summary;
        if (summary == null) {
          System.out.println("null summary");
          continue;
        }
        System.out.printf("%s\n", user.facebookData().friends.summary.total_count);
      }
    });
  }
}
