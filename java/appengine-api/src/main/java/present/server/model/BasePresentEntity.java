package present.server.model;

import com.google.appengine.api.datastore.PropertyContainer;
import com.google.common.base.Suppliers;
import com.googlecode.objectify.Key;
import com.googlecode.objectify.ObjectifyFactory;
import com.googlecode.objectify.Ref;
import com.googlecode.objectify.Result;
import com.googlecode.objectify.annotation.Ignore;
import com.googlecode.objectify.annotation.Index;
import com.googlecode.objectify.annotation.OnSave;
import com.googlecode.objectify.condition.IfNotZero;
import com.googlecode.objectify.impl.translate.Translator;
import java.lang.reflect.Field;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import present.server.MoreObjectify;
import present.server.environment.Environment;
import present.shortid.ShortId;
import present.wire.rpc.core.ClientException;

import static com.googlecode.objectify.ObjectifyService.ofy;

/**
 * Base entity supporting soft delete and storing create and update times.
 *
 * @author Pat Niemeyer (pat@pat.net)
 * @see PresentEntities active()
 */
public abstract class BasePresentEntity<T extends PresentEntity<T>> implements PresentEntity<T> {

  // Create time: Set when entity instantiated.
  public long createdTime = now();

  // Update time: Set on entity save() or delete().
  public long updatedTime = now();

  // Deleted flag.
  // Note: We could use deleteTime for this but since we are doing this for every entity we want
  // to keep this as light as possible.
  @Index public boolean deleted;

  // If flagged don't reset the update time on save. (Used for admin activity).
  @Ignore public boolean preserveUpdateTime;

  @Index(IfNotZero.class) public long shortId;

  /** Sets the short ID on this instance. */
  public void setShortId() {
    this.shortId = ShortId.newIdFor(getClass());
  }

  /**
   * Save the entity, setting its update time.
   */
  public Result<Key<T>> save() {
    return ofy().save().entity(getThis());
  }

  public Result<Key<T>> savePreservingUpdateTime() {
    this.preserveUpdateTime = true;
    return save();
  }

  @OnSave public void onSave() {
    if (!preserveUpdateTime) {
      this.updatedTime = now();
    }
  }

  /**
   * Soft delete the entity, setting the deleted flag and update time.
   */
  public Result<Void> delete() {
    return delete(true);
  }

  public Result<Void> deleteHard() {
    return delete(false);
  }

  /**
   * Hard or soft delete the entity:
   * Soft deleting the entity sets the deleted flag and update time.
   */
  public Result<Void> delete(boolean soft) {
    if (soft) {
      this.deleted = true;
      Result<Key<T>> saveResult = save();
      return () -> {
        saveResult.now();
        return null;
      };
    } else {
      return ofy().delete().entity(this);
    }
  }

  public long getCreatedTime() { return createdTime; }

  public long getUpdatedTime() { return updatedTime; }

  public boolean isDeleted() { return deleted; }

  public static long now() { return System.currentTimeMillis(); }

  @Ignore private final Supplier<Key<T>> keySupplier
      = Suppliers.memoize(() -> Key.create(getThis()));

  @Override public Key<T> getKey() {
    return keySupplier.get();
  }

  public String getWebSafeKey() {
    return getKey().getString();
  }

  @Override public Ref<T> getRef() {
    return Ref.create(getKey());
  }

  @Override public T reload() {
    return ofy().load().key(getKey()).now();
  }

  protected abstract T getThis();

  @Override public T notDeleted() {
    if (isDeleted()) {
      throw new ClientException(getKey() + " is deleted.");
    }
    return getThis();
  }

  public String consoleUrl() {
    Key<?> key = getKey();
    // Note: Dev local console does not support edit view of entities.
    if (Environment.isDevelopment()) return "https://present.co/";
    String project = Environment.applicationId(); // e.g. "present-production"
    String consoleBaseUrl = "https://console.cloud.google.com/datastore/entities/edit";
    return consoleBaseUrl + "?key=" + key.toWebSafeString() + "&project=" + project + "&kind=" + key.getKind() + "&authuser=1";
  }

  @Override public boolean inTransaction(Updater<T> updater) {
    // Update this copy. Note: This copy will still be stale in other regards.
    updater.update(getThis());
    return ofy().transact(() -> {
      T latest = reload();
      if (latest == null || latest == this) {
        // This object isn't in the datastore yet or it's the same instance (because we're
        // running in a larger transaction).
        save();
        return true;
      } else {
        // Update version in the datastore.
        if (updater.update(latest)) {
          latest.save();
          return true;
        }
      }
      return false;
    });
  }

  public void inTransaction(Consumer<T> updater) {
    inTransaction(t -> {
      updater.accept(t);
      return true;
    });
  }

  @Override public String toString() {
    return debugString();
  }

  @Override public String debugString() {
    return MoreObjectify.toString(this);
  }

  // Fields that may be used in filter queries as strings
  public static class Fields {

    private Fields() {}

    public static Field deleted = get("deleted");
    public static Field shortId = get("shortId");

    private static Field get(String fieldName) {
      try {
        return BasePresentEntity.class.getDeclaredField(fieldName);
      } catch (NoSuchFieldException e) {
        throw new RuntimeException(e);
      }
    }
  }
}
