package present.server;

import java.io.IOException;
import org.junit.Test;
import present.proto.UserName;
import present.proto.UserProfileRequest;
import present.server.model.user.User;
import present.server.model.user.UserState;
import present.server.model.util.Coordinates;
import static org.junit.Assert.*;

/**
 * Tests signup location source in User class.
 *
 * @author Gabrielle Taylor (gabrielle@present.co)
 **/
public class SignupLocationTest{

    @Test public void testLocationSource() throws IOException {

      // Frida Kahlo signs up via web and has the zip code 94103
      User frida = new User() {
        {
          this.state = UserState.MEMBER;
          this.uuid = Uuids.newUuid();
        }
      };

      // When we update Frida's profile with her zipcode, her sign up location is set.
      frida.updateFrom(new UserProfileRequest.Builder()
          .name(new UserName.Builder().first("Frida").last("Kahlo").build())
          .zip("94")
          .build());
      // Her sign up location and source no longer null
      assertNotNull(frida.signupLocation);
      assertNotNull(frida.signupLocationSource);
      // The sign up location source is correctly set as ZIP
      assertEquals(frida.signupLocationSource, User.SignupLocationSource.ZIP);
      // Her zipcode is correctly set.
      assertEquals(frida.zip, "94103");


      // Georgia O'Keefe signs up and has a valid sign up location but the SignupLocationFlag is not set
      User georgia = new User() {
        {
          this.state = UserState.MEMBER;
          this.uuid = Uuids.newUuid();
          this.firstName = "Georgia";
          this.lastName = "O'Keefe";
        }
      };
      georgia.signupLocation = Coordinates.fromProto(Cities.NEW_YORK.location);
      georgia.signupLocationSource = null;
      georgia.zip = null;

      // When we determine the sign up location source, we get GPS as the source since there is no valid zip code
      // (prevents users who did not have their location source saved from causing issues).
      assertEquals(georgia.signupLocationSource(), User.SignupLocationSource.GPS);

    }
}
