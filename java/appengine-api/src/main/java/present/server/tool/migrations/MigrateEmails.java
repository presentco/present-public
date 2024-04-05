package present.server.tool.migrations;

import java.util.ArrayList;
import java.util.List;
import present.server.model.user.User;
import present.server.model.user.Users;
import present.server.tool.RemoteTool;

import static com.googlecode.objectify.ObjectifyService.ofy;

/** Copies emails from Facebook data into User.email. */
public class MigrateEmails {
  public static void main(String[] args) {
    RemoteTool.against(RemoteTool.PRODUCTION_SERVER, () -> {
      List<User> save = new ArrayList<>();
      Users.stream().forEach(user -> {
        if (user.email == null) {
          String email = user.email();
          if (email != null) {
            user.email = email.toLowerCase();
            save.add(user);
          }
        }
      });
      ofy().save().entities(save);
    });
  }
}
