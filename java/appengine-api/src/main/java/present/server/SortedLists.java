package present.server;

import com.google.common.collect.ImmutableList;
import java.util.Collections;
import java.util.List;

/**
 * Utilities to operate on sorted lists.
 *
 * @author Bob Lee (bob@present.co)
 */
public class SortedLists {

  /** Returns a new list with one {@code t} removed, or null if no {@code t} is found. */
  public static <T extends Comparable<T>> List<T> remove(List<T> sorted, T t) {
    int index = Collections.binarySearch(sorted, t);
    if (index < 0) return sorted;
    return new ImmutableList.Builder<T>()
        .addAll(sorted.subList(0, index))
        .addAll(sorted.subList(index + 1, sorted.size()))
        .build();
  }

  /** Returns a new list with {@code t} added, or null if {@code t} is already present. */
  public static <T extends Comparable<T>> List<T> add(List<T> sorted, T t) {
    int result = Collections.binarySearch(sorted, t);
    if (result >= 0) return sorted;
    int insertionPoint = ~result;
    return new ImmutableList.Builder<T>()
        .addAll(sorted.subList(0, insertionPoint))
        .add(t)
        .addAll(sorted.subList(insertionPoint, sorted.size()))
        .build();
  }

  /** Returns true if the list contains {@code t}. */
  public static <T extends Comparable<T>> boolean contains(List<T> sorted, T t) {
    int index = Collections.binarySearch(sorted, t);
    return index >= 0;
  }
}
