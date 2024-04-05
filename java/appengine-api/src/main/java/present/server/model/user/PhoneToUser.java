package present.server.model.user;

import com.googlecode.objectify.Key;
import com.googlecode.objectify.Ref;
import com.googlecode.objectify.annotation.Cache;
import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Id;
import com.googlecode.objectify.annotation.Load;
import present.phone.PhoneNumbers;
import present.server.KeysOnly;

/**
 * Enables efficient lookup of users by phone.
 *
 * @author Bob Lee (bob@present.co)
 */
@Entity @Cache public class PhoneToUser {

  /** Phone number in E.164 format. */
  @Id public String phoneNumber;

  @Load(unless = KeysOnly.class) public Ref<User> user;

  public static Key<PhoneToUser> key(String phoneNumber) {
    return Key.create(PhoneToUser.class, PhoneNumbers.validateUsPhone(phoneNumber));
  }
}
