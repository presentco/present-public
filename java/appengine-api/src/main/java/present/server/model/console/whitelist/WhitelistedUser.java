package present.server.model.console.whitelist;

import com.googlecode.objectify.Key;
import com.googlecode.objectify.annotation.Cache;
import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Id;
import com.googlecode.objectify.annotation.Index;
import com.googlecode.objectify.cmd.Query;
import java.util.List;
import javax.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import present.phone.PhoneNumbers;
import present.server.Uuids;
import present.server.model.BasePresentEntity;

import static com.googlecode.objectify.ObjectifyService.ofy;

@Entity @Cache public class WhitelistedUser extends BasePresentEntity<WhitelistedUser> {
  private static final Logger logger = LoggerFactory.getLogger(WhitelistedUser.class);

  public enum Fields { phoneNumber, email }

  @Id public String uuid = Uuids.newUuid();

  @Index public String phoneNumber;
  @Index public String email;

  // True if the user is approved to use the app, else the user is in a pending status.
  public boolean whitelisted = true;

  public String firstName;
  public String lastName;

  public WhitelistedUser(String phoneNumber, String email, String firstName, String lastName, boolean whitelisted) {
    this.phoneNumber = phoneNumber;
    this.email = email;
    this.firstName = firstName;
    this.lastName = lastName;
    this.whitelisted = whitelisted;
  }

  public WhitelistedUser() {}

  public static Key<WhitelistedUser> keyFor(String uuid) {
    return Key.create(WhitelistedUser.class, uuid);
  }
  public static @Nullable WhitelistedUser get(@Nullable String uuid) {
    if (uuid == null) {
      return null;
    }
    return get(keyFor(uuid));
  }
  public static WhitelistedUser get(Key<WhitelistedUser> userKey) {
    return ofy().load().key(userKey).now();
  }

  public static Key<WhitelistedUser> key(String phoneNumber) {
    PhoneNumbers.validateUsPhone(phoneNumber);
    return Key.create(WhitelistedUser.class, phoneNumber);
  }

  public static Query<WhitelistedUser> query() {
    return ofy().load().type(WhitelistedUser.class);
  }

  public static @Nullable WhitelistedUser findByPhone(String phone) {
    return findBy(Fields.phoneNumber, phone);
  }
  public static @Nullable WhitelistedUser findByEmail(String email) {
    return findBy(Fields.email, email);
  }
  private static @Nullable WhitelistedUser findBy(Fields field, String value) {
    List<WhitelistedUser> list = query().filter(field.name(), value).list();
    if (list.size() > 1) {
      logger.error("Multiple whitelist users found with same: "+value);
    }
    return list.isEmpty() ? null : list.get(0);
  }

  @Override protected WhitelistedUser getThis() {
    return this;
  }
}
