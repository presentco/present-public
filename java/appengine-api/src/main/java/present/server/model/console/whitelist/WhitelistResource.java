package present.server.model.console.whitelist;

import com.google.gson.Gson;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.googlecode.objectify.ObjectifyService.ofy;

/**
 * @author pat@present.co
 */
@Path("/whitelist")
public class WhitelistResource {
  private static final Logger logger = LoggerFactory.getLogger(WhitelistResource.class);
  private static final Gson gson = new Gson();

  @POST
  @Path("/delete")
  public Response delete(
      @FormParam("email") String email,
      @QueryParam("auth") String auth
  ) {
    if (!authValid(auth)) {
      return Response.status(Response.Status.FORBIDDEN).build();
    }

    WhitelistedUser wu = WhitelistedUser.findByEmail(email);
    if (wu != null) {
      logger.info("Deleted Whitelist User: " + email);
      ofy().delete().entity(wu).now();
    } else {
      logger.info("Deleted: Ignoring whitelist User not found: " + email);
    }
    return Response.ok().build();
  }

  @POST
  @Produces("application/json; charset=UTF-8")
  @Path("/create")
  public Response create(
      @FormParam("email") String email,
      @FormParam("firstName") String firstName,
      @FormParam("lastName") String lastName,
      @QueryParam("auth") String auth
  ) throws UnsupportedEncodingException {
    if (!authValid(auth)) {
      return Response.status(Response.Status.FORBIDDEN).build();
    }

    logger.info("Create Whitelist User: {}", email);

    if (Whitelist.userIsWhitelisted(null, email)) {
      logger.warn("User already exists: " + email);
      return Response.status(409).build(); // 409 Conflict
    }

    email = email.trim().toLowerCase();
    WhitelistedUser wu = new WhitelistedUser(null, email, firstName, lastName, true);
    ofy().save().entity(wu).now();
    byte[] json = new WhitelistedUsersJson(new WhitelistedUserJson(wu)).toBytes();
    return Response.ok(json).build();
  }

  @GET
  @Produces("application/json; charset=UTF-8")
  public Response list(@QueryParam("auth") String auth) throws UnsupportedEncodingException {
    if (!authValid(auth)) {
      return Response.status(Response.Status.FORBIDDEN).build();
    }
    byte[] json = new WhitelistedUsersJson(allUsers()).toBytes();
    return Response.ok(json).build();
  }

  private List<WhitelistedUserJson> allUsers() {
    List<WhitelistedUser> whitelist = WhitelistedUser.query().list();
    List<WhitelistedUserJson> jsonList = new ArrayList<>();
    for (WhitelistedUser whitelistUser : whitelist) {
        jsonList.add(new WhitelistedUserJson(whitelistUser));
    }
    return jsonList;
  }

  private static class WhitelistedUsersJson {
    public List<WhitelistedUserJson> users;

    public WhitelistedUsersJson(List<WhitelistedUserJson> users) {
      this.users = users;
    }

    public WhitelistedUsersJson(WhitelistedUserJson user) {
      this.users = Arrays.asList(user);
    }

    public byte[] toBytes() {
      try {
        return gson.toJson(this).getBytes("UTF-8");
      } catch (UnsupportedEncodingException e) {
        throw new RuntimeException(e);
      }
    }
  }

  private static class WhitelistedUserJson {
    public String firstName;
    public String lastName;
    public long created;
    public String email;

    public WhitelistedUserJson(WhitelistedUser wu) {
      this.firstName = wu.firstName;
      this.lastName = wu.lastName;
      this.email = wu.email;
      this.created = wu.createdTime;
    }
  }

  private boolean authValid(String authIn) {
    // Currently same auth code as for cron and notification tester.
    final String auth = "xxx";
    return auth.equalsIgnoreCase(authIn);
  }
}
