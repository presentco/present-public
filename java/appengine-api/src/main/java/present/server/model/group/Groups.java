package present.server.model.group;

import com.google.common.collect.Iterables;
import com.google.common.collect.Streams;
import com.googlecode.objectify.Key;
import com.googlecode.objectify.Result;
import com.googlecode.objectify.cmd.Query;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import present.server.Keys;
import present.server.ShortLinks;
import present.server.model.PresentEntities;
import present.server.model.comment.GroupView;
import present.server.model.user.User;
import present.wire.rpc.core.ServerException;

import static com.googlecode.objectify.ObjectifyService.ofy;

/**
 * Group utilities
 *
 * @author Pat Niemeyer (pat@pat.net
 */
public class Groups {

  private static final Logger logger = LoggerFactory.getLogger(Groups.class);

  public static Query<Group> query() { return Group.query(); }
  public static Query<Group> active() { return PresentEntities.active(query()); }
  public static List<Group> all() { return query().list(); }
  public static Stream<Group> stream() { return Streams.stream(all()); }

  /**
   * Find groups by exact title.
   * Note: this is intended for testing.
   */
  public static Iterable<Group> findByTitle(String title) {
    return Group.query().filter("title", title).iterable();
  }

  /**
   * For testing: Find one expected group by title or throw an exception.
   */
  public static Group findOneByTitle(String title) {
    return Iterables.getOnlyElement(findByTitle(title));
  }

  /**
   * Get the group by id. If the group is deleted the result will be null.
   */
  public static Group findActiveByUuid(String uuid) {
    return PresentEntities.active(findByUuid(uuid));
  }

  public static @Nullable Group findByUuid(String uuid) {
    Group group = asyncFindByUuid(uuid).now();
    //if (group == null) throw new ClientException("Not found");
    return group;
  }

  public static Result<Group> asyncFindByUuid(String uuid) {
    if (uuid.contains(":")) {
      throw new ServerException("Looks like this group uuid is a group id with lat/long");
    }

    // Try to load the group directly first.
    Key<Group> key = Key.create(Group.class, uuid);
    Result<Group> loadResult = ofy().load().key(key);

    // Fallback query for legacy groups where ID = "uuid:lat,lon".
    Result<Group> queryResult = Group.query().filterKey(">=", key).limit(1).first();

    return () -> {
      Group group = loadResult.now();
      if (group != null) return group;

      group = queryResult.now();
      // If the UUID doesn't match, we matched the next key.
      if (group == null || !group.uuid().equals(uuid)) return null;

      return group;
    };
  }

  /** Asynchrously load muted groups. */
  public static Iterable<String> getMutedGroups(@Nullable User user) {
    if (user == null) return Collections.emptySet();

    // Get all muted groups for the user
    List<GroupView> muted = GroupView.query()
        .ancestor(user).filter(GroupView.Fields.muted.name(), true).list();
    Iterable<GroupView> mutedGroups = Iterables.filter(muted,
        v -> Keys.typeEquals(v.container, Group.class));
    return Iterables.transform(mutedGroups, v -> v.uuid);
  }

  /** Extracts the UUID from a group ID (which may contain legacy location information). */
  public static String getUuidFromId(String id) {
    // Strip latitude and longitude out of legacy IDs.
    int colon = id.indexOf(':');
    return colon == -1 ? id : id.substring(0, colon);
  }

  /** Retrieves the group at the given URL. */
  public static Group fromUrl(String url) {
    return findByUuid(ShortLinks.resolve(url).group.uuid);
  }
}
