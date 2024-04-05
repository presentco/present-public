package present.server.tool;

import java.util.ArrayList;
import java.util.List;
import present.server.AppEngineUserService;
import present.server.model.user.User;
import present.server.model.user.UserState;
import present.server.model.user.Users;

import static com.googlecode.objectify.ObjectifyService.ofy;

public class ReviewOutcomes {
  public static void main(String[] args) {
    RemoteTool.against(RemoteTool.PRODUCTION_SERVER, () -> {
      List<User> users = new ArrayList<>();
      Users.all().forEach(user -> {
        if (user.review == null && user.state != UserState.SIGNING_UP) {
          AppEngineUserService.stateAfterSignup(user, user.signupLocation);
          user.preserveUpdateTime = true;
          System.out.println(user.fullName() + ": " + user.review);
          users.add(user);
        }
      });
      ofy().save().entities(users);
    });
  }
}
