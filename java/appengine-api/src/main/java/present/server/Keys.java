package present.server;

import com.google.common.base.Function;
import com.google.common.collect.Iterables;
import com.googlecode.objectify.Key;
import com.googlecode.objectify.Ref;
import present.server.model.PresentEntity;
import present.server.model.group.Group;
import present.server.model.group.Groups;

/**
 * @author Pat Niemeyer pat@pat.net
 */
public class Keys {

  public static class KeyToParentKey<T,P> implements Function<Key<T>, Key<P>> {
    @Override public Key<P> apply( Key<T> tKey ) {
      return tKey.getParent();
    }
  }

  public static class KeyToRef<T> implements Function<Key<T>, Ref<T>> {
    @Override public Ref<T> apply( Key<T> tKey ) {
      return Ref.create(tKey);
    }
  }

  public static <T> Iterable<Ref<T>> toRefs(Iterable<Key<T>> keys) {
    return Iterables.transform(keys, new KeyToRef<T>());
  }

  public static boolean typeEquals(Key key, Class type) {
    return key.getKind().equals(type.getSimpleName());
  }
  public static boolean typeEquals(Ref ref, Class clas) {
    return typeEquals(ref.getKey(), clas);
  }

  /**
   * Extract a uuid for the entity from its key.
   * Note that in most cases the key name is the uuid, but where there are exceptions we
   * will handle them here.
   */
  public static String getUuid(Key<?> key) {
    if (typeEquals(key, Group.class)) {
      return Groups.getUuidFromId(key.getName());
    }
    return key.getName();
  }
}
