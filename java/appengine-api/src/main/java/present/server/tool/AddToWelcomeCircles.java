package present.server.tool;


import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import present.server.AppEngineUserService;
import present.server.model.user.User;

import static com.googlecode.objectify.ObjectifyService.ofy;

public class AddToWelcomeCircles {
  public static void main(String[] args) throws IOException {
    RemoteTool.against(RemoteTool.PRODUCTION_SERVER, () -> {
      long millis = LocalDateTime.parse("2017-12-13T00:00:00")
          .atZone(ZoneId.systemDefault())
          .toInstant()
          .toEpochMilli();
      List<User> list = ofy().load().type(User.class).filter("signupTime >=", millis).list();
      System.out.println("Number of users: " + String.valueOf(list.size()));
      for (User u : list) {
        AppEngineUserService.addNewUserToHomeCircle(u);
        System.out.println("User: " + u.name().toString() + "City: " + u.homeCity().toString());
      }
    });
  }
}
