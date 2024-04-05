package present.server.model.user;

import com.google.appengine.tools.development.testing.LocalDatastoreServiceTestConfig;
import com.google.appengine.tools.development.testing.LocalServiceTestHelper;
import com.googlecode.objectify.ObjectifyService;
import com.googlecode.objectify.util.Closeable;
import java.io.IOException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import present.proto.UserRequest;
import present.proto.UserService;
import present.server.AppEngineUserService;
import present.server.Uuids;
import present.server.model.PresentEntities;

import static com.googlecode.objectify.ObjectifyService.ofy;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class UsersTest {

  private final LocalServiceTestHelper helper =
      new LocalServiceTestHelper(new LocalDatastoreServiceTestConfig());

  @Before public void setUp() {
    helper.setUp();
  }

  @After public void tearDown() {
    helper.tearDown();
  }

  @Test public void testGetOrCreateByPhone() throws InterruptedException {
    try (Closeable ignored = ObjectifyService.begin()) {
      PresentEntities.registerAll();

      String phone = "1314";
      User a = Users.getOrCreateByPhone(phone);
      PhoneToUser ptu = ofy().load().key(PhoneToUser.key(phone)).now();
      assertNotNull(ptu);
      assertEquals(a, Users.getOrCreateByPhone(phone));
    }
  }

  @Test public void testAddFriendByPhone() throws InterruptedException, IOException {
    try (Closeable ignored = ObjectifyService.begin()) {
      PresentEntities.registerAll();

      User bob = new User();
      bob.firstName = "Bob";
      bob.uuid = Uuids.repeat('1');
      bob.state = UserState.MEMBER;
      String phone = "1314";
      bob.phoneNumber = phone;
      bob.save();

      Users.setCurrent(bob);

      UserService us = new AppEngineUserService();
      us.addFriend(new UserRequest(null, "14155551212"));
    }
  }
}
