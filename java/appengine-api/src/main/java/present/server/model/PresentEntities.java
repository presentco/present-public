package present.server.model;

import com.google.common.base.Stopwatch;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.googlecode.objectify.Key;
import com.googlecode.objectify.ObjectifyService;
import com.googlecode.objectify.cmd.Query;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import present.server.facebook.FacebookFriendship;
import present.server.facebook.FacebookUserData;
import present.server.model.activity.Event;
import present.server.model.activity.GroupReferral;
import present.server.model.comment.Comment;
import present.server.model.comment.GroupView;
import present.server.model.console.whitelist.WhitelistedUser;
import present.server.model.console.whitelist.geofence.WhitelistGeofences;
import present.server.model.content.Content;
import present.server.model.group.Group;
import present.server.model.group.GroupMembership;
import present.server.model.group.JoinedGroups;
import present.server.model.log.DatastoreOperation;
import present.server.model.user.BlockedUsers;
import present.server.model.user.Client;
import present.server.model.user.Contact;
import present.server.model.user.Friendship;
import present.server.model.user.PhoneToUser;
import present.server.model.user.User;
import present.server.model.user.VerificationRequest;
import present.server.notification.TestNotification;
import present.wire.rpc.core.ServerException;

import static com.googlecode.objectify.ObjectifyService.ofy;

public class PresentEntities {

  private static final Logger logger = LoggerFactory.getLogger(PresentEntities.class);

  // IMPORTANT: When adding new entities to the system we must update the cron-xxx.xml backup
  // IMPORTANT: descriptions to include the new entity in the "kind" list.
  // TODO: Programmatically back up based on this list.
  public static Set<Class<?>> TYPES = ImmutableSet.of(
      BlockedUsers.class,
      Client.class,
      Comment.class,
      GroupView.class,
      Contact.class,
      Content.class,
      DatastoreOperation.class,
      Event.class,
      FacebookFriendship.class,
      FacebookUserData.class,
      Friendship.class,
      Group.class,
      GroupMembership.class,
      GroupReferral.class,
      Group.Log.class,
      JoinedGroups.class,
      PhoneToUser.class,
      TestNotification.class,
      User.class,
      VerificationRequest.class,
      WhitelistGeofences.class,
      WhitelistedUser.class
  );

  private PresentEntities() {}

  public static void registerAll() {
    Stopwatch stopwatch = Stopwatch.createStarted();
    for (Class<?> type : TYPES) ObjectifyService.register(type);
    logger.info("Registered entities in {}.", stopwatch);
  }

  public static <T> void loadRandomSubset( Class<T> typ, int count ) {
    // TODO: We probably shouldn't load every single key.
    List<Key<T>> list = ofy().load().type(typ).keys().list();
    Collections.shuffle( list );
    Map<Key<Key<T>>, Key<T>> entities = ofy().load().entities(list.subList(0, Math.min(list.size()-1, count)));
    //logger.debug("Loaded: " +typ+" - "+entities.size());
  }

  public static <T extends PresentEntity> T expected(@Nullable T t) {
    return expected(t, "");
  }
  public static <T extends PresentEntity> T expected(@Nullable T t, String logMessage) {
    if (t == null) {
      String msg = "Expected entity missing: "+logMessage;
      logger.error(msg);
      throw new ServerException(msg);
    }
    return t;
  }

  public static <T extends PresentEntity<T>> Key<T> expected(Key<T> key) {
    if (key == null) {
      throw new ServerException("Expected entity missing");
    }
    return key;
  }

  /**
   * "Filter" this entity: Returning null if the entity is deleted.
   */
  @Nullable public static <T extends PresentEntity> T active( T t ) {
    return (t == null || t.isDeleted()) ? null : t;
  }

  /**
   * Filter this query to select only non-deleted PresentEntities.
   */
  public static <T extends PresentEntity> Query<T> active(Query<T> query) {
    return query.filter("deleted", false);
  }

  /**
   * "Filter" this key: Return the key or null if it corresponds to a deleted entity.
   */
  @Nullable public static <T extends PresentEntity<T>> Key<T> active(Class<T> type, Key<T> key) {
    return Iterables.getOnlyElement(
        active(type, Collections.singletonList(key)), null);
  }

  // TODO: How efficient is the "in" keys here?
  /**
   * Filter the keys corresponding to the active PresentEntities.
   */
  public static <T extends PresentEntity> Iterable<Key<T>> active(Class<T> type, Iterable<Key<T>> keys) {
    if (Iterables.isEmpty(keys)) { return keys; }
    return active(ofy().load().type(type)).filterKey("in",keys).keys().iterable();
  }
}
