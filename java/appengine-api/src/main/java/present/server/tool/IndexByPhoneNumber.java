package present.server.tool;

import java.util.ArrayList;
import java.util.List;
import present.server.model.user.PhoneToUser;
import present.server.model.user.User;
import present.server.model.user.Users;

import static com.googlecode.objectify.ObjectifyService.ofy;
import static present.server.tool.RemoteTool.STAGING_SERVER;
import static present.server.tool.RemoteTool.against;

/**
 * Index users by phone number.
 *
 * @author Bob Lee (bob@present.co)
 */
public class IndexByPhoneNumber {

  public static void main(String[] args) {
    against(STAGING_SERVER, () -> {
      List<PhoneToUser> index = new ArrayList<>();
      for (User user : Users.all()) {
        if (user.phoneNumber != null) {
          // Strip +
          if (user.phoneNumber.startsWith("+")) {
            System.out.print(".");
            user.inTransaction(u -> {
              u.phoneNumber = user.phoneNumber.substring(1);
            });
          }

          PhoneToUser ptu = new PhoneToUser();
          ptu.user = user.getRef();
          ptu.phoneNumber = user.phoneNumber;
        }
      }
      ofy().save().entities(index);
    });
  }
}
