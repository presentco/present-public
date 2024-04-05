package present.server.model.console.users;

import com.google.common.collect.ImmutableList;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;
import present.proto.Gender;
import present.server.model.Space;
import present.server.model.user.VerificationRequest;

@Path("/verify")
public class CreateVerificationRequest {

  private static final Response FORBIDDEN = Response.status(Response.Status.FORBIDDEN).build();

  @GET
  @Path("/woman")
  @Produces("text/plain; charset=UTF-8")
  public Response woman(@QueryParam("auth") String auth) {
    if (!valid(auth)) return FORBIDDEN;
    VerificationRequest vr = VerificationRequest.clientIndependent();
    vr.gender = Gender.WOMAN;
    vr.save();
    return Response.ok(vr.url()).build();
  }

  @GET
  @Path("/insider")
  @Produces("text/plain; charset=UTF-8")
  public Response insider(@QueryParam("auth") String auth) {
    if (!valid(auth)) return FORBIDDEN;
    VerificationRequest vr = VerificationRequest.clientIndependent();
    vr.spaceIds = ImmutableList.of(Space.PRESENT_INSIDERS.id);
    vr.save();
    return Response.ok(vr.url()).build();
  }

  @GET
  @Path("/womanInsider")
  @Produces("text/plain; charset=UTF-8")
  public Response womanInsider(@QueryParam("auth") String auth) {
    if (!valid(auth)) return FORBIDDEN;
    VerificationRequest vr = VerificationRequest.clientIndependent();
    vr.gender = Gender.WOMAN;
    vr.spaceIds = ImmutableList.of(Space.PRESENT_INSIDERS.id);
    vr.save();
    return Response.ok(vr.url()).build();
  }

  private static boolean valid(String auth) {
    return "xxx".equals(auth);
  }
}
