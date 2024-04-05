package present.server.tool;

import present.server.model.user.User;

import static present.server.tool.RemoteTool.PRODUCTION_SERVER;
import static present.server.tool.RemoteTool.against;

/**
 * @author Bob Lee (bob@present.co)
 */
public class PrintUser {
  public static void main(String[] args) {
    against(PRODUCTION_SERVER, () -> {
      User u = User.get("d38050f9-e9bd-42d9-a682-5e1533fbd2c0");
      System.out.println(u.fullName());
      System.out.println(u.signupLocation);
      System.out.println(u.signupAddress);
      System.out.println(u.phoneNumber);
      System.out.println(u.facebookLink());
    });
  }
}
