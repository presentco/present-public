package present.server.model.user;

import com.google.common.hash.BloomFilter;
import com.google.common.hash.Funnel;
import com.google.common.hash.PrimitiveSink;
import com.googlecode.objectify.Ref;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import javax.annotation.Nullable;

import static com.googlecode.objectify.ObjectifyService.ofy;

/**
 * @author pat@pat.net
 */
public class BlockedUsersFilter
{
  /** The acceptable false positive rate for the inverse users bloom filter test. */
  private static final double INVERSE_USERS_FALSE_POSITIVE_RATE = 1e-9;

  /** A reasonable number of users to expected may block a given user. */
  private static final int INVERSE_USERS_EXPECTED_COUNT = 10;

  private enum UserRefFunnel implements Funnel<Ref<User>> {
    INSTANCE;
    public void funnel(Ref<User> userRef, PrimitiveSink into) {
      into.putUnencodedChars(userRef.getKey().getName()); // Store the user id
    }
  }

  public static BloomFilter<Ref<User>> create() {
    // 56 bytes
    return BloomFilter.create(UserRefFunnel.INSTANCE,
        INVERSE_USERS_EXPECTED_COUNT, INVERSE_USERS_FALSE_POSITIVE_RATE);
  }

  public static BloomFilter<Ref<User>> rebuild(User user) {
    BloomFilter<Ref<User>> inverseUsers = BlockedUsersFilter.create();
    Iterable<Ref<User>> usersBlockingUser = Users.findUsersBlockingUser(user);
    for (Ref<User> userRef : usersBlockingUser) {
      inverseUsers.put(userRef);
    }
    return inverseUsers;
  }

  /**
   * @return the filter or null if there are no users to block.
   */
  public static @Nullable BloomFilter<Ref<User>> getFilterFor(User user) {
    BlockedUsers blockedUsers = BlockedUsers.loadFor(user);
    return blockedUsers == null ? null : blockedUsers.inverseUsers;
  }
}
