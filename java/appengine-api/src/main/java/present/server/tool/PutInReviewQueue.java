package present.server.tool;

import com.googlecode.objectify.Ref;
import java.util.HashSet;
import java.util.Set;
import present.server.model.user.Clients;
import present.server.model.user.User;
import present.server.model.user.UserState;
import present.server.model.user.Users;

public class PutInReviewQueue {
  public static void main(String[] args) {
    RemoteTool.against(RemoteTool.PRODUCTION_SERVER, () -> {
      Set<Ref<User>> usersWithTokens = new HashSet<>();
      Clients.all().forEach(c -> {
          if (c.deviceToken != null) usersWithTokens.add(c.user);
      });
      for (User user : Users.all()) {
        if (usersWithTokens.contains(user.getRef()) && user.state == UserState.SIGNING_UP) {
          System.out.println(user.fullName());
        }
      }
    });
  }
}
