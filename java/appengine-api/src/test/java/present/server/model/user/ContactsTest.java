package present.server.model.user;

import com.google.appengine.tools.development.testing.LocalDatastoreServiceTestConfig;
import com.google.appengine.tools.development.testing.LocalServiceTestHelper;
import com.googlecode.objectify.ObjectifyService;
import com.googlecode.objectify.util.Closeable;
import java.io.IOException;
import java.util.Collections;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import present.proto.AddContactsRequest;
import present.proto.AddContactsResponse;
import present.proto.ContactRequest;
import present.proto.PhoneUserResponse;
import present.proto.UserService;
import present.server.AppEngineUserService;
import present.server.Uuids;
import present.server.model.PresentEntities;

import static org.junit.Assert.assertEquals;

public class ContactsTest {

  private final LocalServiceTestHelper helper =
      new LocalServiceTestHelper(new LocalDatastoreServiceTestConfig());

  @Before public void setUp() {
    helper.setUp();
  }

  @After public void tearDown() {
    helper.tearDown();
  }

  @Test public void testAddContact() throws IOException {
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
      AddContactsResponse response = us.addContacts(
          new AddContactsRequest(Collections.singletonList(
              new ContactRequest(phone, "Bob Lee", "Bob", "Lee"))));
      PhoneUserResponse result = response.results.get(0);
      assertEquals(bob.uuid, result.user.id);
      assertEquals(phone, result.phoneNumber);
    }
  }
}
