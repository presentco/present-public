package present.server.model.geocoding;

import java.io.IOException;
import present.proto.UserRequest;
import present.proto.UserService;
import present.server.RpcQueue;
import present.server.model.user.User;

/**
 * @author Pat Niemeyer (pat@present.co)
 * Date: 11/9/17
 */
public class GeocodeClient {

  private static UserService INSTANCE = RpcQueue.create(UserService.class);

  public static void queueGeotagRequest(User user) {
    try {
      INSTANCE.geocodeSignupLocation(new UserRequest(user.uuid, null));
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

}
