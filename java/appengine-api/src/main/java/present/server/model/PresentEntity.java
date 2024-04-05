package present.server.model;

import com.googlecode.objectify.Key;
import com.googlecode.objectify.Ref;
import com.googlecode.objectify.Result;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * @author Pat Niemeyer (pat@pat.net)
 */
public interface PresentEntity<T extends PresentEntity<T>> {
  /**
   * Save the entity, setting its update time.
   */
  Result<Key<T>> save();

  /**
   * Soft delete the entity, setting the deleted flag and update time.
   */
  Result delete();

  /**
   * Hard or soft delete the entity:
   * Soft deleting the entity sets the deleted flag and update time.
   */
  Result delete(boolean soft);

  long getCreatedTime();

  long getUpdatedTime();

  boolean isDeleted();

  Key<T> getKey();

  Ref<T> getRef();

  /**
   * Reloads this instance from the datastore. Useful for reloading an entity within a
   * transaction.
   */
  T reload();

  /**
   * Return this if not deleted. Throws an exception otherwise.
   */
  T notDeleted();

  /**
   * Reloads this entity from the datastore and updates it in a transaction. Saves the entity if
   * updater returns true. Applies updater to this in-memory copy, too. This copy will
   * not receive other changes from the datastore and may be stale in other ways.
   * 
   * @returns true if the entity was saved
   */
  boolean inTransaction(Updater<T> updater);

  /**
   * Reloads this entity from the datastore and updates it in a transaction. Saves the entity if
   * updater returns true. Applies updater to this in-memory copy, too. This copy will
   * not receive other changes from the datastore and may be stale in other ways.
   */
  void inTransaction(Consumer<T> updater);

  /** Converts an entity to a pretty-printed string. Useful for debugging. */
  String debugString();
}
