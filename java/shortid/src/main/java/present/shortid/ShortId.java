package present.shortid;

import com.googlecode.objectify.ObjectifyService;
import java.util.concurrent.ThreadLocalRandom;

import static com.google.common.base.Preconditions.checkArgument;
import static com.googlecode.objectify.ObjectifyService.ofy;

/**
 * Generates short IDs.
 *
 * @author Bob Lee (bob@present.co)
 */
public class ShortId {

  static {
    ObjectifyService.register(ShortIdCounter.class);
  }

  static final int PARTITION_BITS = 10;
  static final int PARTITIONS = 1 << PARTITION_BITS; // 1024

  private ShortId() {}

  /**
   * Generates a new, positive (> 0), never-been-used short ID for the given type.
   */
  public static long newIdFor(Class<?> type) {
    return newIdFor(type, ThreadLocalRandom.current().nextInt(PARTITIONS));
  }

  static long newIdFor(Class<?> type, int partitionIndex) {
    checkArgument(partitionIndex >= 0);
    checkArgument(partitionIndex < PARTITIONS);
    return ofy().transact(() -> {
      String counterId = type.getSimpleName() + ":" + partitionIndex;
      ShortIdCounter counter = ofy().load().type(ShortIdCounter.class).id(counterId).now();
      if (counter == null) {
        counter = new ShortIdCounter();
        counter.id = counterId;
      }
      counter.value++;
      ofy().save().entity(counter);
      return counter.value << PARTITION_BITS | partitionIndex;
    });
  }
}
