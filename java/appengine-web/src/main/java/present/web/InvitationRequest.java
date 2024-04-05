package present.web;

import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Id;

@Entity public class InvitationRequest {

  @Id public String id;
  public String firstName;
  public String lastName;
  public String email;
  public String zip;
}
