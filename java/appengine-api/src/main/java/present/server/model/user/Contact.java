package present.server.model.user;

import com.googlecode.objectify.Ref;
import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Id;
import com.googlecode.objectify.annotation.Index;
import com.googlecode.objectify.annotation.Parent;
import java.util.List;
import present.phone.PhoneNumbers;
import present.proto.ContactRequest;
import present.server.model.BasePresentEntity;
import present.server.model.SingletonEntity;
import present.server.model.content.Content;

/**
 * An address book contact.
 *
 * @author Bob Lee (bob@present.co)
 */
@Entity public class Contact extends BasePresentEntity<Contact> {

  @Parent public Ref<User> owner;

  /** Same as phoneNumber. */
  @Id public String id;

  /** Used to query contacts by phone across users. */
  @Index public String phoneNumber;

  public String fullName;
  public String firstName;
  public String lastName;

  public static Contact from(User owner, ContactRequest request) {
    Contact contact = new Contact();
    contact.owner = owner.getRef();
    contact.id = request.phoneNumber;
    contact.phoneNumber = PhoneNumbers.validateE164(request.phoneNumber);
    contact.fullName = request.fullName;
    contact.firstName = request.firstName;
    contact.lastName = request.lastName;
    return contact;
  }

  @Override protected Contact getThis() {
    return this;
  }
}
