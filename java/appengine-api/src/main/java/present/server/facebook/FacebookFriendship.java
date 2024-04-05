package present.server.facebook;

import com.google.common.collect.Iterables;
import com.googlecode.objectify.Key;
import com.googlecode.objectify.annotation.Cache;
import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Id;
import com.googlecode.objectify.annotation.Index;
import com.googlecode.objectify.annotation.OnLoad;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import present.server.MoreObjectify;
import present.server.UuidPair;
import present.server.model.user.Friendship;
import present.server.model.user.User;
import present.server.model.user.Users;

import static com.googlecode.objectify.ObjectifyService.ofy;

/**
 * Represent a friendship between two users on Facebook.
 *
 * @author Bob Lee (bob@present.co)
 */
@Entity @Cache public class FacebookFriendship {

  private static final Logger logger = LoggerFactory.getLogger(FacebookFriendship.class);

  /** '[UUID A]:[UUID B]', where UUIDs A and B are in lexicographically sorted order. */
  @Id public String id;

  /** The two user's IDs. */
  @Index public Set<String> userIds;

  /** Returns the key for this entity. */
  public Key<FacebookFriendship> key() {
    return Key.create(FacebookFriendship.class, id);
  }

  /** Creates a canonical instance for the two given user IDs. */
  private static FacebookFriendship newInstance(String ida, String idb) {
    FacebookFriendship ff = new FacebookFriendship();
    UuidPair id = UuidPair.sort(ida, idb);
    ff.id = id.toString();
    ff.userIds = id.toSet();
    return ff;
  }

  /**
   * Saves a user's friends for later retrieval by {@link #friendsOf(User)}. Costs two reads
   * plus one write per newly discovered friendship.
   */
  public static void saveFriends(User user, FacebookUserData facebookData) {
    // TODO: Remove friendships!
    if (!facebookData.hasFriends()) return;
    Set<String> facebookIds = Arrays.stream(facebookData.friends.data)
        .map(friend -> friend.id)
        .collect(Collectors.toSet());
    if (!facebookIds.isEmpty()) {
      List<FacebookFriendship> allFriendships = Users.query()
          .filter("facebookId in", facebookIds)
          .keys()
          .list()
          .stream()
          .map(Key::getName)
          .map(userId -> newInstance(user.uuid, userId))
          .collect(Collectors.toList());
      Map<Key<FacebookFriendship>, FacebookFriendship> newFriends
          = MoreObjectify.saveIfAbsent(allFriendships);

      // New Facebook friends are added as Present friends by default.
      Friendship.addFor(user, newFriends.values());

      logger.info("Added {} new friends out of {} total.", newFriends.size(), allFriendships.size());
    }
  }

  /** Looks up friends of the given user. */
  public static Iterable<User> friendsOf(User user) {
    Iterable<Key<User>> friendKeys = Iterables.transform(friendIdsFor(user), User::keyFor);
    // This will unfortunately block.
    return ofy().load().keys(friendKeys).values();
  }

  /** Asynchronously retrieves the IDs of the other user's friends. */
  public static Iterable<String> friendIdsFor(User user) {
    List<Key<FacebookFriendship>> keys = ofy().load()
        .type(FacebookFriendship.class)
        .filter("userIds", user.uuid)
        .keys()
        .list();
    return Iterables.transform(keys, k -> otherUserId(user, k.getName()));
  }

  /** Takes a user and a FacebookFriendship ID and returns the friend's ID. */
  private static String otherUserId(User user, String friendshipId) {
    UuidPair id = UuidPair.parse(friendshipId);
    return id.otherThan(user.uuid);
  }
}