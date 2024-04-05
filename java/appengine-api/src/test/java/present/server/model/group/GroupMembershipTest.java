package present.server.model.group;

import com.google.appengine.repackaged.com.google.common.collect.ImmutableList;
import com.google.appengine.tools.development.testing.LocalDatastoreServiceTestConfig;
import com.google.appengine.tools.development.testing.LocalServiceTestHelper;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.googlecode.objectify.ObjectifyService;
import com.googlecode.objectify.util.Closeable;
import java.util.Collections;
import java.util.List;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import present.proto.GroupMemberPreapproval;
import present.proto.GroupMembershipState;
import present.proto.Platform;
import present.proto.RequestHeader;
import present.server.RequestHeaders;
import present.server.Uuids;
import present.server.model.PresentEntities;
import present.server.model.Space;
import present.server.model.activity.Event;
import present.server.model.user.Client;
import present.server.model.user.Friendship;
import present.server.model.user.User;
import present.server.model.user.UserState;
import present.server.model.user.Users;
import present.server.notification.TestNotification;

import static com.googlecode.objectify.ObjectifyService.ofy;
import static com.googlecode.objectify.ObjectifyService.register;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class GroupMembershipTest {

  private final LocalServiceTestHelper helper =
      new LocalServiceTestHelper(new LocalDatastoreServiceTestConfig());

  @Before public void setUp() {
    helper.setUp();
  }

  @After public void tearDown() {
    helper.tearDown();
  }

  @Test public void testMembership() throws InterruptedException {
    try (Closeable ignored = ObjectifyService.begin()) {
      PresentEntities.registerAll();

      User a = newUser('A');
      User b = newUser('B');
      User c = newUser('C');
      User d = newUser('D');

      // A owns private group g.
      Group g = new Group();
      g.id = Uuids.newUuid();
      g.title = "Test Group";
      g.preapprove = GroupMemberPreapproval.INVITE_ONLY;
      g.owner = a.getRef();
      g.memberCount = 1;
      g.spaceId = Space.EVERYONE.id;

      GroupMembership gm = GroupMembership.getOrCreate(a, g);
      gm.state = GroupMembershipState.ACTIVE;
      ofy().save().entities(g, gm).now();

      // B requests to join
      assertEquals(0, g.joinRequests);
      g.join(b);
      assertEquals(1, g.joinRequests);
      TestNotification.to(a.uuid).stream()
          .filter(n -> n.body.matches("^User B requested.*"))
          .findFirst()
          .get();

      // A approves B.
      g.addMembers(a, Collections.singleton(b));
      assertEquals(0, g.joinRequests);
      TestNotification.to(b.uuid).stream()
          .filter(n -> n.body.matches("^User A added you.*"))
          .findFirst()
          .get();
      assertTrue(g.isMember(b));

      // A adds C.
      // Clear member cache.
      ofy().clear();
      g = g.reload();
      g.addMembers(a, Collections.singleton(c));
      TestNotification.to(c.uuid).stream()
          .filter(n -> n.body.matches("^User A added you.*"))
          .findFirst()
          .get();
      TestNotification.to(b.uuid).stream()
          .filter(n -> n.body.matches("^User A added User C.*"))
          .findFirst()
          .get();

      assertTrue(g.isMember(a));
      assertTrue(g.isMember(b));
      assertTrue(g.isMember(c));

      g.leave(b);
      assertFalse(g.isMember(b));

      g.removeMembers(a, Collections.singleton(c));
      assertFalse(g.isMember(c));

      // D requests to join
      g.join(d);
      // A removes the request
      assertEquals(1, g.joinRequests);
      g.removeMembers(a, Collections.singleton(d));
      assertEquals(0, g.joinRequests);
    }
  }

  private User newUser(char c) {
    User user = new User();
    user.firstName = "User " + c;
    user.uuid = Uuids.repeat(c);
    user.state = UserState.MEMBER;
    user.save();
    Client client = new Client();
    client.uuid = user.uuid;
    client.user = user.getRef();
    client.platform = Platform.TEST.getValue();
    client.deviceToken = user.uuid;
    client.save();
    return user;
  }
}
