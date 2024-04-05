package present.server.facebook;

import com.google.appengine.tools.development.testing.LocalDatastoreServiceTestConfig;
import com.google.appengine.tools.development.testing.LocalServiceTestHelper;
import com.google.common.base.Charsets;
import com.google.common.collect.ImmutableSet;
import com.google.common.io.Resources;
import com.google.gson.Gson;
import com.googlecode.objectify.ObjectifyService;
import com.googlecode.objectify.util.Closeable;
import java.io.IOException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import present.server.Uuids;
import present.server.model.PresentEntities;
import present.server.model.user.Friendship;
import present.server.model.user.User;

import static com.google.common.collect.Sets.newHashSet;
import static org.junit.Assert.assertEquals;
import static present.server.facebook.FacebookFriendship.friendsOf;

public class FacebookFriendshipTest {

  private final LocalServiceTestHelper helper =
      new LocalServiceTestHelper(new LocalDatastoreServiceTestConfig());

  @Before public void setUp() {
    helper.setUp();
  }

  @After public void tearDown() {
    helper.tearDown();
  }

  @Test public void testFacebookFriends() throws IOException, InterruptedException {
    try (Closeable ignored = ObjectifyService.begin()) {
      PresentEntities.registerAll();

      // Order matters here, as the Facebook friends only contain friends who are already in
      // the system.
      User janete = user("janete.json");
      User bob = user("bob.json");
      User alice = user("alice.json");

      assertEquals(ImmutableSet.of(bob, alice), newHashSet(friendsOf(janete)));
      assertEquals(ImmutableSet.of(janete, alice), newHashSet(friendsOf(bob)));
      assertEquals(ImmutableSet.of(bob, janete), newHashSet(friendsOf(alice)));

      assertEquals(ImmutableSet.of(bob, alice), newHashSet(Friendship.friendsOf(janete)));
      assertEquals(ImmutableSet.of(janete, alice), newHashSet(Friendship.friendsOf(bob)));
      assertEquals(ImmutableSet.of(bob, janete), newHashSet(Friendship.friendsOf(alice)));
    }
  }

  private static Gson gson = new Gson();

  private User user(String facebookJson) {
    try {
      String json = Resources.toString(getClass().getResource(facebookJson), Charsets.UTF_8);
      FacebookUserData fud = gson.fromJson(json, FacebookUserData.class);
      User u = new User();
      u.uuid = Uuids.newUuid();
      u.firstName = fud.first_name;
      u.lastName = fud.last_name;
      u.facebookId = fud.id;
      u.save().now();
      FacebookFriendship.saveFriends(u, fud);
      return u;
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
}
