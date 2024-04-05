package present.server.model.user;

import com.google.appengine.tools.development.testing.LocalMemcacheServiceTestConfig;
import com.google.appengine.tools.development.testing.LocalServiceTestHelper;
import com.google.common.collect.ImmutableList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import present.proto.UnreadCounts;
import present.proto.UnreadState;
import present.server.Uuids;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class UnreadStatesTest {

  private final LocalServiceTestHelper helper =
      new LocalServiceTestHelper(new LocalMemcacheServiceTestConfig());

  @Before public void setUp() {
    helper.setUp();
  }

  @After public void tearDown() {
    helper.tearDown();
  }

  private static final UnreadState EMPTY = new UnreadState(Collections.emptyList());

  final User bob = newUser();
  final User pat = newUser();
  final User janete = newUser();
  final List<User> everyone = ImmutableList.of(bob, pat, janete);

  @Test public void nonCached() {
    UnreadStates states = new FakeUnreadStates(everyone);
    for (User user : everyone) {
      assertTrue(states.nonCachedUsers().contains(user));
    }
    for (User user : everyone) {
      UnreadStates.put(user, EMPTY);
    }
    states = new FakeUnreadStates(everyone);
    assertTrue(states.nonCachedUsers().isEmpty());
  }

  @Test public void markUnreadAndThenRead() {
    for (User user : everyone) {
      UnreadStates.put(user, EMPTY);
    }

    String group1 = Uuids.newUuid();
    String group2 = Uuids.newUuid();
    String group3 = Uuids.newUuid();
    ImmutableList<String> groups = ImmutableList.of(group1, group2, group3);

    // Mark three groups unread.
    for (int i = 0; i < groups.size(); i++) {
      String group = groups.get(i);
      UnreadStates states = new FakeUnreadStates(everyone);
      UnreadStates.Stats stats = states.markGroupUnread(group);
      assertEquals(3, stats.updates);
      for (User user : everyone) {
        UnreadCounts unreadCounts = states.countsFor(user);
        System.out.println(unreadCounts);
        assertEquals(i + 1, unreadCounts.groups.intValue());
        assertEquals(i + 1, unreadCounts.total.intValue());
      }
    }

    // Make sure all unread containers are in cache.
    for (User user : everyone) {
      UnreadState state = UnreadStates.loadStateFor(user);
      for (String group : groups) {
        assertTrue(Collections.binarySearch(state.groups, group) >= 0);
      }
    }

    UnreadStates.markGroupRead(bob, group1);

    UnreadCounts counts = UnreadStates.loadCountsFor(bob);
    assertEquals(2, counts.total.intValue());
    assertEquals(2, counts.groups.intValue());
    UnreadState state = UnreadStates.loadStateFor(bob);
    assertTrue(Collections.binarySearch(state.groups, group1) < 0);


    counts = UnreadStates.loadCountsFor(janete);
    assertEquals(3, counts.total.intValue());
    assertEquals(3, counts.groups.intValue());
  }

  @Test public void conflict() {
    for (User user : everyone) {
      UnreadStates.put(user, EMPTY);
    }
    FakeUnreadStates a = new FakeUnreadStates(everyone);
    FakeUnreadStates b = new FakeUnreadStates(everyone);
    String group = Uuids.newUuid();
    a.markGroupUnread(group);
    b.markGroupUnread(group);
    assertTrue(a.failedKeys.isEmpty());
    assertEquals(3, b.failedKeys.size());
  }

  private User newUser() {
    User user = new User();
    user.uuid = Uuids.newUuid();
    return user;
  }

  static class FakeUnreadStates extends UnreadStates {

    FakeUnreadStates(List<User> users) {
      super(users);
    }

    Set<String> failedKeys;

    @Override void refresh(Set<String> failedKeys) {
      this.failedKeys = failedKeys;
    }
  }
}
