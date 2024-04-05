package present.server.model.user;

import com.google.appengine.api.memcache.MemcacheService;
import com.google.appengine.api.memcache.MemcacheServiceFactory;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import com.google.common.collect.Sets;
import com.google.common.collect.Streams;
import com.googlecode.objectify.Key;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import present.proto.ComputeUnreadCountsRequest;
import present.proto.UnreadCounts;
import present.proto.UnreadState;
import present.proto.UserService;
import present.server.RpcQueue;
import present.server.SortedLists;
import present.server.model.comment.GroupView;
import present.server.model.comment.GroupViews;
import present.server.model.group.Group;

import static com.googlecode.objectify.ObjectifyService.ofy;
import static present.server.model.user.UnreadStates.UnreadStateField.GROUPS;

/**
 * Manages users' {@code UnreadState}s.
 *
 * @author Bob Lee (bob@present.co)
 */
public class UnreadStates {

  // TODO: Simplify this now that we don't have unread Chats.

  private static final Logger logger = LoggerFactory.getLogger(UnreadStates.class);

  private final MemcacheService memcache = MemcacheServiceFactory.getMemcacheService();
  private final Map<User, UnreadStateUpdater> states = new HashMap<>();
  private final Set<User> nonCachedUsers = new HashSet<>();

  @VisibleForTesting UnreadStates(Iterable<User> users) {
    Set<String> cacheKeys = Streams.stream(users).map(UnreadStates::toCacheKey).collect(Collectors.toSet());
    Map<String, MemcacheService.IdentifiableValue> cache = memcache.getIdentifiables(cacheKeys);
    int total = 0;
    int cached = 0;
    for (User user : users) {
      total++;
      String cacheKey = toCacheKey(user);
      MemcacheService.IdentifiableValue value = cache.get(cacheKey);
      if (value == null) {
        nonCachedUsers.add(user);
      } else {
        try {
          states.put(user, new UnreadStateUpdater(cacheKey, value,
              UnreadState.ADAPTER.decode((byte[]) value.getValue())));
          cached++;
        } catch (IOException e) {
          logger.error("Error decoding UnreadState for " + user + ".", e);
          nonCachedUsers.add(user);
        }
      }
    }
    logger.info("{} of {} users had cached UnreadState.", cached, total);
  }

  /** Batch loads cached UnreadThreads for the given users. */
  public static UnreadStates loadFor(Iterable<User> users) {
    return new UnreadStates(users);
  }

  /** Returns the set of users not found in the cache. */
  public Set<User> nonCachedUsers() {
    return Collections.unmodifiableSet(nonCachedUsers);
  }

  /** Marks a group as unread. */
  public Stats markGroupUnread(String groupId) {
    states.values().forEach(state -> state.markUnread(GROUPS, groupId));
    return updateCache();
  }

  private static UserService asyncUserService = RpcQueue.create(UserService.class);

  /**
   * Asynchronously re-computes unread counts for the given user and pushes them to the user's
   * devices.
   */
  public static void recomputeUnreadCountsFor(User user) {
    try {
      asyncUserService.computeUnreadCounts(
          new ComputeUnreadCountsRequest(user.uuid, user.unreadVersion));
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Synchronously updates memcache. Enqueues tasks to re-compute missing or conflicted values.
   */
  private Stats updateCache() {
    Map<String, MemcacheService.CasValues> updates = states.values().stream()
        .filter(UnreadStateUpdater::dirty)
        .collect(Collectors.toMap(UnreadStateUpdater::cacheKey, UnreadStateUpdater::casValues));
    Stats stats;
    if (updates.isEmpty()) {
      refresh(Collections.emptySet());
      stats = new Stats(0, states.size(), nonCachedUsers.size(),0);
    } else {
      Set<String> updatedKeys = memcache.putIfUntouched(updates);
      Set<String> failedKeys = Sets.difference(updates.keySet(), updatedKeys);
      refresh(failedKeys);
      stats = new Stats(
          updatedKeys.size(),
          states.size() - updates.size(),
          nonCachedUsers.size(),
          failedKeys.size()
      );
    }
    stats.log();
    return stats;
  }

  /** Enqueues tasks to recompute the user's unread count. */
  void refresh(Set<String> failedCacheKeys) {
    RpcQueue.batch(() -> {
      List<Key<User>> failedUserKeys = failedCacheKeys.stream()
          .map(UnreadStates::parseUserId)
          .map(id -> Key.create(User.class, id))
          .collect(Collectors.toList());
      ofy().load().keys(failedUserKeys).values().forEach(UnreadStates::recomputeUnreadCountsFor);

      // Refresh users that weren't cached in the first place.
      nonCachedUsers.forEach(UnreadStates::recomputeUnreadCountsFor);
    });
  }

  static String toCacheKey(User user) {
    return UnreadState.class.getSimpleName() + ":" + Preconditions.checkNotNull(user.uuid);
  }

  private static String parseUserId(String cacheKey) {
    int index = cacheKey.indexOf(':');
    return cacheKey.substring(index + 1);
  }

  /** Returns unread counts for the given user or null if they weren't cached. */
  public UnreadCounts countsFor(User user) {
    UnreadState state = stateFor(user);
    if (state == null) return null;
    return toCounts(user, state);
  }

  /** Returns unread state for the given user or null if it wasn't cached. */
  public UnreadState stateFor(User user) {
    UnreadStateUpdater updater = states.get(user);
    if (updater == null) return null;
    if (updater.updated != null) return updater.updated;
    return updater.original;
  }

  private static class UnreadStateUpdater {

    private final String cacheKey;
    private final MemcacheService.IdentifiableValue encoded;
    private final UnreadState original;
    private UnreadState updated;

    private UnreadStateUpdater(String cacheKey, MemcacheService.IdentifiableValue encoded,
        UnreadState decoded) {
      this.cacheKey = cacheKey;
      this.encoded = encoded;
      this.original = decoded;
    }

    private String cacheKey() {
      return this.cacheKey;
    }

    private MemcacheService.CasValues casValues() {
      Preconditions.checkState(dirty());
      return new MemcacheService.CasValues(encoded, updated.encode());
    }

    private void markUnread(UnreadStateField field, String id) {
      checkNotUpdated();
      List<String> original = field.get(this.original);
      List<String> updated = SortedLists.add(original, id);
      this.updated = original == updated ? this.original : field.update(this.original, updated);
    }

    private void checkNotUpdated() {
      Preconditions.checkState(updated == null, "Already updated");
    }

    private boolean dirty() {
      checkUpdated();
      return original != updated;
    }

    private void checkUpdated() {
      Preconditions.checkState(updated != null, "Not updated yet");
    }
  }

  /** Marks the given group read for the current user. */
  public static UnreadState markGroupUnread(User user, String groupId) {
    UnreadStates states = new UnreadStates(Collections.singleton(user));
    states.markGroupUnread(groupId);
    return states.stateFor(user);
  }

  /** Marks the given group read for the current user. */
  public static UnreadState markGroupRead(User user, String groupId) {
    return markRead(user, GROUPS, groupId);
  }

  /**
   * Marks the given group read for the current user.
   */
  private static UnreadState markRead(User user, UnreadStateField field, String id) {
    MemcacheService memcache = MemcacheServiceFactory.getMemcacheService();
    String key = toCacheKey(user);
    try {
      MemcacheService.IdentifiableValue identifiable = memcache.getIdentifiable(key);
      if (identifiable != null) {
        byte[] encoded = (byte[]) identifiable.getValue();
        UnreadState decoded = UnreadState.ADAPTER.decode(encoded);
        List<String> original = field.get(decoded);
        List<String> updatedList = SortedLists.remove(original, id);
        if (updatedList == original) {
          // No change
          return decoded;
        }
        UnreadState updated = field.update(decoded, updatedList);
        encoded = updated.encode();
        if (memcache.putIfUntouched(key, identifiable, encoded)) {
          // Successful update
          return updated;
        } else {
          // Conflict. Fall through.
          logger.info("Conflict while updating UserState. Re-computing UnreadState.");
        }
      }
    } catch (Exception e) {
      logger.error("Error decoding UnreadState", e);
    }

    logger.info("Re-computing UnreadState.");
    // TODO: Do this concurrently with writing the container view.
    // user.incrementUnreadVersion();
    UnreadState computed = computeFor(user);
    memcache.put(key, computed.encode());
    return computed;
  }

  /** Computes UnreadState from scratch for the given user. */
  public static UnreadState computeFor(User user) {
    // Kick off three queries in parallel.
    List<Group> allGroups = user.joinedGroups();

    // Picks unread containers.
    Predicate<? super Group> unread = container -> {
      if (container == null) {
        logger.warn("Null container.");
        return false;
      }
      GroupView view = container.getView(user);
      return view == null || !view.isRead();
    };

    // Load group views into session cache.
    GroupViews.viewsFor(user, allGroups);

    // Sorted list of unread Group IDs
    List<String> unreadGroups = allGroups.stream()
        .filter(unread)
        .filter(g -> !g.isDeleted())
        .map(Group::uuid)
        .sorted()
        .collect(Collectors.toList());

    return new UnreadState(unreadGroups);
  }

  /** Caches unread state for the given user. */
  public static void put(User user, UnreadState state) {
    MemcacheService memcache = MemcacheServiceFactory.getMemcacheService();
    memcache.put(toCacheKey(user), state.encode());
  }

  /** Looks up unread state for the given user. */
  public static UnreadState loadStateFor(User user) {
    MemcacheService memcache = MemcacheServiceFactory.getMemcacheService();
    byte[] encoded = (byte[]) memcache.get(toCacheKey(user));
    try {
      return UnreadState.ADAPTER.decode(encoded);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  /** Looks up unread state for the given user. */
  public static UnreadCounts loadCountsFor(User user) {
    return toCounts(user, loadStateFor(user));
  }

  /** Converts the server's internal unread state into the client's aggregate counts. */
  public static UnreadCounts toCounts(User user, UnreadState state) {
    int unreadGroups = state.groups.size();
    return new UnreadCounts(unreadGroups + user.incomingFriendRequests,
        unreadGroups, user.incomingFriendRequests);
  }

  enum UnreadStateField {

    GROUPS {
      @Override List<String> get(UnreadState state) {
        return state.groups;
      }

      @Override UnreadState update(UnreadState original, List<String> updated) {
        return new UnreadState(updated);
      }
    };

    abstract List<String> get(UnreadState state);
    abstract UnreadState update(UnreadState original, List<String> updated);
  }

  /** Cache operation statistics. */
  public static class Stats {

    /** One update. */
    public static final Stats ONE_UPDATE = new Stats(1, 0, 0, 0);

    /** One skip. */
    public static final Stats ONE_SKIP = new Stats(0, 1, 0, 0);

    /** One miss. */
    public static final Stats ONE_MISS = new Stats(0, 0, 1, 0);

    /** One collision. */
    public static final Stats ONE_COLLISION = new Stats(0, 0, 0, 1);

    /** Number of updated cache entries. */
    public final int updates;

    /** Number of cache entries that remained the same. */
    public final int skips;

    /** Number of entries that were missing from the cache. */
    public final int misses;

    /** Number of updates that collided with concurrent updates. */
    public final int collisions;

    public Stats(int updates, int skips, int misses, int collisions) {
      this.updates = updates;
      this.skips = skips;
      this.misses = misses;
      this.collisions = collisions;
    }

    public void log() {
      logger.info("UnreadStates.Stats:\n"
          + "  {} cached states were updated.\n"
          + "  {} cached states remained the same.\n"
          + "  Re-computing {} states that weren't cached.\n"
          + "  Re-computing {} states that collided with concurrent updates.",
          updates, skips, misses, collisions);
    }
  }
}
