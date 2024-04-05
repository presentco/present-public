package present.server;

import com.google.appengine.tools.development.testing.LocalDatastoreServiceTestConfig;
import com.google.appengine.tools.development.testing.LocalServiceTestHelper;
import com.google.common.collect.ImmutableList;
import com.googlecode.objectify.ObjectifyService;
import com.googlecode.objectify.Ref;
import com.googlecode.objectify.util.Closeable;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import present.server.model.PresentEntities;
import present.server.model.Space;
import present.server.model.group.Group;
import present.server.model.group.GroupRanker;
import present.server.model.group.GroupSearch;
import present.server.model.group.RankedGroup;
import present.server.model.user.User;
import present.server.model.user.UserState;
import present.server.model.user.Users;
import present.server.model.util.Coordinates;

import static com.googlecode.objectify.ObjectifyService.ofy;
import static java.util.stream.Collectors.toList;
import static org.junit.Assert.assertEquals;

public class GroupSearchTest {

  static {
    PresentEntities.registerAll();
  }

  private final LocalServiceTestHelper helper =
      new LocalServiceTestHelper(new LocalDatastoreServiceTestConfig());

  private Closeable objectify;

  User bob = new User();
  {
    bob.uuid = Uuids.newUuid();
    bob.state = UserState.MEMBER;
  };

  @Before public void setUp() {
    Users.setCurrent(bob);
    helper.setUp();
    this.objectify = ObjectifyService.begin();
  }

  @After public void tearDown() {
    objectify.close();
    helper.tearDown();
    Users.setCurrent(null);
  }

  @Test public void testGroupSearch() throws InterruptedException {
    // Populate test groups.
    List<Group> groups = new ArrayList<>();
    for (Neighborhood neighborhood : Neighborhoods.ALL) {
      Group group = new Group();
      group.id = Uuids.newUuid();
      group.spaceId = Space.EVERYONE.id;
      group.lastUpdateMonth = Time.epochMonth();
      group.title = neighborhood.name();
      group.setLocation(neighborhood.location());
      group.owner = Ref.create(bob);
      groups.add(group);
    }
    ofy().save().entity(bob).now();
    ofy().save().entities(groups).now();

    List<String> expected = ImmutableList.of(
        "Union Square",
        "Chinatown",
        "Financial District",
        "Nob Hill",
        "Tenderloin"
    );

    List<Group> found = GroupSearch
        .near(Coordinates.PRESENT_COMPANY)
        .space(Space.EVERYONE)
        .using(GroupRanker.EXPLORE)
        .limit(100)
        .run()
        .stream()
        .map(RankedGroup::group)
        .collect(toList());
    assertEquals(expected, found.stream().limit(5).map(g -> g.title)
        .collect(Collectors.toList()));
  }
}
