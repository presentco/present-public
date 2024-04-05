package present.server.model.group;

import com.googlecode.objectify.Result;
import com.googlecode.objectify.annotation.Load;
import com.googlecode.objectify.cmd.Query;
import java.util.Objects;
import present.server.KeysOnly;
import present.server.model.SingletonEntity;
import present.server.model.user.User;
import com.googlecode.objectify.Key;
import com.googlecode.objectify.Ref;
import com.googlecode.objectify.annotation.Cache;
import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Index;
import com.googlecode.objectify.annotation.Parent;
import java.util.HashSet;
import java.util.Set;

import static com.googlecode.objectify.ObjectifyService.ofy;

/**
 * A user's saved groups. We put the saved list in a standlone entity (instead
 * of on User) so we only store and load them when necessary.
 *
 * @author Bob Lee (bob@present.co)
 */
@Entity(name="SavedGroups") @Cache public class JoinedGroups extends SingletonEntity<JoinedGroups> {

  // Field names that may be used in filter queries as strings
  public enum Fields { groups }

  /** The user who owns this set of saved groups. */
  @Load(unless = KeysOnly.class) @Parent public Ref<User> user;

  /** Set of saved groups. */
  @Load(unless = KeysOnly.class) @Index public Set<Ref<Group>> groups = new HashSet<>();

  public JoinedGroups() {}

  public JoinedGroups(User user) {
    this.user = user.getRef();
  }

  public static JoinedGroups getOrCreate(User user) {
    Key<JoinedGroups> key = SingletonEntity.keyFor(user.getKey(), JoinedGroups.class);
    JoinedGroups joinedGroups = ofy().load().key(key).now();
    if (joinedGroups == null) {
      joinedGroups = new JoinedGroups();
      joinedGroups.user = user.getRef();
    }
    return joinedGroups;
  }

  public int count() {
    return (int) this.groups.stream()
        .map(Ref::get)
        .filter(Objects::nonNull)
        .filter(g -> !g.isDeleted())
        .count();
  }

  @Override public String toString() {
    return "SavedGroups{" + "user=" + user + ", groups=" + groups + '}';
  }

  public static Query<JoinedGroups> query() {
    return ofy().load().type(JoinedGroups.class);
  }

  @Override protected JoinedGroups getThis() {
    return this;
  }
}
