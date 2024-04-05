package present.server.model.console.users;

import com.google.common.base.Preconditions;
import com.google.common.base.Stopwatch;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import com.google.gson.Gson;
import com.googlecode.objectify.cmd.Query;
import java.io.UnsupportedEncodingException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import javax.annotation.Nullable;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import present.server.AppEngineUserService;
import present.server.model.activity.GroupReferrals;
import present.server.model.user.Client;
import present.server.model.user.Clients;
import present.server.model.user.User;
import present.server.model.user.UserState;
import present.server.model.user.Users;

/**
 * @author pat@present.co
 */
@Path("/users")
public class UsersResource {
  private static final Logger logger = LoggerFactory.getLogger(UsersResource.class);
  private static final Gson gson = new Gson();

  /**
   * @param start Start date and time inclusive e.g. "2017-12-19|20:05:22"
   * @param end End date and time exclusive e.g. "2017-12-19|20:05:22"
   */
  @GET
  @Produces("application/json; charset=UTF-8")
  public Response list(
      @QueryParam("auth") String auth,
      @QueryParam("start") String start,
      @QueryParam("end") String end
  ) throws UnsupportedEncodingException {
    if (!authValid(auth)) {
      return Response.status(Response.Status.FORBIDDEN).build();
    }
    Date startDate = null, endDate = null;
    if (start != null) {
      startDate = parseDateTime(start);
      if (startDate == null) {
        return badRequest();
      }
    }
    if (end != null) {
      endDate = parseDateTime(end);
      if (endDate == null) {
        return badRequest();
      }
    }
    Stopwatch stopwatch = Stopwatch.createStarted();

    byte[] json = new UsersJson(getUsers(startDate, endDate)).toBytes();
    logger.info("Encoded users in {}.", stopwatch);
    return Response.ok(json).build();
  }

  // TODO: Pass in target state instead of verb. The verb should just be used for UI.
  @POST
  @Produces("application/json; charset=UTF-8")
  @Path("/update")
  public Response update(
      @FormParam("uuid") String uuid,
      @FormParam("action") String action,
      @QueryParam("auth") String auth
  ) throws UnsupportedEncodingException {
    if (!authValid(auth)) {
      return Response.status(Response.Status.FORBIDDEN).build();
    }
    Preconditions.checkNotNull(action);
    logger.info("Update User: uuid: {}, action: {}", uuid, action);
    Map<String, UserState> userStatesByAdminVerb = Maps.uniqueIndex(
        Arrays.asList(UserState.values()), UserState::adminVerb);
    UserState actionState = userStatesByAdminVerb.get(action);
    if (actionState == null) {
      logger.error("Console sent unrecognized state for action: "+action);
      return Response.status(Response.Status.FORBIDDEN).build();
    }

    User user = User.get(uuid);
    if (user != null) {
      if (user.state != actionState) {
        if (!user.state.validAdminTransitions().contains(actionState)) {
          logger.error("Console sent invalid state transition: " + actionState);
          return Response.status(Response.Status.FORBIDDEN).build();
        }
        logger.info("console: transitioning user to state: " + actionState);
        if (user.transitionTo(actionState)) {
          if (user.state == UserState.MEMBER) {
            try {
              Users.runAsGenericAdminUserRequest(() -> {
                AppEngineUserService.welcomeNewUser(user);
              });
            } catch (Exception e) {
              logger.error("Error running new user welcome.", e);
            }
          }
        }
      }
      return Response.ok(new UsersJson(
          new UserJson(Clients.getClientsForUser(user), user, GroupReferrals.countReferrals(user))).toBytes()).build();
    }

    return Response.status(Response.Status.NOT_FOUND).build();
  }

  @GET
  @Produces("application/json; charset=UTF-8")
  @Path("/visdata")
  public Response visdata(
      @QueryParam("auth") String auth
  ) throws UnsupportedEncodingException {
    if (!authValid(auth)) {
      return Response.status(Response.Status.FORBIDDEN).build();
    }
    List<NodeJson> nodes = new ArrayList<>();
    List<EdgeJson> edges = new ArrayList<>();
    for (User user : Users.all()) {
      nodes.add(new NodeJson(user.uuid, user.publicName(), user.profilePhotoUrl()));
      for (User friend: user.friends()) {
        if (user.uuid.compareTo(friend.uuid)>0) {
          edges.add(new EdgeJson(user.uuid, friend.uuid));
        }
      }
    }
    return Response.ok(new VisData(nodes, edges).toBytes()).build();
  }

  private Response badRequest() {
    return Response.status(Response.Status.BAD_REQUEST).build();
  }

  private List<UserJson> getUsers(@Nullable Date startDate, @Nullable Date endDate) {
    List<UserJson> users = new ArrayList<>();

    // Start batch load of group referrals
    GroupReferrals.ReportQuery reportQuery = new GroupReferrals.ReportQuery();

    Query<User> usersQuery = Users.query();

    // Start date inclusive
    if (startDate != null) {
      usersQuery = usersQuery.filter(User.Fields.signupTime.name() + " >= ", startDate.getTime());
    }
    // End date exclusive
    if (endDate != null) {
      usersQuery = usersQuery.filter(User.Fields.signupTime.name() + " < ", endDate.getTime());
    }
    // Order ascending (oldest first)
    if (startDate != null || endDate != null) {
      usersQuery = usersQuery.order(User.Fields.signupTime.name());
    }

    List<User> allUsers = usersQuery.list();
    List<Client> clients = Clients.query().filter("user !=", null).list();
    Multimap<String, Client> clientsByUser = Multimaps.index(clients, c -> c.user.getKey().getName());
    Map<User, Long> referralsByUser = reportQuery.report();
    for (User user: allUsers) {
      users.add(new UserJson(clientsByUser.get(user.uuid), user, referralsByUser.get(user)));
    }
    users.sort(Comparator.comparing((UserJson u) -> u.lastActivityTime).reversed());

    logger.info("console user count = {}", users.size());
    return users;
  }

  private static class UsersJson {
    public List<UserJson> users;

    public UsersJson(List<UserJson> users) {
      this.users = users;
    }

    public UsersJson(UserJson user) {
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

  static class VisData {
    public List<NodeJson> nodes;
    public List<EdgeJson> edges;

    public VisData(
        List<NodeJson> nodes, List<EdgeJson> edges) {
      this.nodes = nodes;
      this.edges = edges;
    }

    public byte[] toBytes() {
      //logger.info("visdata json = {}", gson.toJson(this));
      try {
        return gson.toJson(this).getBytes("UTF-8");
      } catch (UnsupportedEncodingException e) {
        throw new RuntimeException(e);
      }
    }
  }

  static class NodeJson {
    public String id;
    public String label;
    public String shape = "circularImage";
    public String image;

    public NodeJson(String id, String label, String image) {
      this.id = id;
      this.label = label;
      this.image = image;
    }
  }
  static class EdgeJson {
    public String from, to;

    public EdgeJson(String from, String to) {
      this.from = from;
      this.to = to;
    }
  }

  private boolean authValid(String authIn) {
    // Currently same auth code as for cron and notification tester.
    final String auth = "xxx";
    return auth.equalsIgnoreCase(authIn);
  }

  // e.g. "2017-12-19|20:05:22"
  private @Nullable Date parseDateTime(String dateTime) {
    SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd|HH:mm:ss");
    df.setTimeZone(TimeZone.getTimeZone("PST"));
    try {
      return df.parse(dateTime);
    } catch (ParseException e) {
      return null;
    }
  }
}
