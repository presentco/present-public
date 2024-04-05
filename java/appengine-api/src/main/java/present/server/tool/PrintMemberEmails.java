package present.server.tool;

import present.server.model.user.UserState;
import present.server.model.user.Users;

public class PrintMemberEmails {
  public static void main(String[] args) {
    RemoteTool.against(RemoteTool.PRODUCTION_SERVER, () -> {
      Users.all().stream()
          .filter(u -> u.state == UserState.MEMBER)
          .filter(u -> u.email != null)
          .filter(u -> !u.isTestUser())
          .forEach(user -> {
            System.out.println(user.fullName() + "\t" + user.email);
          });
    });
  }
}
