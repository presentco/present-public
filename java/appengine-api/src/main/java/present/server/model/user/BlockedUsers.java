package present.server.model.user;

import com.google.common.hash.BloomFilter;
import com.googlecode.objectify.Key;
import com.googlecode.objectify.Ref;
import com.googlecode.objectify.annotation.Cache;
import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Id;
import com.googlecode.objectify.annotation.Index;
import com.googlecode.objectify.annotation.Parent;
import com.googlecode.objectify.annotation.Serialize;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import present.server.model.SingletonEntity;

import static com.googlecode.objectify.ObjectifyService.ofy;

/**
 * A user's blocked user set. Similar to SavedGroups, we put the list in a standalone entity
 * so that we only load these when needed.
 *
 * @author Pat Niemeyer (pat@pat.net)
 */
@Entity @Cache public class BlockedUsers extends SingletonEntity<BlockedUsers> {

  /** The user owning this set of blocked users */
  @Parent public Ref<User> user;

  /** Set of users this user is blocking. */
  @Index public Set<Ref<User>> users = new HashSet<>();

  /** A bloom filter for testing if a given user is blocking this user. */
  @Serialize public BloomFilter<Ref<User>> inverseUsers = BlockedUsersFilter.create();

  public BlockedUsers() { }

  public BlockedUsers(Ref<User> parentUser) {
    this.user = parentUser;
  }

  /** Returns true if this user is blocked by the given user. */
  public boolean blockedBy(Ref<User> other) {
    return inverseUsers.mightContain(other);
  }

  /** Returns true if this user is blocked by the given user. */
  public boolean blockedBy(Key<User> other) {
    return inverseUsers.mightContain(Ref.create(other));
  }

  /** Rebuild the inverse users bloom filter for this user */
  public void rebuildInverseUsers() {
    this.inverseUsers = BlockedUsersFilter.rebuild(user.get());
  }

  public static BlockedUsers loadFor(User user) {
    Key<BlockedUsers> key = Key.create(user.getKey(), BlockedUsers.class, ONLY_ID);
    return ofy().load().key(key).now();
  }

  @Override protected BlockedUsers getThis() {
    return this;
  }
}



