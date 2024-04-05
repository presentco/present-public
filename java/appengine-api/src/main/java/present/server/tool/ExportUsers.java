package present.server.tool;

import com.google.common.base.Predicates;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimaps;
import com.googlecode.objectify.Ref;
import com.opencsv.CSVWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import present.server.Cities;
import present.server.NearbyCity;
import present.server.model.comment.Comment;
import present.server.model.comment.Comments;
import present.server.model.console.whitelist.geofence.WhitelistGeofences;
import present.server.model.group.JoinedGroups;
import present.server.model.user.Client;
import present.server.model.user.Clients;
import present.server.model.user.User;
import present.server.model.user.Users;
import present.server.model.util.Coordinates;

import static present.server.tool.RemoteTool.PRODUCTION_SERVER;
import static present.server.tool.RemoteTool.against;

public class ExportUsers {
  public static void main(String[] args) {
    Logger.getLogger("").setLevel(Level.SEVERE);
    against(PRODUCTION_SERVER, () -> {
      exportTo(new FileWriter("out"
          + "/users.csv"));
    });
  }

  public static void exportTo(Writer writer) throws IOException {
    CSVWriter out = new CSVWriter(writer);
    out.writeNext(new String[] {
        "ID",
        "First Name",
        "Last Name",
        "Full Name",
        "Gender",
        "State",
        "Review",
        "Email",
        "Signup Date",
        "Signup Location",
        "Nearest City",
        "Distance (km)",
        "In Geofence",
        "Last Activity",
        "Join Count",
        "Joined Circles",
        "Comments (This Week)",
        "Interests",
        "Platforms",
        "Notifications",
        "Facebook",
        "Profile"
    });
    List<JoinedGroups> allJoinedGroups = JoinedGroups.query().list();
    List<Comment> commentsThisWeek = Comments.thisWeek();
    List<Client> allClients = Clients.query().filter("user !=", null).list();
    List<User> allUsers = Users.all();
    ImmutableMap<User, JoinedGroups> savedGroupsByUser =
        Maps.uniqueIndex(Iterables.filter(allJoinedGroups,
            sg -> sg.user.get() != null), sg -> sg.user.get());
    ImmutableListMultimap<User, Comment> commentsByUser
        = Multimaps.index(commentsThisWeek, comment -> comment.author.get());
    ImmutableListMultimap<User, Client> clientsByUser =
        Multimaps.index(Iterables.filter(allClients, client -> client.user != null
            && client.user.get() != null), client -> client.user.get());
    SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
    WhitelistGeofences geofences = WhitelistGeofences.load();
    for (User user : allUsers) {
      JoinedGroups joinedGroups = savedGroupsByUser.get(user);
      ImmutableList<Comment> comments = commentsByUser.get(user);
      ImmutableList<Client> clients = clientsByUser.get(user);
      NearbyCity nearestCity = nearestCity(user, clients);
      Boolean notificationsEnabled = clients.stream()
          .map(client -> client.notificationsEnabled)
          .filter(Predicates.notNull())
          .reduce(Boolean::logicalOr)
          .orElse(null);
      String savedGroupsString = joinedGroups == null || joinedGroups.groups == null ? ""
          : joinedGroups.groups.stream()
              .map(Ref::get)
              .filter(Objects::nonNull)
              .filter(g -> !g.deleted)
              .map(g -> g.title)
              .collect(Collectors.joining("\n"));
      String commentsString = comments == null ? "" : String.valueOf(comments.size());
      String facebook = "https://www.facebook.com/app_scoped_user_id/" + user.facebookId + "/";
      String inGeofence = "";
      if (user.signupLocation != null) {
        inGeofence = String.valueOf(geofences.contains(user.signupLocation));
      }
      out.writeNext(new String[] {
          user.uuid,
          user.firstName,
          user.lastName,
          user.fullName(),
          user.gender() == null ? "" : user.gender().name(),
          user.state == null ? "" : user.state.name(),
          user.review == null ? "" : user.review.description,
          user.email,
          dateFormat.format(new Date(user.signupTime)),
          user.signupAddress == null ? "" : user.signupAddress.niceString(),
          nearestCity == null ? "" : nearestCity.city.name,
          nearestCity == null ? "" : String.valueOf((int) nearestCity.distance / 1000),
          inGeofence,
          dateFormat.format(new Date(lastActivityTime(user, clients))),
          joinedGroups == null ? "0" : joinedGroups.count() + "",
          savedGroupsString,
          commentsString,
          user.interests.stream().collect(Collectors.joining("\n")),
          clients.stream().map(Client::platform)
              .filter(Objects::nonNull)
              .map(Enum::name)
              .distinct().sorted().collect(Collectors.joining("\n")),
          String.valueOf(notificationsEnabled),
          facebook,
          user.shortLink()
      });
      System.out.print(".");
    }
    out.close();
  }

  public static NearbyCity nearestCity(User user, List<Client> clients) {
    Coordinates best = null;
    long bestTimestamp = 0;
    for (Client client : clients) {
      if (client.locationTimestamp > bestTimestamp) {
        best = Coordinates.fromS2Cell(client.location);
        bestTimestamp = client.locationTimestamp;
      }
    }
    return best != null ? Cities.nearestTo(best) : user.nearestCity();
  }

  private static long lastActivityTime(User user, List<Client> clients) {
    long lastActivityTime = Long.MIN_VALUE;
    for (Client client : clients) {
      if (client.deviceTokenUpdateTime > lastActivityTime) {
        lastActivityTime = client.deviceTokenUpdateTime;
      }
    }
    if (user.updatedTime > lastActivityTime) {
      lastActivityTime = user.updatedTime;
    }
    return lastActivityTime;
  }
}
