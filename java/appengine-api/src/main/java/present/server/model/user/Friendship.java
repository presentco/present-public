package present.server.model.user;

import com.google.common.base.Preconditions;
import com.google.common.collect.Iterables;
import com.googlecode.objectify.Key;
import com.googlecode.objectify.Ref;
import com.googlecode.objectify.annotation.Cache;
import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Id;
import com.googlecode.objectify.annotation.Index;
import com.googlecode.objectify.annotation.Load;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import present.proto.ActivityType;
import present.proto.FriendshipState;
import present.proto.UnreadState;
import present.server.KeysOnly;
import present.server.MoreObjectify;
import present.server.UuidPair;
import present.server.facebook.FacebookFriendship;
import present.server.model.BasePresentEntity;
import present.server.model.activity.Event;
import present.server.notification.Notification;
import present.server.notification.Notifier;
import present.server.phone.PhoneServices;
import present.wire.rpc.core.ClientException;

import static com.googlecode.objectify.ObjectifyService.ofy;
import static present.server.tool.RemoteTool.PRODUCTION_SERVER;
import static present.server.tool.RemoteTool.STAGING_SERVER;
import static present.server.tool.RemoteTool.against;

/**
 * Represent a friendship between two users.
 *
 * @author Bob Lee (bob@present.co)
 */
@Entity @Cache public class Friendship extends BasePresentEntity<Friendship> {

  private static final Logger logger = LoggerFactory.getLogger(Friendship.class);

  /** A sorted UuidPair. */
  @Id public String id;

  /** The two user's IDs. */
  @Index public Set<String> userIds;

  @Index public FriendshipState state;

  /** The user that requested the friendship. Null if it came from Facebook. */
  @Load(unless = KeysOnly.class) @Index public Ref<User> requestor;

  /** The user whose friendship was requested. Null if it came from Facebook. */
  @Load(unless = KeysOnly.class) @Index public Ref<User> requestee;

  public static FriendshipState addFriend(User current, User other) {
    if (!other.canSee(current)) throw new ClientException("Not allowed.");
    if (current.equals(other)) throw new ClientException("Not allowed.");

    Notifier notifier = Notifier.from(current).to(other);

    FriendshipState state = ofy().transact(() -> {
      UuidPair id = uuidPair(current, other);
      Friendship friendship = ofy().load().type(Friendship.class).id(id.toString()).now();

      if (friendship == null) {
        // New request!
        friendship = new Friendship();
        friendship.id = id.toString();
        friendship.userIds = id.toSet();
        friendship.state = FriendshipState.REQUESTED;
        friendship.requestor = current.getRef();
        friendship.requestee = other.getRef();
        friendship.save().now();

        other.inTransaction(u -> { u.incomingFriendRequests++; });

        if (other.state == UserState.INVITED) {
          PhoneServices.sms(other,
              current.fullName() + " added you as friend on Present: " + current.shortLink());
        }

        String message = current.firstName + " sent you a friend request";
        notifier.stage(new Notification()
            .body(message)
            .put(current)
            .event(new Event(ActivityType.FRIEND_REQUEST, message, current.getRef())));

        // Compute badge count so it's included with the notification.
        UnreadState unreadState = notifier.unreadStates().stateFor(other);
        if (unreadState == null) {
          UnreadStates.recomputeUnreadCountsFor(other);
        }

        return friendship.state;
      } else if (current.equals(friendship.requestee.get())
            && friendship.state == FriendshipState.REQUESTED) {
          // Accept friendship request.
          friendship.state = FriendshipState.ACCEPTED;
          friendship.save();
          current.inTransaction(u -> { u.incomingFriendRequests--; });

          String message = current.firstName + " accepted your friend request";
          notifier.stage(new Notification()
              .body(message)
              .put(current)
              .event(new Event(ActivityType.FRIEND_ACCEPT, message, current.getRef())));
          return friendship.state;
      } else {
        // Duplicate request or accept
      }

      return friendship.state;
    });

    notifier.commit();

    return state;
  }

  public static void removeFriend(User current, User other) {
    if (current.equals(other)) throw new ClientException("Not allowed.");

    // Note: We keep a log of friendship state changes.
    String id = uuidPair(current, other).toString();
    ofy().transact(() -> {
      Key<Friendship> key = Key.create(Friendship.class, id);
      Friendship friendship = ofy().load().key(key).now();
      if (friendship != null) {
        if (friendship.state == FriendshipState.REQUESTED) {
          friendship.requestee.get().inTransaction(u -> { u.incomingFriendRequests--; });
        }
        ofy().delete().key(key);
      }
    });
  }

  private static UuidPair uuidPair(User a, User b) {
    return UuidPair.sort(a.uuid, b.uuid);
  }

  /** Looks up friends of the given user. */
  public static Iterable<User> friendsOf(User user) {
    Iterable<Key<User>> friendKeys = Iterables.transform(friendIdsFor(user), User::keyFor);
    // This will unfortunately block.
    return ofy().load().keys(friendKeys).values();
  }

  /** Asynchronously retrieves the IDs of the other user's friends. */
  public static Iterable<String> friendIdsFor(User user) {
    List<Key<Friendship>> keys = ofy().load().type(Friendship.class)
        .filter("state", FriendshipState.ACCEPTED)
        .filter("userIds", user.uuid)
        .keys()
        .list();
    return Iterables.transform(keys, k -> UuidPair.parse(k.getName()).otherThan(user.uuid));
  }

  /** Gets users who have requested friendship with the given user. */
  public static Iterable<User> requestsTo(User user) {
    return requests(user, "requestee");
  }

  /** Gets friendship incoming requests for the given user. */
  public static List<Friendship> rawRequestsTo(User user) {
    return ofy().load().type(Friendship.class)
        .filter("state", FriendshipState.REQUESTED)
        .filter("requestee", user)
        .list();
  }

  /** Gets all pending requests. */
  public static List<Friendship> allRequests() {
    return ofy().load().type(Friendship.class)
        .filter("state", FriendshipState.REQUESTED)
        .list();
  }

  /** Gets users from whom the given user has requested friendships. */
  public static Iterable<User> requestsFrom(User user) {
    return requests(user, "requestor");
  }

  private static Iterable<User> requests(User user, String role) {
    List<Key<Friendship>> friendshipKeys = ofy().load().type(Friendship.class)
        .filter("state", FriendshipState.REQUESTED)
        .filter(role, user.getKey())
        .keys()
        .list();
    Iterable<String> requestorIds = Iterables.transform(friendshipKeys,
        k -> UuidPair.parse(k.getName()).otherThan(user.uuid));
    return ofy().load().type(User.class).ids(requestorIds).values();
  }

  /** Creates a copy of a request to the given user. */
  public Friendship copyRequestTo(User requestee) {
    Friendship copy = new Friendship();
    Preconditions.checkState(state == FriendshipState.REQUESTED);
    UuidPair id = uuidPair(requestor.get(), requestee);
    copy.id = id.toString();
    copy.userIds = id.toSet();
    copy.requestor = this.requestor;
    copy.requestee = requestee.getRef();
    copy.state = state;
    return copy;
  }

  @Override protected Friendship getThis() {
    return this;
  }

  /*
   * Code to map Facebook friendships to Present friendships
   */

  /** Turns Facebook friendships into Present friendships. */
  public static void addFor(User current, Collection<FacebookFriendship> facebookFriendships) {
    if (facebookFriendships.isEmpty()) return;

    // Note: We don't currently overwrite Friendships in REQUESTED state as this would require us
    // to update User.incomingFriendRequests in a transaction.
    List<Friendship> friendships = facebookFriendships.stream()
        .map(FacebookFriendship::key)
        .map(Friendship::map)
        .collect(Collectors.toList());
    Map<Key<Friendship>, Friendship> newFriendships = MoreObjectify.saveIfAbsent(friendships);

    // Notify other users.
    if (newFriendships.isEmpty()) return;
    List<Key<User>> others = newFriendships.values().stream()
        .map(f -> f.otherThan(current))
        .collect(Collectors.toList());
    Notifier notifier = Notifier.from(current).toKeys(others);
    String message = "Your friend " + current.firstName + " joined";
    notifier.send(new Notification()
        .body(message)
        .put(current)
        .event(new Event(ActivityType.FRIEND_JOINED_PRESENT, message, current.getRef())));
  }

  /** Maps a Facebook friendship to a Present friendship. */
  private static Friendship map(Key<FacebookFriendship> key) {
    // Note: There is no requestor/requestee in this case.
    Friendship friendship = new Friendship();
    UuidPair id = UuidPair.parse(key.getName());
    friendship.id = id.toString();
    friendship.userIds = id.toSet();
    friendship.state = FriendshipState.ACCEPTED;
    return friendship;
  }

  private Key<User> otherThan(User user) {
    UuidPair id = UuidPair.parse(this.id);
    return Key.create(User.class, id.otherThan(user.uuid));
  }

  public static void main(String[] args) {
    // Create Present friendships for all existing Facebook friendships.
    against(PRODUCTION_SERVER, () -> {
      List<Friendship> friendships = ofy().load().type(FacebookFriendship.class)
          .keys()
          .list()
          .stream()
          .map(Friendship::map)
          .collect(Collectors.toList());
      System.out.println("Found " + friendships.size() + " friendships.");
      Map<Key<Friendship>, Friendship> result = MoreObjectify.saveIfAbsent(friendships);
      System.out.println("Created " + result.size() + " new friendships.");
    });
  }
}