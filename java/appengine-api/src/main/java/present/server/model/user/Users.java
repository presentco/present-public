package present.server.model.user;

import com.google.appengine.api.datastore.Cursor;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.KeyFactory;
import com.google.appengine.api.datastore.Query.Filter;
import com.google.appengine.api.datastore.Query.FilterPredicate;
import com.google.appengine.api.datastore.QueryResultIterable;
import com.google.common.base.Function;
import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;
import com.google.common.base.Stopwatch;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Streams;
import com.googlecode.objectify.Key;
import com.googlecode.objectify.Ref;
import com.googlecode.objectify.cmd.Query;

import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import present.proto.AdminSearchRequest;
import present.proto.GroupMembershipState;
import present.proto.ResolveUrlResponse;
import present.server.Keys;
import present.server.ShortLinks;
import present.server.Uuids;
import present.server.environment.Environment;
import present.server.facebook.FacebookFriendship;
import present.server.model.PresentEntities;
import present.server.model.activity.Events;
import present.server.model.comment.Comments;
import present.server.model.group.Group;
import present.server.model.group.GroupMembership;
import present.server.model.group.Groups;
import present.server.model.util.Coordinates;
import present.server.tool.ProductionUsers;
import present.server.tool.StagingUsers;
import present.wire.rpc.core.ClientException;

import static com.google.appengine.api.datastore.Entity.KEY_RESERVED_PROPERTY;
import static com.google.appengine.api.datastore.Query.CompositeFilterOperator.and;
import static com.google.appengine.api.datastore.Query.FilterOperator.EQUAL;
import static com.googlecode.objectify.ObjectifyService.ofy;
import static present.server.MoreObjectify.startsWith;
import static present.server.model.PresentEntities.expected;
import static present.server.model.user.User.Fields.email;
import static present.server.model.user.User.Fields.firstName;
import static present.server.model.user.User.Fields.lastName;
import static present.server.model.user.User.Fields.phoneNumber;

/**
 * User factories.
 *
 * @author Bob Lee (bob@present.co)
 */
public class Users {
  private static final Logger logger = LoggerFactory.getLogger(Users.class);

  public static Query<User> query() { return ofy().load().type(User.class); }
  public static Query<User> active() { return PresentEntities.active(query()); }
  public static List<User> all() { return query().chunkAll().list(); }
  public static Stream<User> stream() { return Streams.stream(all()); }

  public static List<User> members() {
    return query().filter(User.Fields.state.name(), UserState.MEMBER.name()).list();
  }

  public static List<User> admins() {
    return query().filter("privileges.isAdmin", true).list();
  }

  private static ThreadLocal<User> localUser = new ThreadLocal<>();

  public static void setCurrent(User user) {
    localUser.set(user);
  }

  /** Retrieves the current user. Returns null if the user isn't logged in. */
  public static User current() {
    return current(true);
  }

  /**
   * Return the current user for the request or null if the user is not yet logged in.
   *
   * @throws ClientException if requireMembership and user is not a member
   */
  public static User current(boolean requireMembership) {
    User user = localUser.get();
    if (requireMembership) {
      if (user == null) throw new ClientException("Client isn't logged in.");
      if (!user.hasAccess()) throw new ClientException("User is not a member.");
    }
    return user;
  }

  /**
   * The same as current(requireMembership) except that when requireMembership is false the User is still
   * expected to be non-null.
   */
  public static User expectedCurrent(boolean requireMembership) {
    return expected(current(requireMembership), "User is not logged in.");
  }

  public static Map<Key<User>, User> load(Iterable<String> userIds) {
    if (userIds == null) return Collections.emptyMap();
    Iterable<Key<User>> keys = Iterables.transform(userIds, User::keyFor);
    return ofy().load().keys(keys);
  }

  public static User findByPhone(String phone) {
    PhoneToUser result = ofy().load().key(PhoneToUser.key(phone)).now();
    if (result == null) return null;
    return result.user.get();
  }

  /** Looks up a user by phone, creating a new user if necessary. */
  public static User getOrCreateByPhone(String phoneNumber) {
    Map<String, User> byPhone = getOrCreateByPhone(Collections.singleton(phoneNumber));
    return byPhone.get(phoneNumber);
  }

  /** Looks up users by phone, creating new users if necessary. */
  public static Map<String, User> getOrCreateByPhone(Iterable<String> phoneNumbers) {
    if (phoneNumbers == null) return Collections.emptyMap();
    List<Key<PhoneToUser>> keys = Streams.stream(phoneNumbers)
        .map(PhoneToUser::key)
        .collect(Collectors.toList());
    Map<Key<PhoneToUser>, PhoneToUser> byKey = ofy().load().keys(keys);
    List<User> newUsers = new ArrayList<>();
    Map<String, User> byPhone = new HashMap<>();
    for (String phoneNumber : phoneNumbers) {
      PhoneToUser ptu = byKey.get(PhoneToUser.key(phoneNumber));
      if (ptu != null) {
        byPhone.put(phoneNumber, ptu.user.get());
      } else {
        User user = Users.create();
        user.state = UserState.INVITED;
        user.phoneNumber = phoneNumber;
        newUsers.add(user);
        byPhone.put(phoneNumber, user);
      }
    }
    // Save synchronously so we don't create broken references later in case the save fails.
    if (!newUsers.isEmpty()) ofy().save().entities(newUsers).now();
    return byPhone;
  }

  /**
   * Return the user or null if not found.
   */
  public static @Nullable User findByEmail(String email) {
    return Users.query().filter(User.Fields.email.name(), email.toLowerCase()).first().now();
  }

  public static List<User> findAllByEmail(String email) {
    return Users.query().filter(User.Fields.email.name(), email.toLowerCase()).list();
  }

  /**
   * Return the user or throw an exception if now found.
   */
  public static User findByEmailExpected(String email) {
    User user = findByEmail(email);
    if (user == null) {
      throw new RuntimeException("User not found: " + email);
    }
    return user;
  }

  private static final String GENERIC_ADMIN_USER_UUID = "05637eb6-6dca-4ec1-9018-40dd6a9d2bb4";

  /**
   * A stand-in user for testing tasks, e.g. testing messaging.
   * Note: We currently load the admin user from a reserved phone. If we decide to bake the user
   * into the app we need to save an actual entity because there are cases where we will try to
   * load the user by key.
   */
  public static User getGenericAdminUser() {
    User admin = User.get(GENERIC_ADMIN_USER_UUID);
    if (admin == null) admin = installGenericAdminUser();
    return admin;
  }

  /**
   * Create our admin user.
   * TODO: This was originally set up as a persistent user so that we could use the customize
   * TODO: app to customize the appearance.  But perhaps this should be completely transient now?
   */
  public static User installGenericAdminUser() {
    User admin = new User();
    admin.uuid = GENERIC_ADMIN_USER_UUID;
    admin.signupTime = new GregorianCalendar(2017, 1, 1).getTime().getTime();
    admin.signupLocation = Coordinates.SAN_FRANCISCO;
    admin.firstName = "Admin";
    admin.lastName = "User";
    admin.bio = "Present Admin";
    admin.state = UserState.MEMBER;
    admin.photo = null; // Use the default missing user photo
    admin.privileges.isAdmin = true;
    admin.save().now();

    logger.info("Installed generic admin user: {}", admin.debugString());
    return admin;
  }

  /**
   * Run the runnable in the context of a request by the generic admin user.
   */
  public static void runAsGenericAdminUserRequest(Runnable runnable) {
    User genericAdminUser = getGenericAdminUser();
    genericAdminUser.run(runnable);
  }

  public static User findByFacebookId(String facebookId) {
    return Users.query().filter("facebookId", facebookId).first().now();
  }

  public static User create() {
    User user = new User();
    user.uuid = UUID.randomUUID().toString();
    user.setShortId();
    user.nascent = true;
    return user;
  }

  public static BlockedUsers getBlockedUsers() {
    return getBlockedUsers(current());
  }

  public static Iterable<User> loadBlockedUsers() {
    return loadBlockedUsers(current());
  }

  /**
   * Get the BlockedUsers object for the user or null if it has not yet been created.
   */
  public static BlockedUsers getBlockedUsers(User user) {
    return BlockedUsers.loadFor(user);
  }

  public static BlockedUsers getOrCreateBlockedUsers(Ref<User> userRef) {
    BlockedUsers invBlockedUsers = getBlockedUsers(userRef.get());
    if (invBlockedUsers == null) {
      invBlockedUsers = new BlockedUsers(userRef);
    }
    return invBlockedUsers;
  }

  /**
   * Return the set of blocked users for the user or the empty set if there are none.
   */
  public static Set<Ref<User>> getBlockedUserSet(User user) {
    BlockedUsers blockedUsers = getBlockedUsers(user);
    if (blockedUsers == null) {
      return Collections.emptySet();
    }
    return blockedUsers.users;
  }

  /**
   * Async load and return the users set.
   * @return the Users or an empty iterable if there are none.
   */
  public static Iterable<User> loadBlockedUsers(User user) {
    BlockedUsers blockedUsers = getBlockedUsers(user);
    if (blockedUsers == null) {
      return Collections.emptySet();
    }
    Map<Key<User>, User> users = ofy().load().refs(blockedUsers.users); // kick off loading async
    return users.values();
  }

  public static Iterable<Ref<User>> findUsersBlockingUser(User user) {
    return Iterables.transform(
          Iterables.transform(
            ofy().load().type(BlockedUsers.class).filter("users", user).keys().iterable(),
              new Keys.KeyToParentKey<BlockedUsers, User>()
          ), new Keys.KeyToRef<User>()
        );
  }

  public static Iterable<Ref<User>> idsToRefs(Iterable<String> userIds) {
    return Iterables.transform(userIds, new Function<String, Ref<User>>() {
      @Override public Ref<User> apply(String userId) {
        return Ref.create(Key.create(User.class, userId));
      }
    });
  }

  public static Iterable<String> refsToIds(Iterable<Ref<User>> userRefs) {
    return Iterables.transform(userRefs, new Function<Ref<User>, String>() {
      @Override public String apply(Ref<User> userRef) {
        return userRef.getKey().getName();
      }
    });
  }

  /**
   * Compose a list of user names containing one or two full user names and an "N others" clause
   * as needed. e.g. The cases for 1-4 users:
   *   "Pat Niemeyer"
   *   "Pat Niemeyer and Bob Lee"
   *   "Pat Niemeyer, Bob Lee, and 1 other"
   *   "Pat Niemeyer, Bob Lee, and 2 others"
   */
  public static String oneTwoOrMoreUsers(User [] users) {
    if (users.length == 0) {
      throw new IllegalArgumentException();
    }
    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < users.length; i++) {
      if (i < 2) {
        if (i > 0) {
          if (users.length == 2) {
            sb.append(" and ");
          } else {
            sb.append(", ");
          }
        }
        sb.append(users[i].publicName());
      } else {
        sb.append(", and " + (users.length - 2) + " other" + (users.length > 3 ? "s" : ""));
        break;
      }
    }
    return sb.toString();
  }

  /**
   * Deletes a user and all of their associated data. Intended for testing.
   */
  public static void cascadingDelete(Key<User> key) {
    if (Environment.isProduction()) throw new UnsupportedOperationException();

    // TODO: BlockedUser, CommentContainerView, SavedGroups, Contact, FacebookData
    // Kick off queries in parallel.
    Iterable<Key<?>> keys = Iterables.concat(
        Collections.singleton(key),
        Clients.query().filter("user", key).keys().list(),
        Groups.query().filter("owner", key).keys().list(),
        Comments.query().filter("author", key).keys().list(),
        Events.query().filter("initiator", key).keys().list(),
        ofy().load().type(FacebookFriendship.class).filter("userIds", key.getName()).keys().list()
    );

    ofy().delete().keys(keys);
  }

  /**
   * Atomically increments User.unreadVersion. Returns the user if successful, null otherwise.
   */
  public static User tryIncrementUnreadVersion(String userId, int expectedVersion) {
    return ofy().transact(() -> {
      User user = User.get(userId);
      if (user == null) {
        logger.error("User #{} not found.", userId);
        return null;
      }
      if (user.unreadVersion != expectedVersion) {
        logger.info("Unread count computation already in progress. Skipping this one.");
        return null;
      }
      user.unreadVersion++;
      user.save();
      return user;
    });
  }

  static final ThreadLocal<Boolean> allowInvisibleUsers = ThreadLocal.withInitial(() -> false);

  /** Allows the rendering of invisible users in the given context. */
  public static <T> T allowInvisibleUsers(Supplier<T> task) {
    if (allowInvisibleUsers.get()) {
      return task.get();
    }
    try {
      allowInvisibleUsers.set(true);
      return task.get();
    } finally {
      allowInvisibleUsers.set(false);
    }
  }

  public static User janete() {
    return Environment.isProduction() ? ProductionUsers.janete() : StagingUsers.janete();
  }

  public static User bob() {
    return Environment.isProduction() ? ProductionUsers.bob() : StagingUsers.bob();
  }

  /**
   * Merges one user account into another. Currently supports placeholder users only.
   * Copies group memberships and friendships. Placeholder users should have no other state.
   * Deletes the placeholder user and their associated state. This isn't transactional.
   * If something fails along the way, it can leave some garbage behind.
   */
  public static void merge(User from, User to) {
    /*
     * Note: If we want to support merging normal users, we also need to update:
     *
     * - Clients
     * - Group ownerships
     * - Comments
     * - Blocks
     * - Events
     * - etc.?
     */

    Preconditions.checkState(!Users.current().equals(from),"Bad idea.");

    Preconditions.checkState(from.state == UserState.INVITED,
        "Only merging placeholder users is allowed.");

    Stopwatch stopwatch = Stopwatch.createStarted();

    logger.info("Merging placeholder user {} into user {}.", from, to);

    List<GroupMembership> oldMemberships = GroupMembership.query(from).list();
    List<Friendship> oldFriendships = Friendship.rawRequestsTo(from);

    logger.info("Found {} group memberships and {} friendship requests.",
        oldMemberships.size(), oldFriendships.size());

    List<Object> newEntities = new ArrayList<>();

    Set<Group> groups = GroupMembership.query(to).list().stream()
        .map(GroupMembership::group)
        .collect(Collectors.toSet());
    for (GroupMembership oldMembership : oldMemberships) {
      Group group = oldMembership.group();
      if (groups.contains(group)) {
        // Don't overwrite existing membership requests. This could throw off our request counts.
        logger.info("Skipping {}", group);
        continue;
      }
      if (oldMembership.state != GroupMembershipState.ACTIVE) {
        logger.warn("Expected membership in {} to be ACTIVE!", oldMembership.group());
      }
      newEntities.add(oldMembership.copyFor(to));
    }

    // Set of all people the destination user already has a relationship with.
    Set<User> relationships = new HashSet<>();
    ofy().load().type(Friendship.class)
        .filter("userIds", to.uuid)
        .list()
        .forEach(f -> {
          relationships.add(f.requestor.get());
          relationships.add(f.requestee.get());
        });

    int newRequests = 0;
    for (Friendship oldFriendship : oldFriendships) {
      // The placeholder user will always be "requestee".
      User requestor = oldFriendship.requestor.get();
      if (relationships.contains(requestor)) {
        logger.info("Skipping {}", requestor);
        continue;
      }
      newEntities.add(oldFriendship.copyRequestTo(to));
      // The state will always be REQUESTED.
      newRequests++;
    }

    if (!newEntities.isEmpty()) {
      // Synchronous. Make sure the entities are saved before we delete the old entities.
      ofy().save().entities(newEntities).now();
    }

    if (newRequests > 0) {
      int finalNewRequests = newRequests;
      logger.info("Adding {} new incoming friend requests.", finalNewRequests);
      to.inTransaction(u -> { u.incomingFriendRequests += finalNewRequests; });
    }

    // Delete old entities.
    logger.info("Deleting placeholder user...");
    if (!oldMemberships.isEmpty()) ofy().delete().entities(oldMemberships);
    if (!oldFriendships.isEmpty()) ofy().delete().entities(oldFriendships);
    ofy().delete().entity(from);

    logger.info("Finished merging in {}.", stopwatch);
  }

  /** Retrieves the user at the given URL. */
  public static User fromUrl(String url) {
    return User.get(ShortLinks.resolve(url).user.id);
  }


  private static Filter keyFilter(String uuid) {
    return new FilterPredicate(KEY_RESERVED_PROPERTY, EQUAL, KeyFactory.createKey("User", uuid));
  }

  /**
   * Convenience method. Searches users by name, email, and phone.
   */
  public static List<User> search(String queryString, int limit) {
    return Lists.newArrayList(search(queryString, null, null, limit));
  }

  /** Searches users by name, email, and phone. */
  public static QueryResultIterable<User> search(String queryString, Cursor cursor,
      AdminSearchRequest.Direction direction, int limit) {
    // TODO: Support multiple terms with AND.
    List<Filter> filters = new ArrayList<>();
    if (queryString != null) {
      String lowercase = queryString.trim().toLowerCase();
      if (!lowercase.isEmpty()) {
        if (Uuids.isValid(lowercase)) {
          filters.add(keyFilter(lowercase));
          filters.add(keyFilter(lowercase.toUpperCase()));
        } else if (lowercase.startsWith("https://")) {
          // Case matters here.
          ResolveUrlResponse response = ShortLinks.resolve(queryString.trim());
          if (response.user != null) {
            filters.add(keyFilter(response.user.id));
          }
        } else {
          String capitalized = lowercase.substring(0, 1).toUpperCase() + lowercase.substring(1);
          filters.add(startsWith(firstName.name(), lowercase));
          filters.add(startsWith(firstName.name(), capitalized));
          filters.add(startsWith(lastName.name(), lowercase));
          filters.add(startsWith(lastName.name(), capitalized));
          filters.add(startsWith(email.name(), lowercase));

          String phone = lowercase.replaceAll("\\D+","");
          if (!phone.startsWith("1")) phone = "1" + phone;
          if (phone.length() >= 4) {
            filters.add(startsWith(phoneNumber.name(), phone));
          }
        }
      }
    }

    Query<User> query = Users.query();

    Long count = null;
    if (!filters.isEmpty()) {
      if (filters.size() == 1) {
        query = query.filter(filters.get(0));
      } else {
        query = query.filter(com.google.appengine.api.datastore.Query.CompositeFilterOperator.or(filters));
      }

      // Note: We don't order because we don't have composite indexes.
      // Note: We don't compute the count as this would require us to count all results.
    } else {
      // When returning all users, sort by signup time.
      query = query.order("-" + User.Fields.signupTime.name());

      try {
        Entity userStatEntity = DatastoreServiceFactory.getDatastoreService().get(
            KeyFactory.createKey("__Stat_Kind__", "User"));
        count = (Long) userStatEntity.getProperty("count");
      } catch (Exception e) {
        logger.warn("No entity statistic.");
      }
    }

    query = query.limit(limit);

    if (cursor != null) {
      if (direction == null) throw new ClientException("Missing direction");
      if (direction == AdminSearchRequest.Direction.FORWARD) {
        query = query.startAt(cursor);
      } else {
        query = query.endAt(cursor);
      }
    }

    return query.iterable();
  }
}
