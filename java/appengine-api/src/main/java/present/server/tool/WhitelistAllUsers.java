package present.server.tool;

import com.google.appengine.tools.remoteapi.RemoteApiInstaller;
import com.googlecode.objectify.ObjectifyService;
import com.googlecode.objectify.util.Closeable;
import java.io.IOException;
import java.util.List;
import present.server.model.PresentEntities;
import present.server.model.user.User;

import static com.googlecode.objectify.ObjectifyService.ofy;

public class WhitelistAllUsers extends RemoteTool {

  public static void main(String[] args) throws IOException {
    RemoteApiInstaller installer = installRemoteAPI(PRODUCTION_SERVER);
    try (Closeable closeable = ObjectifyService.begin()) {
      PresentEntities.registerAll();
      List<User> users = ofy().load().type(User.class).list();
      // TODO: We may update this util when we have email for all users.
      /*
      List<WhitelistedPhoneNumber> phoneNumbers = Lists.transform(users,
          new Function<User, WhitelistedPhoneNumber>() {
        @Override public WhitelistedPhoneNumber apply(User input) {
          return new WhitelistedPhoneNumber(input.phoneNumber);
        }
      });

      ofy().save().entities(phoneNumbers);
      */
    }
    installer.uninstall();
  }
}
