package present.server.tool;

import java.io.IOException;
import present.server.model.PresentEntities;
import present.server.model.geocoding.Geocoding;
import present.server.model.user.Users;

public class GeotagUsers extends RemoteTool {

  public static void main(String[] args) throws IOException
  {
    RemoteTool.against(RemoteTool.STAGING_SERVER, () -> {
      PresentEntities.registerAll();

      Users.stream().forEach(user->{
        if (user.signupAddress == null) {
          System.out.println("user = " + user.publicName());
          Geocoding.geocodeSignupLocation(user);
          user.savePreservingUpdateTime().now();
        }
      });

    });
  }
}
