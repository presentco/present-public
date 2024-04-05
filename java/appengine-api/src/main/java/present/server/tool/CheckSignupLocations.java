package present.server.tool;

import java.util.List;
import present.server.model.user.User;
import present.server.model.user.Users;

public class CheckSignupLocations {
  public static void main(String[] args) {
    RemoteTool.against(RemoteTool.PRODUCTION_SERVER, () -> {
      List<User> members = Users.members();
      int count = 0;
      for (User user : members) {
        if (user.signupLocation == null && user.zip == null) {
          count++;
          System.out.println(user);
        }
      }
      System.out.printf("%s out of %s members are missing location information.", count,
          members.size());
    });
  }
}
