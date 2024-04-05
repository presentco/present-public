package present.server;

import com.google.common.base.Function;
import com.google.common.collect.Iterables;
import com.googlecode.objectify.Key;
import com.googlecode.objectify.Ref;

/**
 * @author Pat Niemeyer (pat@pat.net)
 */
public class Refs {

  public static class RefToKey<T> implements Function<Ref<T>, Key<T>> {
    @Override public Key<T> apply( Ref<T> tRef ) {
      return tRef.getKey();
    }
  }

  public static <T> Iterable<Key<T>> toKeys(Iterable<Ref<T>> refs) {
    return Iterables.transform(refs, new RefToKey<T>());
  }
}
