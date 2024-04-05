package cast.placeholder;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;

import static com.googlecode.objectify.ObjectifyService.ofy;

/**
 * @author Bob Lee (bob@present.co)
 */
@Path("")
public class Censor {

  public static final String CENSOR_CAST = "/censorCast";
  public static final String CENSOR_CLIENT = "/censorClient";

  @Path(CENSOR_CAST)
  @GET
  @Produces("text/plain")
  public String censorCast(@QueryParam("id") String id) {
    Cast cast = ofy().load().type(Cast.class).id(id).now();
    cast.censored = true;
    ofy().save().entity(cast);
    return "Cast censored.";
  }

  @Path(CENSOR_CLIENT)
  @GET
  @Produces("text/plain")
  public String censorClient(@QueryParam("id") String id) {
    Client client = ofy().load().type(Client.class).id(id).now();
    client.censored = true;
    ofy().save().entity(client);
    return "Client censored.";
  }
}
