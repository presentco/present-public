package present.web;

import java.util.UUID;
import javax.ws.rs.FormParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.googlecode.objectify.ObjectifyService.ofy;

/**
 * @author Bob Lee (bob@present.co)
 */
@Path("/requestInvitation")
public class RequestInvitation {

  @POST public Response requestInvitation(
      @FormParam("firstName") String firstName,
      @FormParam("lastName") String lastName,
      @FormParam("email") String email,
      @FormParam("zip") String zip) {
    InvitationRequest ir = new InvitationRequest();
    ir.id = UUID.randomUUID().toString();
    ir.firstName = checkNotNull(firstName);
    ir.lastName = checkNotNull(lastName);
    ir.zip = checkNotNull(zip);
    ir.email = checkNotNull(email);
    ofy().save().entity(ir);
    return Response.ok().build();
  }
}
