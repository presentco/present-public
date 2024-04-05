package present.server.model.console.whitelist.geofence;

import com.google.gson.Gson;
import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import present.server.model.util.Coordinates;
import present.server.model.console.whitelist.Whitelist;
import present.server.model.console.whitelist.WhitelistedUser;

/**
 * @author pat@present.co
 */
@Path("/whitelist-geofence")
public class WhitelistGeofenceService {
  private static final Logger logger = LoggerFactory.getLogger(WhitelistGeofenceService.class);
  private static final Gson gson = new Gson();

  @POST
  @Path("/delete")
  public Response delete(
      @FormParam("uuid") String uuid,
      @QueryParam("auth") String auth
  ) {
    if (!authValid(auth)) {
      return Response.status(Response.Status.FORBIDDEN).build();
    }
    WhitelistGeofences.remove(uuid);
      logger.info("Deleted Whitelist geofence: "+uuid);
    return Response.ok().build();
  }

  @POST
  @Produces("application/json; charset=UTF-8")
  @Path("/create")
  public Response create(
      @FormParam("name") String name,
      @FormParam("address") String address,
      @FormParam("latitude") double latitude,
      @FormParam("longitude") double longitude,
      @FormParam("radius") double radius,
      @QueryParam("auth") String auth
  ) throws UnsupportedEncodingException {
    if (!authValid(auth)) {
      return Response.status(Response.Status.FORBIDDEN).build();
    }

    logger.info("Create Whitelist geofence: "+name+", "+address);

    WhitelistGeofence geofence =
      new WhitelistGeofence(name, address, new Coordinates(latitude, longitude), radius, true);
    WhitelistGeofences.add(geofence);
    // Return a WhitelistGeofences with a single entry for the newly created item.
    byte[] json = gson.toJson(new WhitelistGeofences(Arrays.asList(geofence))).getBytes("UTF-8");
    return Response.ok(json).build();
  }

  @GET
  @Produces("application/json; charset=UTF-8")
  public Response list(
      @QueryParam("auth") String auth
  ) throws UnsupportedEncodingException {
    if (!authValid(auth)) {
      return Response.status(Response.Status.FORBIDDEN).build();
    }
    WhitelistGeofences whitelist = WhitelistGeofences.load();
    byte[] json = gson.toJson(whitelist).getBytes("UTF-8");
    return Response.ok(json).build();
  }

  private boolean authValid(String authIn) {
    // Currently same auth code as for cron and notification tester.
    final String auth = "";
    return auth.equalsIgnoreCase(authIn);
  }
}
