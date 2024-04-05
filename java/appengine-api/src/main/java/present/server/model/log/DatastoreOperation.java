package present.server.model.log;

import com.google.appengine.api.datastore.EmbeddedEntity;
import com.google.appengine.api.datastore.PropertyContainer;
import com.googlecode.objectify.Key;
import com.googlecode.objectify.ObjectifyFactory;
import com.googlecode.objectify.Ref;
import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Id;
import com.googlecode.objectify.annotation.Index;
import com.googlecode.objectify.impl.Path;
import com.googlecode.objectify.impl.translate.SaveContext;
import com.googlecode.objectify.impl.translate.Translator;
import java.lang.reflect.Field;
import present.server.model.group.Group;
import present.server.model.user.User;
import present.server.model.user.Users;

import static com.googlecode.objectify.ObjectifyService.ofy;

/**
 * Log of a datastore operation
 *
 * @author Bob Lee (bob@present.co)
 */
@Entity public class DatastoreOperation {

  @Id public Long id; // set automatically
  @Index public Ref<User> user;
  @Index public long timestamp;
  @Index public Key<?> key;
  public EmbeddedEntity entity;

  private DatastoreOperation(Ref<User> user, long timestamp, Key<?> key, EmbeddedEntity entity) {
    this.user = user;
    this.timestamp = timestamp;
    this.key = key;
    this.entity = entity;
  }

  /** Used by Objectify. */
  public DatastoreOperation() {}

  /** Saves the given entity to the log. */
  public static <T> void log(T entity) {
    ObjectifyFactory factory = ofy().factory();
    @SuppressWarnings("unchecked")
    Translator<T, PropertyContainer> root =
        factory.getTranslators().getRoot((Class<T>) entity.getClass());
    PropertyContainer properties = root.save(entity, false, new SaveContext(), Path.root());
    EmbeddedEntity ee = new EmbeddedEntity();
    ee.setPropertiesFrom(properties);
    ofy().save().entity(new DatastoreOperation(
        Users.current(false).getRef(),
        System.currentTimeMillis(),
        Key.create(entity),
        ee
    ));
  }

  // Fields that may be used in filter queries as strings
  public static class Fields {

    private Fields() {}

    public static Field id = get("id");
    public static Field user = get("user");
    public static Field timestamp = get("timestamp");
    public static Field key = get("key");
    public static Field entity = get("entity");

    private static Field get(String fieldName) {
      try {
        return DatastoreOperation.class.getDeclaredField(fieldName);
      } catch (NoSuchFieldException e) {
        throw new RuntimeException(e);
      }
    }
  }
}
