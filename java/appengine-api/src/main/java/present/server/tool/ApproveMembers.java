package present.server.tool;

import java.util.Arrays;
import java.util.List;
import present.server.AppEngineUserService;
import present.server.model.console.whitelist.Whitelist;
import present.server.model.geocoding.Geocoding;
import present.server.model.user.User;
import present.server.model.user.UserState;
import present.server.model.user.Users;
import present.server.notification.Notifications;

import static present.server.tool.RemoteTool.PRODUCTION_SERVER;
import static present.server.tool.RemoteTool.against;

/**
 * Automatically approves membership for people in review.
 *
 * @author Bob Lee (bob@present.co)
 */
public class ApproveMembers {

  public static void main(String[] args) {
    against(PRODUCTION_SERVER, () -> {
      List<User> users = Users.query().filter("state in",
          Arrays.asList(UserState.WAITING, UserState.REVIEWING)).list();
      int count = 0;
      for (User user : users) {
        if (user.review == User.Review.OUTSIDE_GEOFENCE && user.signupLocation != null
            && Whitelist.inUnitedStates(Geocoding.reverseGeocodeNow(user.signupLocation))) {
          if (user.transitionTo(UserState.MEMBER)) {
            Users.runAsGenericAdminUserRequest(() -> {
              AppEngineUserService.welcomeNewUser(user);
            });
            System.out.println("Approved " + ++count + " members.");
          }
        }
      }
    });
  }
}
