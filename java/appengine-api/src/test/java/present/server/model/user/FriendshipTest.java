package present.server.model.user;

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
import present.proto.Platform;
import present.proto.UnreadState;
import present.server.Uuids;
import present.server.model.PresentEntities;
import present.server.notification.TestNotification;

import static com.googlecode.objectify.ObjectifyService.ofy;
import static org.junit.Assert.assertEquals;

public class FriendshipTest {

  private final LocalServiceTestHelper helper =
      new LocalServiceTestHelper(new LocalDatastoreServiceTestConfig());

  @Before public void setUp() {
    helper.setUp();
  }

  @After public void tearDown() {
    helper.tearDown();
  }

  @Test public void testFriendship() throws InterruptedException {
    try (Closeable ignored = ObjectifyService.begin()) {
      PresentEntities.registerAll();

      User a = newUser('A');
      User b = newUser('B');
      User c = newUser('C');

      // a requests b & c
      Friendship.addFriend(a, b);
      Friendship.addFriend(a, c);

      assertEquals(ImmutableList.of(a), Lists.newArrayList(Friendship.requestsTo(b)));
      assertEquals(ImmutableList.of(a), Lists.newArrayList(Friendship.requestsTo(c)));
      assertEquals(Collections.emptyList(), Lists.newArrayList(Friendship.requestsTo(a)));

      TestNotification notification = TestNotification.to(b.uuid).stream()
          .filter(n -> n.body.matches("^User A sent.*"))
          .findFirst()
          .get();
      assertEquals(1, notification.badge);

      notification = TestNotification.to(c.uuid).stream()
          .filter(n -> n.body.matches("^User A sent.*"))
          .findFirst()
          .get();
      assertEquals(1, notification.badge);

      assertEquals(b.incomingFriendRequests, 1);
      assertEquals(c.incomingFriendRequests, 1);

      // b accepts a's and requests c's
      Friendship.addFriend(b, c);
      Friendship.addFriend(b, a);

      TestNotification.to(a.uuid).stream()
          .filter(n -> n.body.matches("^User B accepted.*"))
          .findFirst()
          .get();
      notification = TestNotification.to(c.uuid).stream()
          .filter(n -> n.body.matches("^User B sent.*"))
          .findFirst()
          .get();
      assertEquals(2, notification.badge);

      assertEquals(0, b.incomingFriendRequests);
      assertEquals(2, c.incomingFriendRequests);

      assertEquals(ImmutableSet.of(a, b), Sets.newHashSet(Friendship.requestsTo(c)));
      assertEquals(ImmutableSet.of(c), Sets.newHashSet(Friendship.requestsFrom(a)));

      // a and b are now friends.
      assertEquals(Collections.emptyList(), Lists.newArrayList(Friendship.requestsTo(b)));
      assertEquals(ImmutableList.of(a), Lists.newArrayList(Friendship.friendsOf(b)));
      assertEquals(ImmutableList.of(b), Lists.newArrayList(Friendship.friendsOf(a)));
      assertEquals(Collections.emptyList(), Lists.newArrayList(Friendship.requestsTo(a)));

      // c accepts b's request
      Friendship.addFriend(c, b);
      assertEquals(ImmutableSet.of(a, c), Sets.newHashSet(Friendship.friendsOf(b)));
      assertEquals(ImmutableSet.of(b), Sets.newHashSet(Friendship.friendsOf(c)));
      assertEquals(1, c.incomingFriendRequests);

      // c removes b
      Friendship.removeFriend(c, b);
      assertEquals(ImmutableSet.of(a), Sets.newHashSet(Friendship.friendsOf(b)));
      assertEquals(Collections.emptyList(), Lists.newArrayList(Friendship.friendsOf(c)));
      assertEquals(c.incomingFriendRequests, 1);

      // c removes a's request
      System.out.println(Friendship.requestsTo(c));
      System.out.println(c.incomingFriendRequests);
      Friendship.removeFriend(c, a);
      assertEquals(Collections.emptyList(), Lists.newArrayList(Friendship.requestsTo(c)));
      assertEquals(0, c.reload().incomingFriendRequests);
    }
  }

  private static void dump() {
    List<Friendship> friendships = ofy().load().type(Friendship.class).list();
    for (Friendship friendship : friendships) {
      System.out.println(friendship.id);
      System.out.println(friendship.userIds);
      System.out.println(friendship.state);
      System.out.println(friendship.requestor);
      System.out.println(friendship.requestee);
      System.out.println();
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

    // Prime unread state so we don't try to compute it asynchronously.
    UnreadState state = new UnreadState(Collections.emptyList());
    UnreadStates.put(user, state);

    return user;
  }
}
