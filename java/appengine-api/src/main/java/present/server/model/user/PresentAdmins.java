package present.server.model.user;

import com.google.appengine.api.taskqueue.RetryOptions;
import com.google.appengine.api.taskqueue.TaskOptions;
import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import javax.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import present.proto.GroupService;
import present.proto.JoinGroupRequest;
import present.server.phone.PoliteScheduler;
import present.server.RequestHeaders;
import present.server.RpcQueue;
import present.server.model.group.Group;

/**
 * @author Pat Niemeyer (pat@present.co)
 * Date: 8/8/17
 */
public class PresentAdmins {
  private static final Logger logger = LoggerFactory.getLogger(PresentAdmins.class);

  // Convenience users for remote testing.
  public static class ByName {
    public static User chauntie = Users.findByEmail("chantankson@gmail.com");
    public static User janete = Users.findByEmail("janete.perez@gmail.com");
    public static User kassia = Users.findByEmail("candysweetisme@gmail.com");
    public static User kristina = Users.findByEmail("bulldogstuff@gmail.com");
    public static User pegah = Users.findByEmail("pegah.keshavarz@yahoo.com");
    public static User pat = Users.findByEmail("pat@pat.net");
    public static User bob = Users.findByEmail("crazybob@crazybob.org");
    public static User gabrielle = Users.findByEmail("gabrielle.taylor121@gmail.com");
    public static User kayla = Users.findByEmail("kaylat10@yahoo.com");
    public static User emma = Users.findByEmail("emmahinkle@gmail.com");
  }

  // A new group was created.
  // Specified admins join new groups automatically.
  public static void newGroup(Group group) {
    User[] autoJoinAdmins = getAutoJoinAdmins();
    if (autoJoinAdmins.length < 1) { return; }

    for (User admin : autoJoinAdmins) {
      logger.debug("Auto-joining admin: "+admin.toString()+" to new group: "+group.toString());
      long delay = PoliteScheduler.delayOf(admin.privileges.autoJoinsTimeDelay);
      RetryOptions retryOptions = RetryOptions.Builder.withTaskRetryLimit(1);
      TaskOptions options = TaskOptions.Builder.withCountdownMillis(delay)
          .retryOptions(retryOptions);
      GroupService service = RpcQueue.to(GroupService.class).with(options).create();
      admin.run(() -> {
        try {
          logger.info("Admin {} attempting to run save group request.", admin.toString());
          service.joinGroup(new JoinGroupRequest(group.uuid(), true));
        } catch (IOException e) {
          logger.error("Error joining admin {} to group {}\n", admin, group);
        }
      });
    }

    //// Send all admins a notification that they have been auto-joined to the group.
    //Notifier notifier = Notifier.fromPresent()
    //    .to(Arrays.stream(autoJoinAdmins).map(User::getKey).collect(Collectors.toList()))
    //    .attachForAutojoin(group);
    //notifier.go();
  }

  // A new user has joined Present.
  // Designated admin(s) initiate a welcome chat automatically.
  public static void newUser(User newUser) {
    User welcomeAdmin = getWelcomeAdmin();
    if (welcomeAdmin == null) {
      logger.error("Welcome admin not found");
      return;
    }
    String template = welcomeAdmin.privileges.welcomesNewUsersTemplate;
    if (template == null) {
      logger.error("Welcome admin missing template");
      return;
    }

    /*
    logger.info("Initiating welcome chat from admin: {} to user: {}", welcomeAdmin, newUser);
    String message = WelcomeMessageTemplate.welcomeMessageFromTo(welcomeAdmin, newUser, template);

    final present.proto.Coordinates coordinates
        = present.server.model.util.Coordinates.SAN_FRANCISCO.toProto();
    // Derive the comment ID from the user ID so we don't send it more than once.
    String commentId = Uuids.fromName("Welcome Message: " + newUser.uuid());
    SendChatMessageToUsersRequest request = new SendChatMessageToUsersRequest(
        commentId, Collections.singletonList(newUser.uuid), coordinates, message, null);
    */
  }

  // Find admins with the welcomes new users privilege
  // If more than one admin is found we choose a random one.
  // @return null if no such user is found.
  static @Nullable User getWelcomeAdmin() {
    List<User> list =
        Users.query()
            .filter("privileges.welcomesNewUsers", true)
            .list();
    if (list.isEmpty()) { return null; }
    Collections.shuffle(list);
    User chosen = list.get(0);
    logger.info("Found {} welcome new user admins: {}, choosing: {}", list.size(), list, chosen);
    return chosen;
  }

  private static User[] queryAutoJoinGroupAdmins() {
    List<User> list = Users.query()
        .filter("privileges.autoJoinsNewCircles", true)
        .list();
    logger.info("Found {} auto join group admins: {}", list.size(), list);
    return list.toArray(new User[0]);
  }

  private static final Supplier<User[]> autoJoinAdmins = Suppliers.memoizeWithExpiration(
      PresentAdmins::queryAutoJoinGroupAdmins, 60, TimeUnit.MINUTES);

  // Find admins with the auto join new circles privilege
  public static User[] getAutoJoinAdmins() {
    logger.info("Finding auto join admins now.\n");
    if (RequestHeaders.isTest()) return queryAutoJoinGroupAdmins();
    return autoJoinAdmins.get();
  }

  public static void makeAdmin(User user) {
    user.inTransaction(u -> { u.privileges.isAdmin = true; });
  }

  public static void addWelcomer(User user) {
    logger.info("Setting user: {} as welcomer (previously set: {})\n",
        user.publicName(), user.privileges.welcomesNewUsers);

    String welcomesNewUsersTemplate = "Hi TEMPLATE:RECIPIENT_FIRST_NAME!\n\n"
        + "Welcome to Present! I'm TEMPLATE:SENDER_FIRST_NAME TEMPLATE:EMOJI_HAND_WAVE. "
        + "Thank you for joining our community of inspiring women! "
        + "Our mission is to bring women together in a supportive environment, "
        + "giving opportunities to engage around common interests. "
        + "Need help finding awesome circles nearby? "
        + "I can help! What things would you like to do?";

    user.privileges.welcomesNewUsers = true;
    if (user.privileges.welcomesNewUsersTemplate == null) {
      user.privileges.welcomesNewUsersTemplate = welcomesNewUsersTemplate;
    } else {
      logger.info("Keeping existing welcome message.");
    }
    user.save().now();
  }
  public static void removeWelcomer(User user) {
    logger.info("Removing user: {} as welcomer (previously set: {})\n",
        user.publicName(), user.privileges.welcomesNewUsers);

    user.privileges.welcomesNewUsers = false;
    user.save().now();
  }

  /**
   * Adds a user to automatically join all circles with no time delay. Convenience constructor.
   * @param user user to auto-join circle
   */
  public static void addAutoJoinUser(User user) {
    addAutoJoinUser(user, 0L, TimeUnit.MILLISECONDS);
  }

  /**
   * Adds a user to automatically join all circles after a given time delay.
   * @param user user to auto-join circle
   * @param timeDelay amount of time to delay joining circle
   * @param timeUnit unit of time to delay joining circle
   * @throws IllegalArgumentException when negative values provided for timeDelay
   */
  public static void addAutoJoinUser(User user, long timeDelay, TimeUnit timeUnit) {
    if (timeDelay < 0L) {
      throw new IllegalArgumentException("Negative time delays are not permitted.");
    }
    logger.info("Setting user: {} as auto joining circles (previously set: {}).\n",
        user.publicName(), user.privileges.autoJoinsNewCircles);
    user.privileges.autoJoinsNewCircles = true;
    user.privileges.autoJoinsTimeDelay = TimeUnit.MILLISECONDS.convert(timeDelay, timeUnit);
    user.save().now();
  }

  /**
   * Removes user from automatically joining all circles.
   * @param user user to stop auto-joining circle
   */
  public static void removeAutoJoinUser(User user) {
    logger.info("Removing user: {} as auto joining circles (previously set: {})\n",
            user.publicName(), user.privileges.autoJoinsNewCircles);
    user.privileges.autoJoinsNewCircles = false;
    user.save().now();
  }

  /**
   * Changes user auto-join time delay.
   * @param user
   * @param timeDelay
   * @param timeUnit
   * @throws IllegalArgumentException when negative values provided for timeDelay
   */
  public static void setAutoJoinUserDelay(User user, Long timeDelay, TimeUnit timeUnit) {
    if (timeDelay < 0L) {
      throw new IllegalArgumentException("Negative time delays are not permitted.");
    }
    logger.info("Setting user: {} auto join time delay as {} {}.\n", user.publicName(), timeDelay, timeUnit);
    if (timeUnit != TimeUnit.MILLISECONDS) {
      timeDelay = TimeUnit.MILLISECONDS.convert(timeDelay, timeUnit);
    }
    user.privileges.autoJoinsTimeDelay = timeDelay;
    user.save().now();
  }

}
