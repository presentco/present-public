package present.server.tool;

import present.server.model.user.User;
import present.server.model.user.Users;

public class DeleteBobbie {
  public static void main(String[] args) {
    RemoteTool.against(RemoteTool.STAGING_SERVER, () -> {
      User bobbie = StagingUsers.bobbie();
      if (bobbie != null) {
        System.out.println("Deleting Bobbie...");
        Users.cascadingDelete(bobbie.getKey());
      } else {
        System.out.println("Bobbie not found.");
      }
    });
  }
}
