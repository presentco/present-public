package present.acceptance;

import com.google.common.collect.Lists;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import present.proto.*;
import present.server.model.group.Group;
import present.server.model.group.Groups;
import present.server.model.user.PresentAdmins;
import present.server.model.user.User;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static present.acceptance.AcceptanceTest.*;
import static org.junit.Assert.*;

/**
 * Tests admin auto-join group feature.
 *
 * @author Gabrielle Taylor (gabrielle@present.co)
 */
public class AutoJoinGroupTest {
  private static final Logger logger = LoggerFactory.getLogger(AutoJoinGroupTest.class);

  private AcceptanceTest tests;

  public AutoJoinGroupTest(AcceptanceTest tests) {
    this.tests = tests;
  }

  public void testAutoJoin() throws IOException, InterruptedException {
    GroupService groupService = tests.groupService;
    UserService userService = tests.userService;
    Coordinates location = tests.location;

    // Create an admin who auto-joins circles with no time delay.
    User first = createAdmin("First");
    PresentAdmins.addAutoJoinUser(first);

    // Create an admin who auto-joins circles after a 50 ms time delay.
    User second = createAdmin("Second");
    PresentAdmins.addAutoJoinUser(second, 50L, TimeUnit.MILLISECONDS);

    // Attempting to create an admin who auto-joins circles, but a negative time delay is provided.
    User third = createAdmin("ThirdNegative");
    try {
      PresentAdmins.addAutoJoinUser(third, -10L, TimeUnit.MILLISECONDS);
    } catch (IllegalArgumentException e) {
      assertEquals(e.getMessage(), "Negative time delays are not permitted.");
    }

    // Attempting to change an admin's time delay to a negative time.
    try {
      PresentAdmins.setAutoJoinUserDelay(first, -6L, TimeUnit.SECONDS);
    } catch (IllegalArgumentException e) {
      assertEquals(e.getMessage(), "Negative time delays are not permitted.");
    }

    // Create an admin who does not auto-join circles.
    User fourth = createAdmin("FourthNotAuto");

    TestUser gabrielle = tests.signUp(new TestUser("Gabrielle", "Taylor")); // Gabrielle is the current user

    // Gabrielle creates a new group.
    String groupTitle = "Group #" + randomNumber(9);
    String groupId = newUuid();
    groupService.putGroup(new PutGroupRequest.Builder()
        .uuid(groupId)
        .location(location)
        .title(groupTitle)
        .createdFrom(location)
        .locationName("Present HQ")
        .build());

    // Gabrielle's group was auto-joined by her and admin1 when she created it.
    tests.retry(() -> {
      Group group = Groups.findByUuid(groupId);
      List<User> members = Lists.newArrayList(group.getMembers(10));
      assertTrue(members.contains(first));
    });

    Thread.sleep(50L);

    // Gabrielle's group was auto-joined by admin2 after the provided delay.
    // Neither admin3 nor admin4 are in the group.
    tests.retry(() -> {
      Group group = Groups.findByUuid(groupId);
      List<User> members = Lists.newArrayList(group.getMembers(10));
      assertTrue(members.contains(first));
      assertTrue(members.contains(second));
      assertFalse(members.contains(third));
      assertFalse(members.contains(fourth));
    });

  }

  private User createAdmin(String name) throws IOException {
    TestUser testUser = tests.signUp(new TestUser(name, "Admin"));
    return User.get(testUser.getUserId());
  }
}
