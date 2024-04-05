package present.server.tool;

import java.io.IOException;
import present.server.model.PresentEntities;
import present.server.model.user.Users;

public class UpdateUserIndexes extends RemoteTool {

  public static void main(String[] args) throws IOException
  {
    RemoteTool.against(RemoteTool.STAGING_SERVER, () -> {
      PresentEntities.registerAll();
      Users.stream().forEach(user->{
        System.out.println("user = " + user.publicName());
        user.signupTime = user.createdTime;
        user.savePreservingUpdateTime().now();
      });

    });
  }
}
