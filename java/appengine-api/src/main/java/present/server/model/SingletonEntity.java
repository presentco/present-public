package present.server.model;

import com.googlecode.objectify.Key;
import com.googlecode.objectify.Ref;
import com.googlecode.objectify.annotation.Id;
import com.googlecode.objectify.annotation.Parent;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.googlecode.objectify.ObjectifyService.ofy;

/**
 * Supports singleton child entities.
 *
 * @author Pat Niemeyer (pat@pat.net)
 */
public abstract class SingletonEntity<T extends PresentEntity<T>> extends BasePresentEntity<T> {

  /** We have only one instance per user, so the ID is always the same. */
  public static final long ONLY_ID = 1;

  @Id public long id = ONLY_ID;

  /** Loads all singleton child entities of the given parents and type. */
  public static <T> Map<Key<T>, T> loadAll(List<? extends Key<?>> parents, Class<T> type) {
    List<Key<T>> keys = parents.stream().map(parent ->
        keyFor(parent, type)).collect(Collectors.toList());
    return ofy().load().keys(keys);
  }

  /** Returns a key for a singleton entity with the given parent and type. */
  public static <T> Key<T> keyFor(Key<?> parent, Class<T> type) {
    return Key.create(parent, type, ONLY_ID);
  }
}
